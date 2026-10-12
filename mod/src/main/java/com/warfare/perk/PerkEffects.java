package com.warfare.perk;

import com.tacz.guns.api.GunProperties;
import com.tacz.guns.api.event.common.AttachmentPropertyEvent;
import com.tacz.guns.api.event.common.EntityHurtByGunEvent;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.warfare.loadout.Perk;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.PlayLevelSoundEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;

/**
 * Effets des atouts.
 *
 * Deux atouts passent par les proprietes d'arme de TaCZ (com.tacz.guns.api.GunProperties,
 * modifiables dans AttachmentPropertyEvent). Comme cet evenement ne donne que l'ItemStack
 * et pas le porteur, les armes distribuees portent un marqueur NBT "WarfarePerks".
 */
public final class PerkEffects {
    /** Tag NBT pose sur les armes distribuees. */
    public static final String PERK_TAG = "WarfarePerks";

    private static final float ADS_FACTOR = 0.70f;        // Visee eclair : -30% de temps de visee
    private static final float RPM_FACTOR = 1.15f;        // Cadence soutenue : +15%
    private static final float DAMAGE_FACTOR = 1.20f;     // Puissance d'arret : +20%
    private static final float EXPLOSION_FACTOR = 0.70f;  // Blinde : -30%

    // ------------------------------------------------- proprietes d'arme (TaCZ)

    @SubscribeEvent
    public static void onGunProperties(AttachmentPropertyEvent event) {
        ItemStack gun = event.getGunItem();
        CompoundTag tag = gun.getTag();
        if (tag == null || !tag.contains(PERK_TAG)) return;
        String marker = tag.getString(PERK_TAG);
        AttachmentCacheProperty cache = event.getCacheProperty();

        if (hasMarker(marker, Perk.QUICK_AIM)) {
            Float adsTime = cache.getCache(GunProperties.ADS_TIME);
            if (adsTime != null) cache.setCache(GunProperties.ADS_TIME, adsTime * ADS_FACTOR);
        }
        if (hasMarker(marker, Perk.RAPID_FIRE)) {
            Integer rpm = cache.getCache(GunProperties.ROUNDS_PER_MINUTE);
            if (rpm != null) cache.setCache(GunProperties.ROUNDS_PER_MINUTE, Math.round(rpm * RPM_FACTOR));
        }
    }

    public static String marker(com.warfare.loadout.ClassLoadout loadout) {
        StringBuilder builder = new StringBuilder();
        for (int category = 1; category <= Perk.CATEGORIES; category++) {
            if (category > 1) builder.append(',');
            builder.append(loadout.perk(category).id());
        }
        return builder.toString();
    }

    private static boolean hasMarker(String marker, Perk perk) {
        for (String part : marker.split(",")) {
            if (part.equals(perk.id())) return true;
        }
        return false;
    }

    // ------------------------------------------------------------ degats

    /** Puissance d'arret : evenement officiel TaCZ, modifie les degats avant application. */
    @SubscribeEvent
    public static void onHurtByGun(EntityHurtByGunEvent.Pre event) {
        if (event.getLogicalSide() != LogicalSide.SERVER) return;
        if (!(event.getAttacker() instanceof ServerPlayer attacker)) return;
        if (!ActivePerks.has(attacker.getUUID(), Perk.STOPPING_POWER)) return;
        event.setBaseAmount(event.getBaseAmount() * DAMAGE_FACTOR);
    }

    /** Blinde : degats d'explosion, quelle qu'en soit l'origine. */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;
        if (!ActivePerks.has(victim.getUUID(), Perk.FLAK)) return;
        if (!event.getSource().is(DamageTypeTags.IS_EXPLOSION)) return;
        event.setAmount(event.getAmount() * EXPLOSION_FACTOR);
    }

    // ------------------------------------------------------------ discretion

    /**
     * Pas de loup : les bruits de pas d'un joueur sont diffuses par le serveur
     * (Entity.playSound a la position exacte du joueur). On annule la diffusion
     * pour les porteurs de l'atout ; le joueur continue d'entendre ses propres pas,
     * joues localement par son client.
     */
    @SubscribeEvent
    public static void onLevelSound(PlayLevelSoundEvent.AtPosition event) {
        if (event.getLevel() == null || event.getLevel().isClientSide()) return;
        if (event.getSource() != SoundSource.PLAYERS) return;
        if (event.getSound() == null) return;

        boolean isStep = event.getSound().unwrapKey()
                .map(key -> key.location().getPath().endsWith(".step"))
                .orElse(false);
        if (!isStep) return;

        Vec3 at = event.getPosition();
        for (var player : event.getLevel().players()) {
            if (!(player instanceof ServerPlayer serverPlayer)) continue;
            if (!ActivePerks.has(serverPlayer.getUUID(), Perk.GHOST)) continue;
            if (serverPlayer.position().distanceToSqr(at) < 0.0001) {
                event.setCanceled(true);
                return;
            }
        }
    }

    private PerkEffects() {}
}
