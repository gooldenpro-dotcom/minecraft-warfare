package com.warfare.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/** Bouton dessine a la main, dans le style du mod. */
public class WarButton extends AbstractWidget {
    private final Runnable action;
    public boolean selected;

    public WarButton(int x, int y, int width, int height, Component message, Runnable action) {
        super(x, y, width, height, message);
        this.action = action;
    }

    public WarButton selected(boolean value) {
        this.selected = value;
        return this;
    }

    public WarButton enabled(boolean value) {
        this.active = value;
        return this;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int background = !active ? Palette.INSET
                : selected ? Palette.SELECTED
                : isHovered() ? Palette.HOVER : Palette.INSET;
        graphics.fill(getX(), getY(), getX() + width, getY() + height, background);
        Palette.outline(graphics, getX(), getY(), width, height,
                !active ? Palette.DISABLED : selected ? Palette.ACCENT : Palette.BORDER);
        if (selected && active) graphics.fill(getX(), getY(), getX() + 2, getY() + height, Palette.ACCENT);

        int color = !active ? Palette.DISABLED
                : selected ? Palette.ACCENT
                : isHovered() ? 0xFFFFFFFF : Palette.TEXT;
        graphics.drawCenteredString(Minecraft.getInstance().font, Fonts.ui(getMessage().getString()),
                getX() + width / 2, getY() + (height - 8) / 2, color);
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
