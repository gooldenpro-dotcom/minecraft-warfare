package com.warfare.game;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.warfare.Warfare;
import com.warfare.WarfareConfig;
import com.warfare.loadout.ClassLoadout;
import com.warfare.perk.ActivePerks;
import com.warfare.perk.PerkEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Optional;

/**
 * Construit et distribue l'equipement d'une classe (armes TaCZ + munitions)
 * et active les atouts correspondants.
 *
 * Identifiants verifies dans tacz-1.20.1-1.1.8-hotfix2.jar :
 *   - item arme     : tacz:modern_kinetic_gun, NBT GunId
 *   - item munition : tacz:ammo, NBT AmmoId
 * Le calibre et la taille du chargeur sont lus dans l'index TaCZ de l'arme.
 */
public final class Loadout {
    private static final ResourceLocation GUN_ITEM = new ResourceLocation("tacz", "modern_kinetic_gun");
    private static final ResourceLocation AMMO_ITEM = new ResourceLocation("tacz", "ammo");

    /** Vide l'inventaire, donne la classe et active ses atouts. */
    public static void give(ServerPlayer player, ClassLoadout loadout) {
        player.getInventory().clearContent();
        String perkMarker = PerkEffects.marker(loadout);

        int slot = giveWeapon(player, 0, loadout.primaryGun(), WarfareConfig.primaryMagazines, perkMarker);
        giveWeapon(player, slot, loadout.secondaryGun(), WarfareConfig.secondaryMagazines, perkMarker);

        for (String entry : WarfareConfig.extraItems) {
            ItemStack stack = parseExtra(entry);
            if (!stack.isEmpty()) player.getInventory().add(stack);
        }
        clearStreakSlot(player);
        player.getInventory().selected = 0;
        player.inventoryMenu.broadcastChanges();
        ActivePerks.apply(player, loadout);
    }

    /**
     * L'emplacement {@link com.warfare.streak.StreakItems#SLOT} est reserve aux recompenses
     * de killstreak : si des munitions y ont atterri, on les deplace dans l'inventaire.
     */
    private static void clearStreakSlot(ServerPlayer player) {
        var inventory = player.getInventory();
        ItemStack occupying = inventory.getItem(com.warfare.streak.StreakItems.SLOT);
        if (occupying.isEmpty()) return;
        inventory.setItem(com.warfare.streak.StreakItems.SLOT, ItemStack.EMPTY);
        for (int slot = 9; slot < 36; slot++) {
            if (inventory.getItem(slot).isEmpty()) {
                inventory.setItem(slot, occupying);
                return;
            }
        }
    }

    /** @return le prochain slot libre de la barre d'action. */
    private static int giveWeapon(ServerPlayer player, int slot, String gunIdStr, int magazines, String perkMarker) {
        ResourceLocation gunId = ResourceLocation.tryParse(gunIdStr);
        if (gunId == null) {
            Warfare.LOGGER.warn("Identifiant d'arme invalide : {}", gunIdStr);
            return slot;
        }
        Optional<CommonGunIndex> index = TimelessAPI.getCommonGunIndex(gunId);
        if (index.isEmpty()) {
            Warfare.LOGGER.warn("Arme TaCZ inconnue : {} (verifier les packs charges)", gunId);
            player.sendSystemMessage(com.warfare.util.Txt.info(
                    Component.literal("Arme TaCZ inconnue : " + gunId)));
            return slot;
        }

        int magSize = Math.max(1, index.get().getGunData().getAmmoAmount());
        ResourceLocation ammoId = index.get().getGunData().getAmmoId();

        ItemStack gun = makeGun(gunId, magSize, perkMarker);
        if (!gun.isEmpty()) {
            player.getInventory().setItem(slot, gun);
            slot++;
        }
        giveAmmo(player, ammoId, magSize * magazines);
        return slot;
    }

    /** Une arme deja chargee, chambre comprise, marquee avec les atouts de la classe. */
    public static ItemStack makeGun(ResourceLocation gunId, int magSize, String perkMarker) {
        Item item = ForgeRegistries.ITEMS.getValue(GUN_ITEM);
        if (item == null) {
            Warfare.LOGGER.error("Item {} introuvable : TaCZ n'est pas charge ?", GUN_ITEM);
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(item);
        IGun iGun = IGun.getIGunOrNull(stack);
        if (iGun == null) return ItemStack.EMPTY;
        iGun.setGunId(stack, gunId);
        iGun.setCurrentAmmoCount(stack, magSize);
        iGun.setBulletInBarrel(stack, true);
        if (perkMarker != null && !perkMarker.isEmpty()) {
            stack.getOrCreateTag().putString(PerkEffects.PERK_TAG, perkMarker);
        }
        return stack;
    }

    private static void giveAmmo(ServerPlayer player, ResourceLocation ammoId, int count) {
        if (ammoId == null || count <= 0) return;
        Item item = ForgeRegistries.ITEMS.getValue(AMMO_ITEM);
        if (item == null) return;
        int left = count;
        while (left > 0) {
            ItemStack stack = new ItemStack(item);
            IAmmo iAmmo = IAmmo.getIAmmoOrNull(stack);
            if (iAmmo == null) return;
            iAmmo.setAmmoId(stack, ammoId);
            int size = Math.min(left, stack.getMaxStackSize());
            stack.setCount(size);
            left -= size;
            if (!player.getInventory().add(stack)) break;
        }
    }

    /** "minecraft:golden_apple 2" -> ItemStack. */
    private static ItemStack parseExtra(String entry) {
        String[] parts = entry.trim().split("\\s+");
        ResourceLocation id = ResourceLocation.tryParse(parts[0]);
        if (id == null) return ItemStack.EMPTY;
        Item item = ForgeRegistries.ITEMS.getValue(id);
        if (item == null) {
            Warfare.LOGGER.warn("Item inconnu dans extraItems : {}", parts[0]);
            return ItemStack.EMPTY;
        }
        int count = 1;
        if (parts.length > 1) {
            try {
                count = Math.max(1, Integer.parseInt(parts[1]));
            } catch (NumberFormatException ignored) {
            }
        }
        return new ItemStack(item, count);
    }

    /** Nom localise de l'arme (cle de traduction TaCZ), pour le killfeed et le menu. */
    public static Component gunName(ResourceLocation gunId) {
        return TimelessAPI.getCommonGunIndex(gunId)
                .map(i -> (Component) Component.translatable(i.getPojo().getName()))
                .orElseGet(() -> Component.literal(gunId.getPath()));
    }

    private Loadout() {}
}
