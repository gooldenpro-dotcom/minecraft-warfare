package com.warfare.game;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Les arenes du serveur et celle qui est selectionnee, sauvegardees avec le monde
 * (world/data/warfare.dat).
 *
 * L'ancien format (un lobby + une liste de points, sans arene) est relu et converti
 * en une arene nommee "defaut" au premier chargement.
 */
public class WarfareData extends SavedData {
    private static final String NAME = "warfare";

    private final Map<String, Arena> arenas = new LinkedHashMap<>();
    private String selected = "";

    /** Le mode de jeu choisi pour la prochaine partie, conserve entre deux redemarrages. */
    private String mode = "";

    public static WarfareData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(WarfareData::load, WarfareData::new, NAME);
    }

    // --------------------------------------------------------------- arenes

    public List<Arena> arenas() {
        return List.copyOf(arenas.values());
    }

    public List<String> arenaNames() {
        return List.copyOf(arenas.keySet());
    }

    @Nullable
    public Arena arena(String name) {
        return arenas.get(key(name));
    }

    /** L'arene selectionnee, ou la premiere disponible, ou null si aucune n'existe. */
    @Nullable
    public Arena current() {
        Arena arena = arenas.get(selected);
        if (arena != null) return arena;
        return arenas.isEmpty() ? null : arenas.values().iterator().next();
    }

    public String currentName() {
        Arena arena = current();
        return arena == null ? "" : arena.name();
    }

    /** Cree l'arene si elle n'existe pas et la renvoie. */
    public Arena getOrCreate(String name) {
        String key = key(name);
        Arena arena = arenas.get(key);
        if (arena == null) {
            arena = new Arena(key);
            arenas.put(key, arena);
            if (selected.isEmpty()) selected = key;
            setDirty();
        }
        return arena;
    }

    public boolean select(String name) {
        String key = key(name);
        if (!arenas.containsKey(key)) return false;
        selected = key;
        setDirty();
        return true;
    }

    public boolean delete(String name) {
        String key = key(name);
        if (arenas.remove(key) == null) return false;
        if (selected.equals(key)) selected = arenas.isEmpty() ? "" : arenas.keySet().iterator().next();
        setDirty();
        return true;
    }

    /** A appeler apres toute modification d'une arene recuperee par reference. */
    public void touch() {
        setDirty();
    }

    // ----------------------------------------------------------------- mode

    public GameMode mode() {
        return mode.isEmpty() ? GameMode.defaultMode() : GameMode.byId(mode);
    }

    public void setMode(GameMode value) {
        this.mode = value.id();
        setDirty();
    }

    // ------------------------------------------------------- raccourcis jeu

    @Nullable
    public Spot getLobby() {
        Arena arena = current();
        return arena == null ? null : arena.lobby();
    }

    public List<Spot> spawnsFor(Team team) {
        Arena arena = current();
        return arena == null ? List.of() : arena.spawnsFor(team);
    }

    public boolean isReady() {
        Arena arena = current();
        return arena != null && arena.isReady();
    }

    public boolean isReadyFor(GameMode mode) {
        Arena arena = current();
        return arena != null && arena.isReadyFor(mode);
    }

    // -------------------------------------------------------- serialisation

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Arena arena : arenas.values()) list.add(arena.write());
        tag.put("arenas", list);
        tag.putString("selected", selected);
        tag.putString("mode", mode);
        return tag;
    }

    public static WarfareData load(CompoundTag tag) {
        WarfareData data = new WarfareData();
        if (tag.contains("arenas")) {
            ListTag list = tag.getList("arenas", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                Arena arena = Arena.read(list.getCompound(i));
                if (!arena.name().isEmpty()) data.arenas.put(arena.name(), arena);
            }
            data.selected = tag.getString("selected");
        } else {
            // Ancien format : un seul lobby et une liste de points sans equipe.
            Arena arena = new Arena("defaut");
            if (tag.contains("lobby")) arena.setLobby(Spot.read(tag.getCompound("lobby")));
            ListTag list = tag.getList("spawns", Tag.TAG_COMPOUND);
            List<Spot> spots = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) spots.add(Spot.read(list.getCompound(i)));
            for (Spot spot : spots) arena.add(Team.NONE, spot);
            if (arena.lobby() != null || !spots.isEmpty()) {
                data.arenas.put(arena.name(), arena);
                data.selected = arena.name();
            }
        }
        data.mode = tag.getString("mode");
        if (!data.arenas.containsKey(data.selected)) {
            data.selected = data.arenas.isEmpty() ? "" : data.arenas.keySet().iterator().next();
        }
        return data;
    }

    /** Nom d'arene normalise : minuscules, sans espace ni ponctuation. */
    public static String key(String name) {
        String cleaned = name.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_-]+", "_")
                .replaceAll("_{2,}", "_");
        return cleaned.replaceAll("^_+", "").replaceAll("_+$", "");
    }
}
