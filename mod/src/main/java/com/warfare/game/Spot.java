package com.warfare.game;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/** Une position orientee dans une dimension. */
public record Spot(ResourceKey<Level> dimension, double x, double y, double z, float yaw, float pitch) {

    public static Spot of(ServerPlayer player) {
        return new Spot(player.level().dimension(), player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot());
    }

    public CompoundTag write() {
        CompoundTag tag = new CompoundTag();
        tag.putString("dim", dimension.location().toString());
        tag.putDouble("x", x);
        tag.putDouble("y", y);
        tag.putDouble("z", z);
        tag.putFloat("yaw", yaw);
        tag.putFloat("pitch", pitch);
        return tag;
    }

    public static Spot read(CompoundTag tag) {
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION,
                new ResourceLocation(tag.getString("dim")));
        return new Spot(dimension, tag.getDouble("x"), tag.getDouble("y"), tag.getDouble("z"),
                tag.getFloat("yaw"), tag.getFloat("pitch"));
    }

    @Nullable
    public ServerLevel level(MinecraftServer server) {
        return server.getLevel(dimension);
    }

    public String describe() {
        return String.format("%s %.1f %.1f %.1f", dimension.location(), x, y, z);
    }
}
