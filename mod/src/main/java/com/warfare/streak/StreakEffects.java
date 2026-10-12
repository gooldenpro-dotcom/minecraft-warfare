package com.warfare.streak;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IGun;
import com.warfare.WarfareConfig;
import com.warfare.game.WarfareData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;

/** Les effets concrets des killstreaks. Aucune explosion ne casse de blocs. */
public final class StreakEffects {

    /** Index 0 des donnees synchronisees d'Entity : les drapeaux partages (bit 0x40 = surbrillance). */
    private static final EntityDataAccessor<Byte> SHARED_FLAGS =
            new EntityDataAccessor<>(0, EntityDataSerializers.BYTE);
    private static final int GLOWING_FLAG = 0x40;

    // ------------------------------------------------------------------ drone

    /**
     * Surbrillance visible par un seul joueur : on renvoie a ce client seul les drapeaux
     * de l'entite avec le bit de surbrillance force. Le serveur, lui, n'applique aucun effet,
     * donc les autres joueurs ne voient rien.
     */
    public static void sendGlow(ServerPlayer viewer, Entity target, boolean glowing) {
        byte flags = target.getEntityData().get(SHARED_FLAGS);
        byte patched = (byte) (glowing ? (flags | GLOWING_FLAG) : (flags & ~GLOWING_FLAG));
        viewer.connection.send(new ClientboundSetEntityDataPacket(target.getId(),
                List.of(SynchedEntityData.DataValue.create(SHARED_FLAGS, patched))));
    }

    // ------------------------------------------------------------ ravitaillement

    private static final MobEffect[] BONUSES = {
            MobEffects.ABSORPTION, MobEffects.REGENERATION, MobEffects.MOVEMENT_SPEED,
            MobEffects.DAMAGE_BOOST, MobEffects.DAMAGE_RESISTANCE
    };
    private static final String[] BONUS_NAMES = {
            "Absorption", "Regeneration", "Vitesse", "Force", "Resistance"
    };

    /** Recharge toutes les armes de l'inventaire et tire un bonus au hasard. @return le nom du bonus. */
    public static String resupply(ServerPlayer player) {
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            IGun iGun = IGun.getIGunOrNull(stack);
            if (iGun == null) continue;
            ResourceLocation gunId = iGun.getGunId(stack);
            var index = TimelessAPI.getCommonGunIndex(gunId);
            if (index.isEmpty()) continue;
            int magSize = Math.max(1, index.get().getGunData().getAmmoAmount());
            iGun.setCurrentAmmoCount(stack, magSize);
            iGun.setBulletInBarrel(stack, true);
            giveAmmo(player, index.get().getGunData().getAmmoId(), magSize * 3);
        }
        player.inventoryMenu.broadcastChanges();

        int pick = player.getRandom().nextInt(BONUSES.length);
        int duration = BONUSES[pick] == MobEffects.REGENERATION ? 200 : 600;
        player.addEffect(new MobEffectInstance(BONUSES[pick], duration, 1));
        player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f, 1.6f);
        return BONUS_NAMES[pick];
    }

    private static void giveAmmo(ServerPlayer player, @Nullable ResourceLocation ammoId, int count) {
        if (ammoId == null || count <= 0) return;
        var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("tacz", "ammo"));
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

    // ------------------------------------------------------------- explosions

    /**
     * Explosion qui n'abime jamais le terrain : ExplosionInteraction.NONE.
     * L'auteur est passe comme source, donc les eliminations lui sont creditees.
     */
    public static void explode(ServerLevel level, @Nullable Entity owner, Vec3 at, double power) {
        level.explode(owner, at.x, at.y, at.z, (float) power,
                net.minecraft.world.level.Level.ExplosionInteraction.NONE);
    }

    /** Un point d'impact au hasard dans un disque, ramene a la surface. */
    public static Vec3 scatter(ServerLevel level, Vec3 center, double radius) {
        double angle = level.random.nextDouble() * Math.PI * 2;
        double distance = Math.sqrt(level.random.nextDouble()) * radius;
        double x = center.x + Math.cos(angle) * distance;
        double z = center.z + Math.sin(angle) * distance;
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, (int) x, (int) z);
        return new Vec3(x, Math.max(y, center.y - 8), z);
    }

    // ---------------------------------------------------------------- tourelle

    public static ArmorStand spawnTurret(ServerLevel level, Vec3 at, String ownerName) {
        ArmorStand turret = new ArmorStand(level, at.x, at.y, at.z);
        turret.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DISPENSER));
        turret.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        turret.setNoGravity(true);
        turret.setInvulnerable(true);
        turret.setSilent(true);
        turret.setCustomName(net.minecraft.network.chat.Component.literal("Tourelle de " + ownerName));
        turret.setCustomNameVisible(true);
        level.addFreshEntity(turret);
        return turret;
    }

    /** Tir de la tourelle : degats directs credites a l'auteur, plus un trace visible. */
    public static void turretShot(ServerLevel level, ArmorStand turret, ServerPlayer target,
                                  @Nullable ServerPlayer owner, double damage) {
        Vec3 from = turret.position().add(0, 1.4, 0);
        Vec3 to = target.position().add(0, 1.0, 0);
        Vec3 step = to.subtract(from).scale(1.0 / 12.0);
        for (int i = 1; i < 12; i++) {
            Vec3 point = from.add(step.scale(i));
            level.sendParticles(ParticleTypes.CRIT, point.x, point.y, point.z, 1, 0, 0, 0, 0);
        }
        level.playSound(null, turret.getX(), turret.getY(), turret.getZ(),
                SoundEvents.ARROW_SHOOT, SoundSource.HOSTILE, 1.0f, 1.4f);

        turret.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, to);
        var source = owner == null ? level.damageSources().magic()
                : level.damageSources().playerAttack(owner);
        target.hurt(source, (float) damage);
    }

    // ------------------------------------------------------------ zone d'arene

    /**
     * Zone bombardee : la boite englobante des points d'apparition, elargie par la marge
     * de la config. Sans point d'apparition, un simple disque autour de l'auteur.
     */
    public static AABB arena(MinecraftServer server, Vec3 fallback) {
        List<WarfareData.Spot> spawns = WarfareData.get(server).getSpawns();
        int margin = WarfareConfig.carpetMargin;
        if (spawns.isEmpty()) {
            return new AABB(fallback.x - margin, fallback.y - 8, fallback.z - margin,
                    fallback.x + margin, fallback.y + 8, fallback.z + margin);
        }
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        for (WarfareData.Spot spot : spawns) {
            minX = Math.min(minX, spot.x());
            minY = Math.min(minY, spot.y());
            minZ = Math.min(minZ, spot.z());
            maxX = Math.max(maxX, spot.x());
            maxY = Math.max(maxY, spot.y());
            maxZ = Math.max(maxZ, spot.z());
        }
        return new AABB(minX - margin, minY - 8, minZ - margin, maxX + margin, maxY + 8, maxZ + margin);
    }

    private StreakEffects() {}
}
