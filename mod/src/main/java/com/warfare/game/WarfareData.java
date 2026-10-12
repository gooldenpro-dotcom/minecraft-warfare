package com.warfare.game;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Lobby et points d'apparition, sauvegardes avec le monde (world/data/warfare.dat).
 */
public class WarfareData extends SavedData {
    private static final String NAME = "warfare";

    /** Une position orientee dans une dimension. */
    public record Spot(ResourceKey<Level> dimension, double x, double y, double z, float yaw, float pitch) {
        public static Spot of(ServerPlayer player) {
            return new Spot(player.level().dimension(), player.getX(), player.getY(), player.getZ(),
                    player.getYRot(), player.getXRot());
        }

        public CompoundTag write() {
            CompoundTag t = new CompoundTag();
            t.putString("dim", dimension.location().toString());
            t.putDouble("x", x);
            t.putDouble("y", y);
            t.putDouble("z", z);
            t.putFloat("yaw", yaw);
            t.putFloat("pitch", pitch);
            return t;
        }

        public static Spot read(CompoundTag t) {
            ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(t.getString("dim")));
            return new Spot(dim, t.getDouble("x"), t.getDouble("y"), t.getDouble("z"),
                    t.getFloat("yaw"), t.getFloat("pitch"));
        }

        @Nullable
        public ServerLevel level(MinecraftServer server) {
            return server.getLevel(dimension);
        }

        public String describe() {
            return String.format("%s %.1f %.1f %.1f", dimension.location(), x, y, z);
        }
    }

    @Nullable
    private Spot lobby;
    private final List<Spot> spawns = new ArrayList<>();

    public static WarfareData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(WarfareData::load, WarfareData::new, NAME);
    }

    @Nullable
    public Spot getLobby() {
        return lobby;
    }

    public void setLobby(Spot spot) {
        this.lobby = spot;
        setDirty();
    }

    public List<Spot> getSpawns() {
        return List.copyOf(spawns);
    }

    public int addSpawn(Spot spot) {
        spawns.add(spot);
        setDirty();
        return spawns.size();
    }

    /** @return true si l'index existait. */
    public boolean removeSpawn(int index) {
        if (index < 0 || index >= spawns.size()) return false;
        spawns.remove(index);
        setDirty();
        return true;
    }

    public boolean isReady() {
        return lobby != null && !spawns.isEmpty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        if (lobby != null) tag.put("lobby", lobby.write());
        ListTag list = new ListTag();
        for (Spot s : spawns) list.add(s.write());
        tag.put("spawns", list);
        return tag;
    }

    public static WarfareData load(CompoundTag tag) {
        WarfareData data = new WarfareData();
        if (tag.contains("lobby")) data.lobby = Spot.read(tag.getCompound("lobby"));
        ListTag list = tag.getList("spawns", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) data.spawns.add(Spot.read(list.getCompound(i)));
        return data;
    }
}
