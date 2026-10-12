package com.warfare.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundClearTitlesPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;

/** Helpers de texte et de titres. */
public final class Txt {

    public static MutableComponent c(String text, ChatFormatting... formats) {
        return Component.literal(text).withStyle(formats);
    }

    /** Prefixe commun des messages du mod. */
    public static MutableComponent prefix() {
        return Component.empty()
                .append(c("[", ChatFormatting.DARK_GRAY))
                .append(c("WARFARE", ChatFormatting.GOLD, ChatFormatting.BOLD))
                .append(c("] ", ChatFormatting.DARK_GRAY));
    }

    public static MutableComponent info(Component body) {
        return prefix().append(body);
    }

    public static String clock(int seconds) {
        if (seconds < 0) seconds = 0;
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    public static void title(ServerPlayer player, @Nullable Component title, @Nullable Component subtitle,
                             int fadeIn, int stay, int fadeOut) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut));
        player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle == null ? Component.empty() : subtitle));
        player.connection.send(new ClientboundSetTitleTextPacket(title == null ? Component.empty() : title));
    }

    public static void clearTitle(ServerPlayer player) {
        player.connection.send(new ClientboundClearTitlesPacket(true));
    }

    public static void actionBar(ServerPlayer player, Component text) {
        player.displayClientMessage(text, true);
    }

    private Txt() {}
}
