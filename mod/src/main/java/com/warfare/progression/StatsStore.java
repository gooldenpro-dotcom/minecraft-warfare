package com.warfare.progression;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.warfare.Warfare;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Persistance des performances : un fichier JSON par joueur dans
 * world/warfare/stats/&lt;uuid&gt;.json, charge a la connexion et ecrit a chaque partie.
 *
 * Le classement est reconstruit depuis le dossier, donc il inclut les joueurs hors ligne.
 */
public final class StatsStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<UUID, PlayerStats> CACHE = new HashMap<>();

    /** Une ligne de classement. */
    public record Row(UUID id, String name, int level, int prestige, int kills, int deaths,
                      int wins, long score) {
        public double kd() {
            return deaths == 0 ? kills : (double) kills / deaths;
        }
    }

    public static PlayerStats get(MinecraftServer server, UUID id) {
        return CACHE.computeIfAbsent(id, uuid -> load(server, uuid));
    }

    public static PlayerStats of(ServerPlayer player) {
        PlayerStats stats = get(player.server, player.getUUID());
        stats.name = player.getGameProfile().getName();
        return stats;
    }

    public static void unload(MinecraftServer server, UUID id) {
        save(server, id);
        CACHE.remove(id);
    }

    public static void saveAll(MinecraftServer server) {
        for (UUID id : List.copyOf(CACHE.keySet())) save(server, id);
    }

    public static void clearCache() {
        CACHE.clear();
    }

    private static Path directory(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("warfare").resolve("stats");
    }

    private static PlayerStats load(MinecraftServer server, UUID id) {
        Path file = directory(server).resolve(id + ".json");
        if (!Files.isRegularFile(file)) return new PlayerStats();
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            return PlayerStats.fromJson(root);
        } catch (Exception e) {
            Warfare.LOGGER.warn("Statistiques illisibles pour {} ({}), remise a zero", id, e.toString());
            return new PlayerStats();
        }
    }

    public static void save(MinecraftServer server, UUID id) {
        PlayerStats stats = CACHE.get(id);
        if (stats == null) return;
        Path file = directory(server).resolve(id + ".json");
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(stats.toJson()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            Warfare.LOGGER.error("Ecriture des statistiques impossible pour {}", id, e);
        }
    }

    /** Classement complet, trie par score decroissant. Les joueurs en ligne passent par le cache. */
    public static List<Row> leaderboard(MinecraftServer server, int limit) {
        Map<UUID, PlayerStats> all = new HashMap<>();
        Path directory = directory(server);
        if (Files.isDirectory(directory)) {
            try (Stream<Path> files = Files.list(directory)) {
                files.filter(path -> path.getFileName().toString().endsWith(".json")).forEach(path -> {
                    String fileName = path.getFileName().toString();
                    try {
                        UUID id = UUID.fromString(fileName.substring(0, fileName.length() - 5));
                        if (CACHE.containsKey(id)) return;
                        JsonObject root = JsonParser.parseString(
                                Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
                        all.put(id, PlayerStats.fromJson(root));
                    } catch (Exception ignored) {
                        // fichier etranger ou illisible : on l'ignore
                    }
                });
            } catch (IOException e) {
                Warfare.LOGGER.warn("Lecture du classement impossible : {}", e.toString());
            }
        }
        all.putAll(CACHE);

        List<Row> rows = new ArrayList<>();
        for (Map.Entry<UUID, PlayerStats> entry : all.entrySet()) {
            PlayerStats stats = entry.getValue();
            if (stats.matches == 0 && stats.kills == 0) continue;
            String name = stats.name.isEmpty() ? entry.getKey().toString().substring(0, 8) : stats.name;
            rows.add(new Row(entry.getKey(), name, stats.level, stats.prestige,
                    stats.kills, stats.deaths, stats.wins, stats.score));
        }
        rows.sort(Comparator.comparingLong(Row::score).reversed());
        return rows.size() > limit ? List.copyOf(rows.subList(0, limit)) : List.copyOf(rows);
    }

    private StatsStore() {}
}
