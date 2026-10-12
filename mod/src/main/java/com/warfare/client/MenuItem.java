package com.warfare.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * Entree de menu a plat : du texte aligne a droite, une barre pleine sur l'element
 * survole ou actif, et un bandeau oblique en bout de ligne. Aucun cadre, aucune texture.
 */
public class MenuItem extends AbstractWidget {
    private final Runnable action;
    public boolean selected;

    public MenuItem(int x, int y, int width, int height, Component message, Runnable action) {
        super(x, y, width, height, message);
        this.action = action;
    }

    public MenuItem enabled(boolean value) {
        this.active = value;
        return this;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean highlight = active && (isHovered() || selected);
        int right = getX() + width;

        if (highlight) {
            graphics.fill(-width * 4, getY(), right + 4, getY() + height, Palette.BAR_ACTIVE);
            graphics.fill(right + 4, getY(), right + 6, getY() + height, Palette.ACCENT);
            // bandeau oblique, purement decoratif
            for (int i = 0; i < height; i++) {
                graphics.fill(right + 8 + (height - i) / 2, getY() + i,
                        right + 12 + (height - i) / 2, getY() + i + 1, Palette.ACCENT);
            }
        }

        int color = !active ? 0xFF55594F : highlight ? 0xFFFFFFFF : Palette.TEXT;
        var font = Minecraft.getInstance().font;
        net.minecraft.network.chat.Component label = Fonts.ui(getMessage().getString());
        graphics.drawString(font, label, right - font.width(label),
                getY() + (height - 8) / 2, color, false);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (active) action.run();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
