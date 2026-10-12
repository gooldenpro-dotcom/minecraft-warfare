package com.warfare.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Polices de l'interface, embarquees dans le jar du mod : le jar est charge comme un
 * pack de ressources, donc les joueurs n'ont rien a installer de plus.
 *
 * On applique la police par le style du Component, qui est la voie supportee par
 * Minecraft : aucune reflexion, aucun mixin, aucun pack a servir separement.
 *
 * Caractere : Rajdhani (Indian Type Foundry), licence SIL Open Font 1.1, redistribuable.
 * Licence complete dans assets/warfare/font/LICENCE-Rajdhani-OFL.txt.
 * Ce n'est pas la police de Call of Duty : c'est une police libre du meme registre.
 */
public final class Fonts {
    public static final ResourceLocation UI = new ResourceLocation("warfare", "ui");
    public static final ResourceLocation TITLE = new ResourceLocation("warfare", "title");

    public static Component ui(String text) {
        return Component.literal(text).withStyle(style -> style.withFont(UI));
    }

    public static Component title(String text) {
        return Component.literal(text).withStyle(style -> style.withFont(TITLE));
    }

    /** Largeur d'une chaine dans la police de l'interface. */
    public static int width(Font font, String text) {
        return font.width(ui(text));
    }

    /**
     * Titre interlettre : Minecraft ne sait pas espacer les caracteres, on dessine
     * donc glyphe par glyphe en ajoutant l'ecart demande.
     */
    public static void drawTracked(GuiGraphics graphics, Font font, String text,
                                   int x, int y, int color, int tracking, float scale) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(scale, scale, 1.0f);
        int cursor = 0;
        for (int i = 0; i < text.length(); i++) {
            Component glyph = title(String.valueOf(text.charAt(i)));
            graphics.drawString(font, glyph, cursor, 0, color, false);
            cursor += font.width(glyph) + tracking;
        }
        graphics.pose().popPose();
    }

    public static int trackedWidth(Font font, String text, int tracking, float scale) {
        int total = 0;
        for (int i = 0; i < text.length(); i++) {
            total += font.width(title(String.valueOf(text.charAt(i)))) + tracking;
        }
        return Math.round(Math.max(0, total - tracking) * scale);
    }

    private Fonts() {}
}
