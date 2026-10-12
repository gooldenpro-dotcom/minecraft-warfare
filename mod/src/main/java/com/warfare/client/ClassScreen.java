package com.warfare.client;

import com.warfare.loadout.ClassLoadout;
import com.warfare.loadout.GunCatalog;
import com.warfare.loadout.Perk;
import com.warfare.loadout.PlayerClasses;
import com.warfare.progression.Unlocks;
import com.warfare.net.WarfareNetwork;
import com.warfare.streak.Killstreak;
import net.minecraft.client.Minecraft;
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
 * Menu "Creer une classe" : armes, atouts et killstreaks, en trois onglets.
 *
 * Habillage entierement dessine a la main (aplats sombres, liseres olive et ambre) :
 * aucune texture, aucun son et aucun visuel repris d'un jeu existant.
 */
public class ClassScreen extends Screen {
    private static final int PANEL_W = 440;
    private static final int PANEL_H = 276;
    private static final int SIDEBAR_W = 70;
    private static final int ROW_H = 12;

    private static final int C_SHADE = Palette.SHADE;
    private static final int C_PANEL = Palette.PANEL;
    private static final int C_INSET = Palette.INSET;
    private static final int C_BORDER = Palette.BORDER;
    private static final int C_ACCENT = Palette.ACCENT;
    private static final int C_TEXT = Palette.TEXT;
    private static final int C_MUTED = Palette.MUTED;
    private static final int C_SELECTED = Palette.SELECTED;

    public enum Tab { WEAPONS, PERKS, STREAKS }

    private int left;
    private int top;
    private int contentX;

    private Tab tab = Tab.WEAPONS;
    private int editingSlot;
    @Nullable
    private ClassLoadout draft;
    private boolean editingPrimary = true;
    @Nullable
    private String typeFilter;
    private int scroll;

    @Nullable
    private String locked;

    private List<GunCatalog.Entry> catalog = List.of();
    private List<String> types = List.of();

    private int listX;
    private int listY;
    private int listW;
    private int listH;

    @Nullable
    private final Screen parent;

    public ClassScreen() {
        this(Tab.WEAPONS, null);
    }

    public ClassScreen(Tab initialTab, @Nullable Screen parent) {
        super(Component.literal("Creer une classe"));
        this.tab = initialTab;
        this.parent = parent;
    }

    // ------------------------------------------------------------------ init

    @Override
    protected void init() {
        left = (width - PANEL_W) / 2;
        top = (height - PANEL_H) / 2;
        contentX = left + SIDEBAR_W + 8;

        catalog = ClientClassData.catalog();
        types = catalog.stream().map(GunCatalog.Entry::type).distinct().toList();
        if (draft == null) draft = ClientClassData.classes.get(editingSlot);

        buildSlots();
        buildHeader();
        switch (tab) {
            case WEAPONS -> buildWeaponsTab();
            case PERKS -> buildPerksTab();
            case STREAKS -> buildStreaksTab();
        }
        buildFooter();
    }

    private void buildSlots() {
        for (int i = 0; i < PlayerClasses.SLOTS; i++) {
            final int slot = i;
            String label = (ClientClassData.classes.selected() == i ? "▸ " : "  ")
                    + trim(ClientClassData.classes.get(i).name(), 10);
            WarButton button = new WarButton(left + 5, top + 26 + i * 20, SIDEBAR_W - 10, 17,
                    Component.literal(label), () -> selectSlot(slot));
            button.selected = i == editingSlot;
            addRenderableWidget(button);
        }
    }

    private void buildHeader() {
        EditBox nameBox = new EditBox(font, contentX + 30, top + 24, 170, 14, Component.literal("Nom"));
        nameBox.setMaxLength(ClassLoadout.MAX_NAME_LENGTH);
        nameBox.setValue(draft.name());
        nameBox.setResponder(value -> draft = draft.withName(value));
        addRenderableWidget(nameBox);

        int tabX = contentX;
        for (Tab value : Tab.values()) {
            WarButton button = new WarButton(tabX, top + 44, 96, 16, Component.literal(labelOf(value)), () -> {
                tab = value;
                rebuildWidgets();
            });
            button.selected = tab == value;
            addRenderableWidget(button);
            tabX += 100;
        }
    }

    private static String labelOf(Tab tab) {
        return switch (tab) {
            case WEAPONS -> "ARMES";
            case PERKS -> "ATOUTS";
            case STREAKS -> "KILLSTREAKS";
        };
    }

    private void buildWeaponsTab() {
        listX = contentX;
        listY = top + 120;
        listW = 220;
        listH = 120;

        WarButton primary = new WarButton(contentX, top + 66, 108, 16,
                Component.literal("PRINCIPALE"), () -> {
            editingPrimary = true;
            typeFilter = null;
            scroll = 0;
            rebuildWidgets();
        });
        primary.selected = editingPrimary;
        addRenderableWidget(primary);

        WarButton secondary = new WarButton(contentX + 112, top + 66, 108, 16,
                Component.literal("SECONDAIRE"), () -> {
            editingPrimary = false;
            typeFilter = null;
            scroll = 0;
            rebuildWidgets();
        });
        secondary.selected = !editingPrimary;
        addRenderableWidget(secondary);

        // Filtres par type : on passe a la ligne suivante plutot que d'en perdre.
        int filterX = contentX;
        int filterY = top + 86;
        WarButton allTypes = new WarButton(filterX, filterY, 32, 14, Component.literal("Tout"), () -> {
            typeFilter = null;
            scroll = 0;
            rebuildWidgets();
        });
        allTypes.selected = typeFilter == null;
        addRenderableWidget(allTypes);
        filterX += 34;

        for (String type : types) {
            String label = GunCatalog.typeShort(type);
            int w = font.width(label) + 10;
            if (filterX + w > contentX + listW) {
                filterX = contentX;
                filterY += 16;
                if (filterY > top + 102) break;   // deux lignes au maximum
            }
            final String target = type;
            WarButton button = new WarButton(filterX, filterY, w, 14, Component.literal(label), () -> {
                typeFilter = target;
                scroll = 0;
                rebuildWidgets();
            });
            button.selected = type.equals(typeFilter);
            addRenderableWidget(button);
            filterX += w + 2;
        }
    }

    private void buildPerksTab() {
        int y = top + 76;
        for (int category = 1; category <= Perk.CATEGORIES; category++) {
            int x = contentX;
            for (Perk perk : Perk.ofCategory(category)) {
                boolean unlocked = Unlocks.perkUnlocked(perk, ClientClassData.unlockLevel);
                String label = unlocked ? trim(perk.title(), 22)
                        : trim(perk.title(), 14) + " [NIV." + Unlocks.perkLevel(perk) + "]";
                WarButton button = new WarButton(x, y, 168, 16, Component.literal(label), () -> {
                    draft = draft.withPerk(perk);
                    rebuildWidgets();
                });
                button.selected = draft.has(perk);
                button.enabled(unlocked);
                button.setTooltip(Tooltip.create(Component.literal(unlocked ? perk.description()
                        : "Debloque au niveau " + Unlocks.perkLevel(perk))));
                addRenderableWidget(button);
                x += 172;
            }
            y += 38;
        }
    }

    private void buildStreaksTab() {
        int y = top + 70;
        for (Killstreak streak : Killstreak.sorted()) {
            boolean unlocked = Unlocks.streakUnlocked(streak, ClientClassData.unlockLevel);
            String label = unlocked
                    ? streak.threshold() + " — " + trim(streak.title(), 20)
                    : trim(streak.title(), 16) + " [NIV." + Unlocks.streakLevel(streak) + "]";
            WarButton button = new WarButton(contentX, y, 190, 18, Component.literal(label),
                    () -> {
                        draft = draft.withStreakToggled(streak);
                        rebuildWidgets();
                    });
            button.selected = draft.has(streak);
            button.enabled(unlocked);
            button.setTooltip(Tooltip.create(Component.literal(unlocked ? streak.description()
                    : "Debloque au niveau " + Unlocks.streakLevel(streak))));
            addRenderableWidget(button);
            y += 24;
        }
    }

    private void buildFooter() {
        addRenderableWidget(new WarButton(contentX, top + PANEL_H - 26, 108, 18,
                Component.literal("ENREGISTRER"), this::save));
        addRenderableWidget(new WarButton(contentX + 112, top + PANEL_H - 26, 108, 18,
                Component.literal("EQUIPER"), this::equip));
        addRenderableWidget(new WarButton(left + PANEL_W - 24, top + 4, 19, 14,
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
        graphics.fill(left, top, left + PANEL_W, top + PANEL_H, C_PANEL);
        outline(graphics, left, top, PANEL_W, PANEL_H, C_BORDER);
        graphics.fill(left + 1, top + 1, left + PANEL_W - 1, top + 19, C_INSET);
        graphics.fill(left + 1, top + 19, left + PANEL_W - 1, top + 20, C_ACCENT);
        graphics.drawString(font, "CREER UNE CLASSE", left + 8, top + 6, C_ACCENT, false);
        String header = "niveau " + ClientClassData.level
                + (ClientClassData.prestige > 0 ? " - P" + ClientClassData.prestige : "")
                + "   emplacement " + (editingSlot + 1) + "/" + PlayerClasses.SLOTS;
        graphics.drawString(font, header, left + PANEL_W - 30 - font.width(header), top + 6,
                C_MUTED, false);

        graphics.fill(left + 1, top + 20, left + SIDEBAR_W, top + PANEL_H - 1, C_INSET);
        graphics.vLine(left + SIDEBAR_W, top + 20, top + PANEL_H - 1, C_BORDER);
        graphics.drawString(font, "NOM", contentX, top + 27, C_MUTED, false);

        switch (tab) {
            case WEAPONS -> renderWeaponsTab(graphics, mouseX, mouseY);
            case PERKS -> renderPerksTab(graphics);
            case STREAKS -> renderStreaksTab(graphics);
        }

        graphics.drawString(font, "Equipee : " + trim(ClientClassData.classes.current().name(), 14),
                contentX + 228, top + PANEL_H - 21, C_MUTED, false);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderWeaponsTab(GuiGraphics graphics, int mouseX, int mouseY) {
        int sideX = contentX + listW + 8;
        graphics.drawString(font, "PRINCIPALE", sideX, top + 70, C_MUTED, false);
        graphics.drawString(font, trim(gunLabel(draft.primaryGun()), 20), sideX, top + 81, C_TEXT, false);
        graphics.drawString(font, "SECONDAIRE", sideX, top + 99, C_MUTED, false);
        graphics.drawString(font, trim(gunLabel(draft.secondaryGun()), 20), sideX, top + 110, C_TEXT, false);
        graphics.drawString(font, "molette : defiler", sideX, top + 136, C_MUTED, false);
        graphics.drawString(font, "clic : choisir", sideX, top + 147, C_MUTED, false);
        if (locked != null) {
            graphics.drawString(font, trim(locked, 26), sideX, top + 165, 0xFFB4534A, false);
        }

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

            boolean unlocked = Unlocks.gunUnlocked(entry.gunId(), ClientClassData.unlockLevel);

            if (chosen) {
                graphics.fill(listX + 1, y, listX + listW - 1, y + ROW_H, C_SELECTED);
                graphics.fill(listX + 1, y, listX + 3, y + ROW_H, C_ACCENT);
            } else if (hovered) {
                graphics.fill(listX + 1, y, listX + listW - 1, y + ROW_H, 0xFF20241D);
            }
            graphics.drawString(font, trim(nameOf(entry), 22), listX + 7, y + 2,
                    !unlocked ? 0xFF6A5252 : chosen ? C_ACCENT : C_TEXT, false);
            String right = unlocked ? GunCatalog.typeShort(entry.type())
                    : "NIV. " + Unlocks.gunLevel(entry.gunId());
            graphics.drawString(font, right, listX + listW - 10 - font.width(right), y + 2,
                    unlocked ? C_MUTED : 0xFFB4534A, false);
        }
        graphics.disableScissor();

        if (visible.isEmpty()) {
            graphics.drawString(font, "Aucune arme disponible", listX + 7, listY + 6, C_MUTED, false);
        }
        if (visible.size() > maxRows) {
            int trackH = listH - 2;
            int thumbH = Math.max(10, trackH * maxRows / visible.size());
            int thumbY = listY + 1 + (trackH - thumbH) * scroll / Math.max(1, visible.size() - maxRows);
            graphics.fill(listX + listW - 4, listY + 1, listX + listW - 1, listY + listH - 1, 0xFF14170F);
            graphics.fill(listX + listW - 4, thumbY, listX + listW - 1, thumbY + thumbH, C_BORDER);
        }
    }

    private void renderPerksTab(GuiGraphics graphics) {
        int y = top + 66;
        for (int category = 1; category <= Perk.CATEGORIES; category++) {
            graphics.drawString(font, "CATEGORIE " + category, contentX, y, C_MUTED, false);
            graphics.drawString(font, trim(draft.perk(category).description(), 54),
                    contentX, y + 28, C_TEXT, false);
            y += 38;
        }
    }

    private void renderStreaksTab(GuiGraphics graphics) {
        graphics.drawString(font, "3 killstreaks par classe - seuil en eliminations consecutives",
                contentX, top + 60, C_MUTED, false);
        int y = top + 70;
        for (Killstreak streak : Killstreak.sorted()) {
            graphics.drawString(font, trim(streak.description(), 36), contentX + 176, y + 5,
                    draft.has(streak) ? C_TEXT : C_MUTED, false);
            y += 24;
        }
        int chosen = draft.streaks().size();
        graphics.drawString(font, "selection : " + chosen + "/" + Killstreak.EQUIPPED,
                contentX, top + PANEL_H - 44, chosen == Killstreak.EQUIPPED ? C_ACCENT : C_MUTED, false);
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
        if (tab == Tab.WEAPONS && button == 0 && mouseX >= listX && mouseX < listX + listW
                && mouseY >= listY && mouseY < listY + listH) {
            int row = (int) ((mouseY - listY - 1) / ROW_H) + scroll;
            List<GunCatalog.Entry> visible = filtered();
            if (row >= 0 && row < visible.size()) {
                String gunId = visible.get(row).gunId();
                if (!Unlocks.gunUnlocked(gunId, ClientClassData.unlockLevel)) {
                    locked = "Debloquee au niveau " + Unlocks.gunLevel(gunId);
                    return true;
                }
                locked = null;
                draft = editingPrimary ? draft.withPrimary(gunId) : draft.withSecondary(gunId);
                rebuildWidgets();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (tab == Tab.WEAPONS && mouseX >= listX && mouseX < listX + listW
                && mouseY >= listY && mouseY < listY + listH) {
            scroll -= (int) Math.signum(delta);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        save();
        if (parent != null && minecraft != null) {
            minecraft.setScreen(parent);
            return;
        }
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
        String text = Component.translatable(entry.nameKey()).getString();
        return text.equals(entry.nameKey()) ? entry.gunId() : text;
    }

    private String gunLabel(String gunId) {
        for (GunCatalog.Entry entry : catalog) {
            if (entry.gunId().equals(gunId)) return nameOf(entry);
        }
        ResourceLocation id = ResourceLocation.tryParse(gunId);
        return id == null ? gunId : id.getPath();
    }

    private static String trim(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }

}
