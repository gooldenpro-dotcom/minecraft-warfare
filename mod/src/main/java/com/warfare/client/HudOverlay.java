package com.warfare.client;

import com.warfare.Warfare;
import com.warfare.WarfareConfig;
import com.warfare.util.Txt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * ATH du mod : chrono et score en haut, serie et vies en bas a gauche,
 * progression de grade en bas a droite, points qui montent au centre.
 *
 * Tout est dessine a plat avec la police du jeu. Aucun element graphique,
 * sonore ou typographique repris d'un jeu existant.
 */
@Mod.EventBusSubscriber(modid = Warfare.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class HudOverlay {
    private static final int TEXT = 0xFFD6DACF;
    private static final int MUTED = 0xFF8A937F;
    private static final int ACCENT = 0xFFB9922F;
    private static final int PANEL = 0xA00A0B08;

    @SubscribeEvent
    public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("warfare_hud", HudOverlay::render);
    }

    private static void render(ForgeGui gui, GuiGraphics graphics, float partialTick,
                               int width, int height) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) return;
        if (!ClientMatchState.synced || !WarfareConfig.hudEnabled) return;
        if (minecraft.screen instanceof LobbyScreen || minecraft.screen instanceof MatchIntroScreen) return;

        if (WarfareConfig.scorePopups) ScorePopups.render(graphics, width, height);
        if (!ClientMatchState.running()) return;

        var font = minecraft.font;

        // --- Bandeau haut : chrono et scores ---
        String clock = Txt.clock(ClientMatchState.secondsLeft);
        int bandWidth = 150;
        int bandX = (width - bandWidth) / 2;
        graphics.fill(bandX, 2, bandX + bandWidth, 16, PANEL);
        graphics.fill(bandX, 16, bandX + bandWidth, 17, 0x60B9922F);

        if (ClientMatchState.teamMode()) {
            String alpha = String.valueOf(ClientMatchState.alphaScore);
            String bravo = String.valueOf(ClientMatchState.bravoScore);
            graphics.drawString(font, alpha, bandX + 10, 5, 0xFFD24A3A, false);
            graphics.drawString(font, clock, (width - font.width(clock)) / 2, 5, TEXT, false);
            graphics.drawString(font, bravo, bandX + bandWidth - 10 - font.width(bravo), 5,
                    0xFF3FA9C9, false);
        } else {
            String score = ClientMatchState.myScore + " / " + ClientMatchState.scoreLimit;
            graphics.drawString(font, clock, (width - font.width(clock)) / 2, 5, TEXT, false);
            graphics.drawString(font, score, bandX + bandWidth - 10 - font.width(score), 5, MUTED, false);
        }

        // --- Bas gauche : mode, serie, vies, echelle d'armes ---
        int y = height - 60;
        graphics.drawString(font, ClientMatchState.modeTitle().toUpperCase(java.util.Locale.ROOT),
                6, y, ACCENT, false);
        y += 11;

        if (ClientMatchState.nextStreakAt > 0) {
            String streak = "Serie " + ClientMatchState.myStreak + " / " + ClientMatchState.nextStreakAt;
            graphics.drawString(font, streak, 6, y, TEXT, false);
            y += 11;
        }
        if (ClientMatchState.myLives > 0) {
            graphics.drawString(font, "Vies " + ClientMatchState.myLives, 6, y, TEXT, false);
            y += 11;
        }
        if (ClientMatchState.ladderSize > 0) {
            graphics.drawString(font, "Arme " + (ClientMatchState.ladderStep + 1) + " / "
                    + ClientMatchState.ladderSize, 6, y, TEXT, false);
        }

        // --- Bas droite : grade et progression ---
        String rank = "NIV. " + ClientMatchState.level
                + (ClientMatchState.prestige > 0 ? "  P" + ClientMatchState.prestige : "");
        int barWidth = 90;
        int barX = width - barWidth - 6;
        int barY = height - 18;
        graphics.drawString(font, rank, barX, barY - 11, ACCENT, false);
        graphics.fill(barX, barY, barX + barWidth, barY + 4, 0xA0101309);
        long need = Math.max(1, ClientMatchState.xpToNext);
        int filled = (int) Math.min(barWidth, ClientMatchState.xpIntoLevel * barWidth / need);
        graphics.fill(barX, barY, barX + filled, barY + 4, ACCENT);
    }

    private HudOverlay() {}
}
