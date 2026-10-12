package com.warfare.client;

import com.warfare.loadout.ClassLoadout;
import com.warfare.loadout.GunCatalog;
import com.warfare.loadout.Perk;
import com.warfare.loadout.PlayerClasses;
import com.warfare.net.WarfareNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Menu "Creer une classe".
 *
 * Habillage entierement dessine a la main (aplats sombres, liseres olive et ambre) :
 * aucune texture, aucun son et aucun visuel repris d'un jeu existant.
 */
public class ClassScreen extends Screen {
    private static final int PANEL_W = 400;
    private static final int PANEL_H = 230;
    private static final int SIDEBAR_W = 58;
    private static final int ROW_H = 12;

    private static final int C_SHADE = 0xC0000000;
    private static final int C_PANEL = 0xFF191C17;
    private static final int C_INSET = 0xFF101209;
    private static final int C_BORDER = 0xFF46523F;
    private static final int C_ACCENT = 0xFFB9922F;
    private static final int C_TEXT = 0xFFD6DACF;
    private static final int C_MUTED = 0xFF76806F;
    private static final int C_SELECTED = 0xFF2B3526;

    private int left;
    private int top;

    private int editingSlot;
    @Nullable
    private ClassLoadout draft;
    private boolean editingPrimary = true;
    @Nullable
    private String typeFilter;
    private int scroll;

    private List<GunCatalog.Entry> catalog = List.of();
    private List<String> types = List.of();
    @Nullable
    private EditBox nameBox;

    private int listX;
    private int listY;
    private int listW;
    private int listH;

    public ClassScreen() {
        super(Component.literal("Creer une classe"));
    }

    // ------------------------------------------------------------------ init

    @Override
    protected void init() {
        left = (width - PANEL_W) / 2;
        top = (height - PANEL_H) / 2;

        catalog = ClientClassData.catalog();
        types = catalog.stream().map(GunCatalog.Entry::type).distinct().toList();
        if (draft == null) draft = ClientClassData.classes.get(editingSlot);

        int contentX = left + SIDEBAR_W + 8;
        listX = contentX;
        listY = top + 86;
        listW = 186;
        listH = 104;

        // Emplacements de classe
        for (int i = 0; i < PlayerClasses.SLOTS; i++) {
            final int slot = i;
            String label = (ClientClassData.classes.selected() == i ? "▸ " : "  ")
                    + trim(ClientClassData.classes.get(i).name(), 8);
            WarButton button = new WarButton(left + 5, top + 26 + i * 20, SIDEBAR_W - 10, 17,
                    Component.literal(label), () -> selectSlot(slot));
            button.selected = i == editingSlot;
            addRenderableWidget(button);
        }

        // Nom de la classe
        nameBox = new EditBox(font, contentX + 32, top + 24, 140, 14, Component.literal("Nom"));
        nameBox.setMaxLength(ClassLoadout.MAX_NAME_LENGTH);
        nameBox.setValue(draft.name());
        nameBox.setResponder(value -> draft = draft.withName(value));
        addRenderableWidget(nameBox);

        // Choix de l'arme en cours d'edition
        WarButton primary = new WarButton(contentX, top + 46, 91, 17,
                Component.literal("PRINCIPALE"), () -> {
            editingPrimary = true;
            typeFilter = null;
            scroll = 0;
            rebuildWidgets();
        });
        primary.selected = editingPrimary;
        primary.setTooltip(Tooltip.create(gunLabel(draft.primaryGun())));
        addRenderableWidget(primary);

        WarButton secondary = new WarButton(contentX + 95, top + 46, 91, 17,
                Component.literal("SECONDAIRE"), () -> {
            editingPrimary = false;
            typeFilter = null;
            scroll = 0;
            rebuildWidgets();
        });
        secondary.selected = !editingPrimary;
        secondary.setTooltip(Tooltip.create(gunLabel(draft.secondaryGun())));
        addRenderableWidget(secondary);

        // Filtres par type d'arme
        int filterX = contentX;
        WarButton allTypes = new WarButton(filterX, top + 68, 30, 14, Component.literal("Tout"), () -> {
            typeFilter = null;
            scroll = 0;
            rebuildWidgets();
        });
        allTypes.selected = typeFilter == null;
        addRenderableWidget(allTypes);
        filterX += 32;
        for (String type : types) {
            String label = trim(GunCatalog.typeLabel(type), 9);
            int w = Math.max(28, font.width(label) + 8);
            if (filterX + w > contentX + listW) break;
            final String target = type;
            WarButton button = new WarButton(filterX, top + 68, w, 14, Component.literal(label), () -> {
                typeFilter = target;
                scroll = 0;
                rebuildWidgets();
            });
            button.selected = type.equals(typeFilter);
            addRenderableWidget(button);
            filterX += w + 2;
        }

        // Atouts
        int perkX = contentX + listW + 8;
        int perkW = PANEL_W - SIDEBAR_W - listW - 32;
        int perkY = top + 96;
        for (int category = 1; category <= Perk.CATEGORIES; category++) {
            int slotY = perkY;
            for (Perk perk : Perk.ofCategory(category)) {
                WarButton button = new WarButton(perkX, slotY, perkW, 15,
                        Component.literal(trim(perk.title(), 18)), () -> {
                    draft = draft.withPerk(perk);
                    rebuildWidgets();
                });
                button.selected = draft.has(perk);
                button.setTooltip(Tooltip.create(Component.literal(perk.description())));
                addRenderableWidget(button);
                slotY += 17;
            }
            perkY = slotY + 8;
        }

        // Actions
        addRenderableWidget(new WarButton(contentX, top + PANEL_H - 26, 90, 18,
                Component.literal("ENREGISTRER"), this::save));
        addRenderableWidget(new WarButton(contentX + 94, top + PANEL_H - 26, 90, 18,
                Component.literal("EQUIPER"), this::equip));
        addRenderableWidget(new WarButton(left + PANEL_W - 24, top + 5, 19, 14,
                Component.literal("X"), this::onClose));
    }

    private void selectSlot(int slot) {
        save();
        editingSlot = slot;
        draft = ClientClassData.classes.get(slot);
        scroll = 0;
        rebuildWidgets();
    }

    private void save() {
        if (draft == null) return;
        ClientClassData.classes.set(editingSlot, draft);
        WarfareNetwork.CHANNEL.sendToServer(new WarfareNetwork.SaveClass(editingSlot, draft));
    }

    private void equip() {
        save();
        ClientClassData.classes.select(editingSlot);
        WarfareNetwork.CHANNEL.sendToServer(new WarfareNetwork.SelectClass(editingSlot));
        rebuildWidgets();
    }

    // ---------------------------------------------------------------- rendu

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, C_SHADE);

        // Cadre principal
        graphics.fill(left, top, left + PANEL_W, top + PANEL_H, C_PANEL);
        outline(graphics, left, top, PANEL_W, PANEL_H, C_BORDER);
        graphics.fill(left + 1, top + 1, left + PANEL_W - 1, top + 19, C_INSET);
        graphics.fill(left + 1, top + 19, left + PANEL_W - 1, top + 20, C_ACCENT);
        graphics.drawString(font, "CREER UNE CLASSE", left + 8, top + 6, C_ACCENT, false);
        graphics.drawString(font, "emplacement " + (editingSlot + 1) + "/" + PlayerClasses.SLOTS,
                left + PANEL_W - 110, top + 6, C_MUTED, false);

        // Colonne des emplacements
        graphics.fill(left + 1, top + 20, left + SIDEBAR_W, top + PANEL_H - 1, C_INSET);
        graphics.vLine(left + SIDEBAR_W, top + 20, top + PANEL_H - 1, C_BORDER);

        int contentX = left + SIDEBAR_W + 8;
        graphics.drawString(font, "NOM", contentX, top + 27, C_MUTED, false);

        // Resume des armes choisies
        graphics.drawString(font, trim(gunLabel(draft.primaryGun()).getString(), 22),
                contentX + 2, top + 64 - 11, C_TEXT, false);
        graphics.drawString(font, trim(gunLabel(draft.secondaryGun()).getString(), 22),
                contentX + 97, top + 64 - 11, C_TEXT, false);

        // Liste des armes
        renderGunList(graphics, mouseX, mouseY);

        // Titre de la colonne des atouts
        int perkX = contentX + listW + 8;
        graphics.drawString(font, "ATOUTS", perkX, top + 86, C_ACCENT, false);
        graphics.drawString(font, "Equipee : " + trim(ClientClassData.classes.current().name(), 14),
                perkX, top + PANEL_H - 22, C_MUTED, false);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderGunList(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.fill(listX, listY, listX + listW, listY + listH, C_INSET);
        outline(graphics, listX, listY, listW, listH, C_BORDER);

        List<GunCatalog.Entry> visible = filtered();
        int maxRows = listH / ROW_H;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, visible.size() - maxRows)));
        String current = editingPrimary ? draft.primaryGun() : draft.secondaryGun();

        graphics.enableScissor(listX + 1, listY + 1, listX + listW - 1, listY + listH - 1);
        for (int row = 0; row < maxRows && row + scroll < visible.size(); row++) {
            GunCatalog.Entry entry = visible.get(row + scroll);
            int y = listY + 1 + row * ROW_H;
            boolean chosen = entry.gunId().equals(current);
            boolean hovered = mouseX >= listX && mouseX < listX + listW && mouseY >= y && mouseY < y + ROW_H;

            if (chosen) {
                graphics.fill(listX + 1, y, listX + listW - 1, y + ROW_H, C_SELECTED);
                graphics.fill(listX + 1, y, listX + 3, y + ROW_H, C_ACCENT);
            } else if (hovered) {
                graphics.fill(listX + 1, y, listX + listW - 1, y + ROW_H, 0xFF20241D);
            }
            graphics.drawString(font, trim(nameOf(entry), 24), listX + 7, y + 2,
                    chosen ? C_ACCENT : C_TEXT, false);
            graphics.drawString(font, trim(GunCatalog.typeLabel(entry.type()), 10),
                    listX + listW - 62, y + 2, C_MUTED, false);
        }
        graphics.disableScissor();

        if (visible.isEmpty()) {
            graphics.drawString(font, "Aucune arme disponible", listX + 7, listY + 6, C_MUTED, false);
        }
        // Barre de defilement
        if (visible.size() > maxRows) {
            int trackH = listH - 2;
            int thumbH = Math.max(10, trackH * maxRows / visible.size());
            int thumbY = listY + 1 + (trackH - thumbH) * scroll / Math.max(1, visible.size() - maxRows);
            graphics.fill(listX + listW - 4, listY + 1, listX + listW - 1, listY + listH - 1, 0xFF14170F);
            graphics.fill(listX + listW - 4, thumbY, listX + listW - 1, thumbY + thumbH, C_BORDER);
        }
    }

    private static void outline(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        graphics.hLine(x, x + w - 1, y, color);
        graphics.hLine(x, x + w - 1, y + h - 1, color);
        graphics.vLine(x, y, y + h - 1, color);
        graphics.vLine(x + w - 1, y, y + h - 1, color);
    }

    // ---------------------------------------------------------------- souris

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= listX && mouseX < listX + listW
                && mouseY >= listY && mouseY < listY + listH) {
            int row = (int) ((mouseY - listY - 1) / ROW_H) + scroll;
            List<GunCatalog.Entry> visible = filtered();
            if (row >= 0 && row < visible.size()) {
                String gunId = visible.get(row).gunId();
                draft = editingPrimary ? draft.withPrimary(gunId) : draft.withSecondary(gunId);
                rebuildWidgets();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX >= listX && mouseX < listX + listW && mouseY >= listY && mouseY < listY + listH) {
            scroll -= (int) Math.signum(delta);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        save();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---------------------------------------------------------------- outils

    private List<GunCatalog.Entry> filtered() {
        if (typeFilter == null) return catalog;
        List<GunCatalog.Entry> out = new ArrayList<>();
        for (GunCatalog.Entry entry : catalog) {
            if (entry.type().equals(typeFilter)) out.add(entry);
        }
        return out;
    }

    private static String nameOf(GunCatalog.Entry entry) {
        Component translated = Component.translatable(entry.nameKey());
        String text = translated.getString();
        return text.equals(entry.nameKey()) ? entry.gunId() : text;
    }

    private Component gunLabel(String gunId) {
        for (GunCatalog.Entry entry : catalog) {
            if (entry.gunId().equals(gunId)) return Component.literal(nameOf(entry));
        }
        ResourceLocation id = ResourceLocation.tryParse(gunId);
        return Component.literal(id == null ? gunId : id.getPath());
    }

    private static String trim(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }

    /** Bouton dessine a la main, dans le style du menu. */
    private static final class WarButton extends AbstractWidget {
        private final Runnable action;
        private boolean selected;

        WarButton(int x, int y, int width, int height, Component message, Runnable action) {
            super(x, y, width, height, message);
            this.action = action;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int background = selected ? C_SELECTED : (isHovered() ? 0xFF232821 : C_INSET);
            graphics.fill(getX(), getY(), getX() + width, getY() + height, background);
            outline(graphics, getX(), getY(), width, height, selected ? C_ACCENT : C_BORDER);
            if (selected) graphics.fill(getX(), getY(), getX() + 2, getY() + height, C_ACCENT);

            int color = selected ? C_ACCENT : (isHovered() ? 0xFFFFFFFF : C_TEXT);
            graphics.drawCenteredString(net.minecraft.client.Minecraft.getInstance().font,
                    getMessage(), getX() + width / 2, getY() + (height - 8) / 2, color);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            action.run();
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
