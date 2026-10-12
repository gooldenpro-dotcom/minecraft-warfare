package com.warfare.loadout;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.warfare.Warfare;
import com.warfare.WarfareConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Persistance des classes : un fichier JSON par joueur dans
 * world/warfare/classes/&lt;uuid&gt;.json. Charge a la connexion, ecrit a chaque modification.
 */
public final class ClassStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<UUID, PlayerClasses> CACHE = new HashMap<>();

    public static PlayerClasses get(MinecraftServer server, UUID id) {
        return CACHE.computeIfAbsent(id, uuid -> load(server, uuid));
    }

    public static void unload(UUID id) {
        CACHE.remove(id);
    }

    public static void clearCache() {
        CACHE.clear();
    }

    private static Path directory(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("warfare").resolve("classes");
    }

    private static PlayerClasses load(MinecraftServer server, UUID id) {
        Path file = directory(server).resolve(id + ".json");
        PlayerClasses classes = new PlayerClasses();
        if (!Files.isRegularFile(file)) return classes;
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            JsonArray array = root.getAsJsonArray("classes");
            if (array != null) {
                for (int i = 0; i < Math.min(array.size(), PlayerClasses.SLOTS); i++) {
                    classes.set(i, sanitize(fromJson(array.get(i).getAsJsonObject(), i), i));
                }
            }
            if (root.has("selected")) classes.select(root.get("selected").getAsInt());
        } catch (Exception e) {
            Warfare.LOGGER.warn("Classes illisibles pour {} ({}), remise a zero", id, e.toString());
        }
        return classes;
    }

    public static void save(MinecraftServer server, UUID id) {
        PlayerClasses classes = CACHE.get(id);
        if (classes == null) return;
        Path file = directory(server).resolve(id + ".json");
        try {
            Files.createDirectories(file.getParent());
            JsonObject root = new JsonObject();
            root.addProperty("selected", classes.selected());
            JsonArray array = new JsonArray();
            for (ClassLoadout loadout : classes.all()) array.add(toJson(loadout));
            root.add("classes", array);
            Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException e) {
            Warfare.LOGGER.error("Ecriture des classes impossible pour {}", id, e);
        }
    }

    private static JsonObject toJson(ClassLoadout loadout) {
        JsonObject object = new JsonObject();
        object.addProperty("name", loadout.name());
        object.addProperty("primary", loadout.primaryGun());
        object.addProperty("secondary", loadout.secondaryGun());
        JsonArray perks = new JsonArray();
        for (int category = 1; category <= Perk.CATEGORIES; category++) perks.add(loadout.perk(category).id());
        object.add("perks", perks);
        return object;
    }

    private static ClassLoadout fromJson(JsonObject object, int slot) {
        ClassLoadout loadout = ClassLoadout.defaultFor(slot);
        if (object.has("name")) loadout = loadout.withName(object.get("name").getAsString());
        if (object.has("primary")) loadout = loadout.withPrimary(object.get("primary").getAsString());
        if (object.has("secondary")) loadout = loadout.withSecondary(object.get("secondary").getAsString());
        if (object.has("perks")) {
            for (var element : object.getAsJsonArray("perks")) {
                Perk perk = Perk.byId(element.getAsString());
                if (perk != null) loadout = loadout.withPerk(perk);
            }
        }
        return loadout;
    }

    /**
     * Validation serveur : rien de ce qui vient du client n'est repris tel quel.
     * Nom borne et nettoye, armes verifiees contre le catalogue, atouts forcement valides.
     */
    public static ClassLoadout sanitize(ClassLoadout incoming, int slot) {
        String name = incoming.name() == null ? "" : incoming.name().replaceAll("[\\p{Cntrl}§]", "").trim();
        if (name.length() > ClassLoadout.MAX_NAME_LENGTH) name = name.substring(0, ClassLoadout.MAX_NAME_LENGTH);
        if (name.isBlank()) name = "Classe " + (slot + 1);

        String primary = validGun(incoming.primaryGun(), WarfareConfig.primaryGun);
        String secondary = validGun(incoming.secondaryGun(), WarfareConfig.secondaryGun);

        List<Perk> perks = List.of(
                valid(incoming.perk(1), 1),
                valid(incoming.perk(2), 2),
                valid(incoming.perk(3), 3));
        return new ClassLoadout(name, primary, secondary, perks);
    }

    private static Perk valid(Perk perk, int category) {
        return perk != null && perk.category() == category ? perk : Perk.defaultOf(category);
    }

    private static String validGun(String gunId, String fallback) {
        if (gunId != null && GunCatalog.isAllowed(gunId)) return gunId;
        if (GunCatalog.isAllowed(fallback)) return fallback;
        List<GunCatalog.Entry> all = GunCatalog.all();
        return all.isEmpty() ? fallback : all.get(0).gunId();
    }

    private ClassStore() {}
}
