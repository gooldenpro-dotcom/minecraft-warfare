package com.warfare.client;

import com.tacz.guns.api.TimelessAPI;
import com.warfare.game.Loadout;
import com.warfare.loadout.ClassLoadout;
import com.warfare.loadout.GunCatalog;
import com.warfare.loadout.Perk;
import com.warfare.loadout.PlayerClasses;
import com.warfare.net.WarfareNetwork;
import com.warfare.progression.Unlocks;
import com.warfare.streak.Killstreak;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Choix de la classe : la liste des emplacements a gauche, l'apercu complet de la
 * classe selectionnee a droite (armes avec leur modele, atouts, killstreaks).
 *
 * C'est l'ecran d'entree ; la modification passe par ClassScreen, qui reste
 * l'editeur detaille. Meme grille centree que le menu d'accueil.
 */
public class ChooseClassScreen extends Screen {
    private static final int CONTENT_MAX = 1080;
    private static final int COLUMN_GAP = 48;

    @Nullable
    private final Screen parent;
    private int selected;

    private int contentX;
    private int contentW;
    private int menuRight;
    private int panelX;
    private int panelW;
    private int headerY;
    private int bodyY;
    private int footerY;

    /** Pile d'objets construits une fois par rendu de classe, pour l'apercu. */
    private ItemStack primaryStack = ItemStack.EMPTY;
    private ItemStack secondaryStack = ItemStack.EMPTY;
    private int previewFor = -1;

    public ChooseClassScreen(@Nullable Screen parent) {
        super(Component.literal("Choisir une classe"));
        this.parent = parent;
        this.selected = ClientClassData.classes.selected();
    }

    @Override
    protected void init() {
        int gutter = Math.max(24, Math.round(width * 0.06f));
        contentW = Math.min(CONTENT_MAX, width - gutter * 2);
        contentX = (width - contentW) / 2;
        int menuW = Math.max(190, Math.round(contentW * 0.36f));
        menuRight = contentX + menuW;
        panelX = menuRight + COLUMN_GAP;
        panelW = contentX + contentW - panelX;
        headerY = Math.max(28, Math.round(height * 0.11f));
        bodyY = headerY + 54;
        footerY = height - Math.max(34, Math.round(height * 0.07f));

        int count = PlayerClasses.SLOTS + 2;              // classes + EQUIPER + MODIFIER
        int step = Math.max(16, Math.min(28, (footerY - 20 - bodyY) / count));
        int itemHeight = Math.max(12, step - 4);

        int y = bodyY;
        for (int i = 0; i < PlayerClasses.SLOTS; i++) {
            final int slot = i;
            MenuItem item = new MenuItem(contentX, y, menuW, itemHeight,
                    Component.literal(ClientClassData.classes.get(i).name()), () -> {
                selected = slot;
                previewFor = -1;
                rebuildWidgets();
            });
            item.selected = i == selected;
            addRenderableWidget(item);
            y += step;
        }

        y += 10;
        boolean equipped = selected == ClientClassData.classes.selected();
        addRenderableWidget(new MenuItem(contentX, y, menuW, itemHeight,
                Component.literal(equipped ? "CLASSE EQUIPEE" : "EQUIPER"), this::equip)
                .enabled(!equipped));
        y += step;
        addRenderableWidget(new MenuItem(contentX, y, menuW, itemHeight,
                Component.literal("MODIFIER"),
                () -> minecraft.setScreen(new ClassScreen(ClassScreen.Tab.WEAPONS, this, selected))));
    }

    private void equip() {
        ClientClassData.classes.select(selected);
        WarfareNetwork.CHANNEL.sendToServer(new WarfareNetwork.SelectClass(selected));
        rebuildWidgets();
    }

    @Override
    public void onClose() {
        if (parent != null && minecraft != null) {
            minecraft.setScreen(parent);
            return;
        }
        super.onClose();
    }

    // -------------------------------------------------------------------- rendu

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fillGradient(0, 0, width, height, 0xF00A0B09, 0xFA060706);

        Fonts.drawTracked(graphics, font, "CHOISIR UNE CLASSE", contentX, headerY, 0xFFFFFFFF, 3, 1.6f);
        graphics.fill(contentX, headerY + 26, contentX + contentW, headerY + 27, Palette.ACCENT);
        String rank = "NIVEAU " + ClientClassData.level
                + (ClientClassData.prestige > 0 ? "   PRESTIGE " + ClientClassData.prestige : "");
        graphics.drawString(font, Fonts.ui(rank),
                contentX + contentW - Fonts.width(font, rank), headerY + 33, Palette.MUTED, false);
        graphics.drawString(font, Fonts.ui(PlayerClasses.SLOTS + " emplacements"),
                contentX, headerY + 33, Palette.MUTED, false);

        // Filet au-dessus des actions
        int separatorY = bodyY + PlayerClasses.SLOTS
                * Math.max(16, Math.min(28, (footerY - 20 - bodyY) / (PlayerClasses.SLOTS + 2))) + 3;
        graphics.fill(contentX, separatorY, menuRight, separatorY + 1, Palette.RULE);

        renderPreview(graphics);

        graphics.fill(contentX, footerY, contentX + contentW, footerY + 1, 0x55B9922F);
        graphics.drawString(font, Fonts.ui("ECHAP  retour     MODIFIER pour changer armes, atouts et killstreaks"),
                contentX, footerY + 8, Palette.MUTED, false);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderPreview(GuiGraphics graphics) {
        ClassLoadout loadout = ClientClassData.classes.get(selected);
        if (previewFor != selected) {
            primaryStack = displayStack(loadout.primaryGun());
            secondaryStack = displayStack(loadout.secondaryGun());
            previewFor = selected;
        }

        // Bandeau du nom de la classe
        graphics.fill(panelX, bodyY - 22, panelX + panelW, bodyY - 2, 0x33FFFFFF);
        Fonts.drawTracked(graphics, font, loadout.name().toUpperCase(Locale.ROOT),
                panelX + 10, bodyY - 18, 0xFFFFFFFF, 2, 1.2f);

        int y = bodyY + 10;
        y = renderWeapon(graphics, "ARME PRINCIPALE", loadout.primaryGun(), primaryStack, y);
        y = renderWeapon(graphics, "ARME SECONDAIRE", loadout.secondaryGun(), secondaryStack, y);

        // Atouts
        graphics.fill(panelX, y, panelX + panelW, y + 1, Palette.RULE);
        y += 10;
        Fonts.drawTracked(graphics, font, "ATOUTS", panelX, y, Palette.MUTED, 2, 1.0f);
        y += 14;
        for (int category = 1; category <= Perk.CATEGORIES; category++) {
            Perk perk = loadout.perk(category);
            boolean locked = !Unlocks.perkUnlocked(perk, ClientClassData.unlockLevel);
            graphics.fill(panelX, y + 2, panelX + 3, y + 11, locked ? 0x66FF5555 : Palette.ACCENT);
            graphics.drawString(font, Fonts.ui(perk.title()), panelX + 12, y,
                    locked ? 0xFFB06060 : Palette.TEXT, false);
            String note = locked ? "NIV. " + Unlocks.perkLevel(perk) : perk.description();
            graphics.drawString(font, Fonts.ui(note),
                    panelX + panelW - Fonts.width(font, note), y, Palette.MUTED, false);
            y += 15;
        }

        // Killstreaks
        y += 6;
        graphics.fill(panelX, y, panelX + panelW, y + 1, Palette.RULE);
        y += 10;
        Fonts.drawTracked(graphics, font, "KILLSTREAKS", panelX, y, Palette.MUTED, 2, 1.0f);
        y += 14;
        for (Killstreak streak : loadout.streaksNormalized()) {
            boolean locked = !Unlocks.streakUnlocked(streak, ClientClassData.unlockLevel);
            String threshold = streak.threshold() + " ELIM.";
            graphics.drawString(font, Fonts.ui(threshold), panelX, y, Palette.ACCENT, false);
            graphics.drawString(font, Fonts.ui(streak.title()), panelX + 56, y,
                    locked ? 0xFFB06060 : Palette.TEXT, false);
            if (locked) {
                String note = "NIV. " + Unlocks.streakLevel(streak);
                graphics.drawString(font, Fonts.ui(note),
                        panelX + panelW - Fonts.width(font, note), y, Palette.MUTED, false);
            }
            y += 15;
        }
    }

    /** Une arme : intitule, nom, et le modele de l'objet rendu en grand. */
    private int renderWeapon(GuiGraphics graphics, String label, String gunId, ItemStack stack, int y) {
        Fonts.drawTracked(graphics, font, label, panelX, y, Palette.MUTED, 2, 1.0f);
        String name = gunName(gunId);
        Fonts.drawTracked(graphics, font, name.toUpperCase(Locale.ROOT), panelX, y + 13,
                Palette.TEXT, 1, 1.35f);

        if (!stack.isEmpty()) {
            graphics.pose().pushPose();
            graphics.pose().translate(panelX + panelW - 86, y - 2, 0);
            graphics.pose().scale(4.0f, 4.0f, 1.0f);
            graphics.renderItem(stack, 0, 0);
            graphics.pose().popPose();
        }
        return y + 48;
    }

    /** Pile d'affichage uniquement : meme construction que l'equipement reel. */
    private static ItemStack displayStack(String gunIdStr) {
        ResourceLocation gunId = ResourceLocation.tryParse(gunIdStr);
        if (gunId == null) return ItemStack.EMPTY;
        int magSize = TimelessAPI.getCommonGunIndex(gunId)
                .map(index -> Math.max(1, index.getGunData().getAmmoAmount()))
                .orElse(30);
        return Loadout.makeGun(gunId, magSize, "");
    }

    private static String gunName(String gunIdStr) {
        for (GunCatalog.Entry entry : ClientClassData.catalog()) {
            if (entry.gunId().equals(gunIdStr)) {
                String translated = Component.translatable(entry.nameKey()).getString();
                return translated.equals(entry.nameKey()) ? entry.gunId() : translated;
            }
        }
        ResourceLocation id = ResourceLocation.tryParse(gunIdStr);
        return id == null ? gunIdStr : id.getPath();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
