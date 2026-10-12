package com.warfare.client;

import net.minecraft.client.gui.GuiGraphics;

/** Identite visuelle du mod : aplats sombres, liseres olive, accent ambre. */
public final class Palette {
    public static final int SHADE = 0xC0000000;
    public static final int PANEL = 0xFF191C17;
    public static final int INSET = 0xFF101209;
    public static final int BORDER = 0xFF46523F;
    public static final int ACCENT = 0xFFB9922F;
    public static final int TEXT = 0xFFD6DACF;
    public static final int MUTED = 0xFF76806F;
    public static final int SELECTED = 0xFF2B3526;
    public static final int HOVER = 0xFF232821;
    public static final int DISABLED = 0xFF4A4F46;

    public static void outline(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.hLine(x, x + w - 1, y, color);
        g.hLine(x, x + w - 1, y + h - 1, color);
        g.vLine(x, y, y + h - 1, color);
        g.vLine(x + w - 1, y, y + h - 1, color);
    }

    /** Cadre standard : fond, liseres, bandeau de titre souligne d'ambre. */
    public static void frame(GuiGraphics g, net.minecraft.client.gui.Font font,
                             int x, int y, int w, int h, String title, String corner) {
        g.fill(x, y, x + w, y + h, PANEL);
        outline(g, x, y, w, h, BORDER);
        g.fill(x + 1, y + 1, x + w - 1, y + 19, INSET);
        g.fill(x + 1, y + 19, x + w - 1, y + 20, ACCENT);
        g.drawString(font, title, x + 8, y + 6, ACCENT, false);
        if (corner != null) {
            g.drawString(font, corner, x + w - 8 - font.width(corner), y + 6, MUTED, false);
        }
    }

    private Palette() {}
}
