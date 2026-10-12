package com.warfare.streak;

import com.warfare.WarfareConfig;
import com.warfare.loadout.ClassStore;
import com.warfare.util.Txt;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Series d'eliminations : compteur, attribution des recompenses et suivi des effets en cours.
 */
public final class StreakManager {
    private static final StreakManager INSTANCE = new StreakManager();

    public static StreakManager get() {
        return INSTANCE;
    }

    private static final double DESIGNATION_RANGE = 160.0;
    private static final double TURRET_PLACE_RANGE = 6.0;

    private long tick;

    /** Vrai le temps qu'une killstreak applique ses degats, pour ne pas crediter la serie. */
    private boolean inStreakDamage;

    private final Map<UUID, Integer> streak = new HashMap<>();
    private final Map<UUID, Long> droneUntil = new HashMap<>();
    private final Map<UUID, Deque<Killstreak>> queued = new HashMap<>();
    private final List<PendingStrike> strikes = new ArrayList<>();
    private final List<Turret> turrets = new ArrayList<>();
    private final List<Carpet> carpets = new ArrayList<>();

    private record PendingStrike(UUID owner, ResourceKey<Level> dimension, Vec3 center, long impactTick) {}

    private static final class Turret {
        final UUID owner;
        final ArmorStand entity;
        final long endTick;
        long nextShot;

        Turret(UUID owner, ArmorStand entity, long endTick) {
            this.owner = owner;
            this.entity = entity;
            this.endTick = endTick;
        }
    }

    private static final class Carpet {
        final UUID owner;
        final ResourceKey<Level> dimension;
        final AABB area;
        final long endTick;
        int remaining;
        long nextDrop;

        Carpet(UUID owner, ResourceKey<Level> dimension, AABB area, long endTick, int remaining) {
            this.owner = owner;
            this.dimension = dimension;
            this.area = area;
            this.endTick = endTick;
            this.remaining = remaining;
        }
    }

    public boolean isStreakDamage() {
        return inStreakDamage;
    }

    public int streakOf(UUID player) {
        return streak.getOrDefault(player, 0);
    }

    // ------------------------------------------------------------- compteur

    /** Une elimination a l'arme fait monter la serie ; une elimination par killstreak non. */
    public void onKill(ServerPlayer killer, boolean fromStreak) {
        if (!WarfareConfig.streaksEnabled) return;
        if (fromStreak && !WarfareConfig.streakKillsCount) return;

        int value = streak.merge(killer.getUUID(), 1, Integer::sum);
        List<Killstreak> equipped = ClassStore.get(killer.server, killer.getUUID()).current().streaks();
        for (Killstreak candidate : equipped) {
            if (candidate.threshold() == value) grant(killer, candidate);
        }
    }

    public void onDeath(UUID player) {
        streak.remove(player);
        queued.remove(player);
        stopDrone(player);
    }

    /** Fin de partie, /warfare stop : on nettoie tout, entites comprises. */
    public void reset(MinecraftServer server) {
        for (Turret turret : turrets) turret.entity.discard();
        turrets.clear();
        strikes.clear();
        carpets.clear();
        queued.clear();
        for (UUID player : List.copyOf(droneUntil.keySet())) stopDrone(player);
        streak.clear();
    }

    // ----------------------------------------------------------- recompenses

    private void grant(ServerPlayer player, Killstreak reward) {
        queued.computeIfAbsent(player.getUUID(), k -> new ArrayDeque<>()).add(reward);
        Txt.title(player, Txt.c(reward.title(), ChatFormatting.GOLD, ChatFormatting.BOLD),
                Txt.c("disponible", ChatFormatting.GRAY), 0, 40, 10);
        player.sendSystemMessage(Txt.info(Component.empty()
                .append(Txt.c(reward.title(), ChatFormatting.GOLD))
                .append(Txt.c(" disponible - clic droit pour l'utiliser", ChatFormatting.GRAY))));
        player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.2f);
    }

    /** Depose la recompense en attente dans l'emplacement dedie des qu'il est libre. */
    private void deliverQueued(ServerPlayer player) {
        Deque<Killstreak> pending = queued.get(player.getUUID());
        if (pending == null || pending.isEmpty()) return;
        ItemStack current = player.getInventory().getItem(StreakItems.SLOT);
        if (!current.isEmpty() && !StreakItems.isStreakItem(current)) return;
        if (StreakItems.isStreakItem(current)) return;

        player.getInventory().setItem(StreakItems.SLOT, StreakItems.create(pending.poll()));
        player.inventoryMenu.broadcastChanges();
    }

    // ------------------------------------------------------------ activation

    /** Clic droit sur l'objet-recompense. @return true si la killstreak a bien demarre. */
    public boolean activate(ServerPlayer player, Killstreak streakType) {
        MinecraftServer server = player.server;
        ServerLevel level = (ServerLevel) player.level();

        switch (streakType) {
            case DRONE -> {
                droneUntil.put(player.getUUID(), tick + WarfareConfig.droneSeconds * 20L);
                refreshDrone(level, player);
                player.playNotifySound(SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 0.5f, 2.0f);
            }
            case RESUPPLY -> {
                String bonus = StreakEffects.resupply(player);
                player.sendSystemMessage(Txt.info(Txt.c("Ravitaillement : munitions pleines + "
                        + bonus, ChatFormatting.GREEN)));
            }
            case AIRSTRIKE -> {
                Vec3 target = designate(player, DESIGNATION_RANGE);
                strikes.add(new PendingStrike(player.getUUID(), level.dimension(), target,
                        tick + WarfareConfig.airstrikeDelay * 20L));
                level.playSound(null, target.x, target.y, target.z,
                        SoundEvents.ELYTRA_FLYING, SoundSource.HOSTILE, 4.0f, 0.6f);
                player.sendSystemMessage(Txt.info(Txt.c(String.format(
                        "Frappe designee en %.0f %.0f %.0f, impact dans %d s",
                        target.x, target.y, target.z, WarfareConfig.airstrikeDelay),
                        ChatFormatting.GOLD)));
            }
            case TURRET -> {
                Vec3 at = designate(player, TURRET_PLACE_RANGE);
                ArmorStand entity = StreakEffects.spawnTurret(level, at, player.getGameProfile().getName());
                turrets.add(new Turret(player.getUUID(), entity, tick + WarfareConfig.turretSeconds * 20L));
            }
            case CARPET -> {
                AABB area = StreakEffects.arena(server, player.position());
                carpets.add(new Carpet(player.getUUID(), level.dimension(), area,
                        tick + WarfareConfig.carpetSeconds * 20L, WarfareConfig.carpetBombs));
                for (ServerPlayer everyone : server.getPlayerList().getPlayers()) {
                    Txt.title(everyone, Txt.c("BOMBARDEMENT", ChatFormatting.DARK_RED, ChatFormatting.BOLD),
                            Txt.c("Mettez-vous a couvert", ChatFormatting.GRAY), 0, 40, 10);
                    everyone.playNotifySound(SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 1.0f, 0.6f);
                }
            }
        }

        announceUse(server, player, streakType);
        return true;
    }

    private void announceUse(MinecraftServer server, ServerPlayer player, Killstreak streakType) {
        server.getPlayerList().broadcastSystemMessage(Txt.info(Component.empty()
                .append(Txt.c(player.getGameProfile().getName(), ChatFormatting.RED))
                .append(Txt.c(" a declenche ", ChatFormatting.GRAY))
                .append(Txt.c(streakType.title(), ChatFormatting.GOLD))), false);
    }

    /** Point vise par le joueur, ou le bout de son regard s'il ne vise aucun bloc. */
    private static Vec3 designate(ServerPlayer player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1.0f).scale(range));
        BlockHitResult hit = player.level().clip(new ClipContext(eye, end,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
    }

    // ------------------------------------------------------------------ tick

    public void tick(MinecraftServer server) {
        tick++;
        if (!WarfareConfig.streaksEnabled) return;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) deliverQueued(player);

        tickDrones(server);
        tickStrikes(server);
        tickTurrets(server);
        tickCarpets(server);
    }

    private void tickDrones(MinecraftServer server) {
        if (droneUntil.isEmpty()) return;
        for (UUID id : List.copyOf(droneUntil.keySet())) {
            ServerPlayer owner = server.getPlayerList().getPlayer(id);
            if (owner == null || tick >= droneUntil.get(id)) {
                stopDrone(id);
                if (owner != null) {
                    owner.sendSystemMessage(Txt.info(Txt.c("Drone hors service.", ChatFormatting.GRAY)));
                }
                continue;
            }
            // Le client peut recevoir une mise a jour legitime des drapeaux : on reaffirme.
            if (tick % 10 == 0) refreshDrone((ServerLevel) owner.level(), owner);
        }
    }

    private void refreshDrone(ServerLevel level, ServerPlayer owner) {
        for (ServerPlayer target : level.players()) {
            if (target.getUUID().equals(owner.getUUID())) continue;
            StreakEffects.sendGlow(owner, target, true);
        }
    }

    private void stopDrone(UUID id) {
        if (droneUntil.remove(id) == null) return;
        MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        ServerPlayer owner = server.getPlayerList().getPlayer(id);
        if (owner == null) return;
        for (ServerPlayer target : ((ServerLevel) owner.level()).players()) {
            if (target.getUUID().equals(id)) continue;
            StreakEffects.sendGlow(owner, target, false);
        }
    }

    private void tickStrikes(MinecraftServer server) {
        if (strikes.isEmpty()) return;
        for (PendingStrike strike : List.copyOf(strikes)) {
            if (tick < strike.impactTick()) continue;
            strikes.remove(strike);
            ServerLevel level = server.getLevel(strike.dimension());
            if (level == null) continue;
            ServerPlayer owner = server.getPlayerList().getPlayer(strike.owner());

            inStreakDamage = true;
            try {
                for (int i = 0; i < WarfareConfig.airstrikeBombs; i++) {
                    Vec3 at = StreakEffects.scatter(level, strike.center(), WarfareConfig.airstrikeSpread);
                    StreakEffects.explode(level, owner, at, WarfareConfig.airstrikePower);
                }
            } finally {
                inStreakDamage = false;
            }
        }
    }

    private void tickTurrets(MinecraftServer server) {
        if (turrets.isEmpty()) return;
        for (Turret turret : List.copyOf(turrets)) {
            if (tick >= turret.endTick || !turret.entity.isAlive()) {
                turret.entity.discard();
                turrets.remove(turret);
                continue;
            }
            if (tick < turret.nextShot) continue;
            turret.nextShot = tick + WarfareConfig.turretInterval;

            ServerLevel level = (ServerLevel) turret.entity.level();
            ServerPlayer owner = server.getPlayerList().getPlayer(turret.owner);
            ServerPlayer target = nearestEnemy(level, turret, owner);
            if (target == null) continue;

            inStreakDamage = true;
            try {
                StreakEffects.turretShot(level, turret.entity, target, owner, WarfareConfig.turretDamage);
            } finally {
                inStreakDamage = false;
            }
        }
    }

    @Nullable
    private static ServerPlayer nearestEnemy(ServerLevel level, Turret turret, @Nullable ServerPlayer owner) {
        double range = WarfareConfig.turretRange;
        double best = range * range;
        ServerPlayer found = null;
        for (ServerPlayer candidate : level.players()) {
            if (owner != null && candidate.getUUID().equals(owner.getUUID())) continue;
            if (candidate.isSpectator() || candidate.isInvulnerable()) continue;
            double distance = candidate.distanceToSqr(turret.entity);
            if (distance > best) continue;
            if (!turret.entity.hasLineOfSight(candidate)) continue;
            best = distance;
            found = candidate;
        }
        return found;
    }

    private void tickCarpets(MinecraftServer server) {
        if (carpets.isEmpty()) return;
        for (Carpet carpet : List.copyOf(carpets)) {
            if (tick >= carpet.endTick || carpet.remaining <= 0) {
                carpets.remove(carpet);
                continue;
            }
            if (tick < carpet.nextDrop) continue;

            long ticksLeft = Math.max(1, carpet.endTick - tick);
            int perDrop = Math.max(1, (int) Math.ceil(carpet.remaining / (ticksLeft / 5.0)));
            carpet.nextDrop = tick + 5;

            ServerLevel level = server.getLevel(carpet.dimension);
            if (level == null) {
                carpets.remove(carpet);
                continue;
            }
            ServerPlayer owner = server.getPlayerList().getPlayer(carpet.owner);
            Vec3 center = carpet.area.getCenter();
            double radius = Math.max(carpet.area.getXsize(), carpet.area.getZsize()) / 2.0;

            inStreakDamage = true;
            try {
                for (int i = 0; i < perDrop && carpet.remaining > 0; i++) {
                    carpet.remaining--;
                    Vec3 at = StreakEffects.scatter(level, center, radius);
                    StreakEffects.explode(level, owner, at, WarfareConfig.carpetPower);
                }
            } finally {
                inStreakDamage = false;
            }
        }
    }

    private StreakManager() {}
}
