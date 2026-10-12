package com.warfare.perk;

import com.warfare.loadout.ClassLoadout;
import com.warfare.loadout.Perk;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.*;

/**
 * Atouts reellement actifs sur chaque joueur : ceux de la classe avec laquelle il a
 * reapparu, jamais ceux que le client pretend avoir.
 */
public final class ActivePerks {
    private static final UUID SPEED_MODIFIER = UUID.fromString("5f3b9c21-4d6a-4a1e-9b77-1c0d2e8a6f41");
    private static final Map<UUID, Set<Perk>> ACTIVE = new HashMap<>();

    public static void apply(ServerPlayer player, ClassLoadout loadout) {
        Set<Perk> perks = EnumSet.noneOf(Perk.class);
        for (int category = 1; category <= Perk.CATEGORIES; category++) perks.add(loadout.perk(category));
        ACTIVE.put(player.getUUID(), perks);
        refreshAttributes(player, perks);
    }

    public static void clear(ServerPlayer player) {
        ACTIVE.remove(player.getUUID());
        refreshAttributes(player, Set.of());
    }

    public static void forget(UUID id) {
        ACTIVE.remove(id);
    }

    public static boolean has(UUID player, Perk perk) {
        Set<Perk> perks = ACTIVE.get(player);
        return perks != null && perks.contains(perk);
    }

    private static void refreshAttributes(ServerPlayer player, Set<Perk> perks) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        speed.removeModifier(SPEED_MODIFIER);
        if (perks.contains(Perk.SPRINTER)) {
            speed.addTransientModifier(new AttributeModifier(SPEED_MODIFIER, "warfare_sprinteur",
                    0.10, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    private ActivePerks() {}
}
