package com.warfare.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Ecran de preparation, affiche pendant le compte a rebours et referme au debut de la partie.
 * Entierement dessine : aucun logo, aucune texture et aucun texte repris d'un jeu existant.
 */
public class MatchIntroScreen extends Screen {

    /** Conseils maison, affiches a tour de role en bas de l'ecran. */
    private static final String[] TIPS = {
            "Les series d'eliminations se construisent sans mourir entre deux eliminations.",
            "Touche C dans le lobby pour modifier tes 5 classes.",
            "La recompense de killstreak arrive dans le dernier emplacement de ta barre.",
            "Le drone de reperage ne revele les ennemis que pour toi, personne d'autre.",
            "Aucune explosion n'abime le terrain : le relief ne changera pas de la partie.",
            "Tu reapparais toujours au point d'apparition le plus eloigne des ennemis vivants.",
            "Puissance d'arret ajoute 20 % de degats a chaque balle.",
            "Pas de loup rend tes deplacements totalement silencieux pour les autres.",
            "Vise au clic droit : la dispersion diminue nettement.",
            "Une elimination obtenue avec une killstreak ne fait pas monter ta serie.",
    };

    private int ticks;

    public MatchIntroScreen() {
        super(Component.literal("Preparation de la partie"));
    }

    @Override
    public void tick() {
        ticks++;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xF00A0B08);

        int right = width - 40;
        drawScaled(graphics, "CHACUN POUR SOI", right, 46, 2.0f, Palette.ACCENT, true);
        drawScaled(graphics, ClientMatchState.mapName, right, 70, 1.6f, Palette.TEXT, true);
        String waiting = ClientMatchState.state.equals("COUNTDOWN")
                ? "Preparation de la partie..." : "Partie en cours";
        graphics.drawString(font, waiting, right - font.width(waiting), 94, Palette.MUTED, false);

        // Compte a rebours
        if (ClientMatchState.state.equals("COUNTDOWN")) {
            String seconds = String.valueOf(Math.max(0, ClientMatchState.secondsLeft));
            drawScaled(graphics, seconds, width / 2, height / 2 - 20, 5.0f, Palette.ACCENT, false);
            String label = "avant le debut";
            graphics.drawString(font, label, (width - font.width(label)) / 2, height / 2 + 26,
                    Palette.MUTED, false);
        }

        // Liseres
        graphics.fill(40, height - 86, width - 40, height - 85, Palette.BORDER);
        graphics.fill(40, height - 86, 140, height - 85, Palette.ACCENT);

        graphics.drawString(font, "INFO", 40, height - 78, Palette.ACCENT, false);
        String tip = TIPS[(ticks / 80) % TIPS.length];
        graphics.drawString(font, tip, 40, height - 64, Palette.TEXT, false);

        String hint = "Echap pour masquer cet ecran";
        graphics.drawString(font, hint, width - 40 - font.width(hint), height - 64, Palette.MUTED, false);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawScaled(GuiGraphics graphics, String text, int x, int y, float scale,
                            int color, boolean rightAligned) {
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0f);
        float width = font.width(text) * scale;
        float drawX = rightAligned ? (x - width) / scale : (x - width / 2) / scale;
        graphics.drawString(font, text, (int) drawX, (int) (y / scale), color, false);
        graphics.pose().popPose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
