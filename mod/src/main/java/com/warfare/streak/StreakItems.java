package com.warfare.streak;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/** L'objet-recompense pose dans l'emplacement dedie de la barre d'action. */
public final class StreakItems {
    public static final String TAG = "WarfareStreak";
    /** Emplacement reserve de la barre d'action (le dernier). */
    public static final int SLOT = 8;

    public static ItemStack create(Killstreak streak) {
        ItemStack stack = new ItemStack(streak.item());
        stack.getOrCreateTag().putString(TAG, streak.id());
        stack.setHoverName(Component.literal(streak.title())
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        return stack;
    }

    @Nullable
    public static Killstreak of(ItemStack stack) {
        if (stack.isEmpty()) return null;
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG)) return null;
        return Killstreak.byId(tag.getString(TAG));
    }

    public static boolean isStreakItem(ItemStack stack) {
        return of(stack) != null;
    }

    private StreakItems() {}
}
