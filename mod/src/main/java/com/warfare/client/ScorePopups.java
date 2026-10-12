package com.warfare.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Les points qui montent a l'ecran a chaque elimination ("+100 ELIMINATION").
 *
 * Entierement dessine avec la police du jeu : aucun visuel ni son repris d'ailleurs.
 */
public final class ScorePopups {
    private static final int LIFETIME = 50;      // ticks d'affichage
    private static final int MAX_SHOWN = 5;

    private static final class Popup {
        final String text;
        int age;

        Popup(String text) {
            this.text = text;
        }
    }

    private static final List<Popup> POPUPS = new ArrayList<>();

    public static void add(int points, String label) {
        POPUPS.add(new Popup("+" + points + (label == null || label.isBlank() ? "" : "  " + label)));
        while (POPUPS.size() > MAX_SHOWN) POPUPS.remove(0);

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.35f, 1.6f);
        }
    }

    public static void tick() {
        Iterator<Popup> iterator = POPUPS.iterator();
        while (iterator.hasNext()) {
            if (++iterator.next().age > LIFETIME) iterator.remove();
        }
    }

    public static void clear() {
        POPUPS.clear();
    }

    /** Pile de points, alignee a droite au-dessus du centre de l'ecran. */
    public static void render(GuiGraphics graphics, int width, int height) {
        if (POPUPS.isEmpty()) return;
        var font = Minecraft.getInstance().font;
        int right = width / 2 + 120;
        int y = height / 2 - 30;

        for (int i = POPUPS.size() - 1; i >= 0; i--) {
            Popup popup = POPUPS.get(i);
            int fade = Math.min(255, (LIFETIME - popup.age) * 12);
            if (fade <= 8) continue;
            int rise = popup.age / 3;
            int color = (fade << 24) | 0x00E4C45A;
            graphics.drawString(font, Fonts.ui(popup.text), right - font.width(Fonts.ui(popup.text)), y - rise, color, true);
            y -= 12;
        }
    }

    private ScorePopups() {}
}
