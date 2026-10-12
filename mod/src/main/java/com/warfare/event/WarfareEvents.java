package com.warfare.event;

import com.tacz.guns.api.event.common.EntityHurtByGunEvent;
import com.tacz.guns.api.event.common.EntityKillByGunEvent;
import com.warfare.Warfare;
import com.warfare.game.GameManager;
import com.warfare.game.WarfareData;
import com.warfare.loadout.ClassStore;
import com.warfare.streak.Killstreak;
import com.warfare.streak.StreakItems;
import com.warfare.streak.StreakManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;

import java.util.UUID;

/** Branchement du mode de jeu sur les evenements Forge et TaCZ. */
public final class WarfareEvents {

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        GameManager.get().tick(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        // Filet de securite : si l'evenement de config n'a pas ete recu, on relit ici.
        com.warfare.WarfareConfig.reload();
        com.warfare.game.Teams.ensure(server);
        GameManager.get().ensureDefaultArena(server);
        WarfareData data = WarfareData.get(server);
        Warfare.LOGGER.info("Warfare pret : mode={} carte={} ({} carte(s) enregistree(s))",
                data.mode().id(), data.currentName().isEmpty() ? "aucune" : data.currentName(),
                data.arenas().size());
        if (!data.isReady()) {
            Warfare.LOGGER.warn("Aucune carte jouable : /warfare arena create <nom>, "
                    + "puis /warfare setlobby et /warfare addspawn");
        }
        GameManager.get().cleanMobs(server);
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        com.warfare.progression.StatsStore.saveAll(event.getServer());
        com.warfare.progression.StatsStore.clearCache();
        ClassStore.clearCache();
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) GameManager.get().onJoin(player);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) GameManager.get().onLeave(player);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) GameManager.get().onRespawn(player);
    }

    /**
     * Enregistre la mort avant que TaCZ ne poste EntityKillByGunEvent (TaCZ le poste
     * apres hurt(), quand la victime est deja isDeadOrDying()).
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;
        UUID killer = event.getSource().getEntity() instanceof ServerPlayer p ? p.getUUID() : null;
        GameManager.get().onDeath(victim, killer);
    }

    /** Evenement officiel de TaCZ : donne l'arme utilisee et le tir a la tete. */
    @SubscribeEvent
    public static void onGunKill(EntityKillByGunEvent event) {
        if (event.getLogicalSide() != LogicalSide.SERVER) return;
        if (!(event.getKilledEntity() instanceof ServerPlayer victim)) return;
        UUID killer = event.getAttacker() instanceof ServerPlayer p ? p.getUUID() : null;
        GameManager.get().onGunKill(victim.getUUID(), killer, event.getGunId(), event.isHeadShot());
    }

    // ------------------------------------------------- objets de killstreak

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!StreakItems.isStreakItem(event.getItemStack())) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        useStreak(event.getEntity(), event.getItemStack());
    }

    /**
     * Plusieurs recompenses sont des items de bloc (coffre, distributeur, TNT) :
     * sans ce gestionnaire, le clic droit sur un bloc les poserait.
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!StreakItems.isStreakItem(event.getItemStack())) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        useStreak(event.getEntity(), event.getItemStack());
    }

    private static void useStreak(net.minecraft.world.entity.player.Player rawPlayer, ItemStack stack) {
        if (!(rawPlayer instanceof ServerPlayer player)) return;   // cote client : on annule seulement
        Killstreak streak = StreakItems.of(stack);
        if (streak == null) return;

        if (!GameManager.get().isRunning() || player.isSpectator()) {
            player.displayClientMessage(
                    Component.literal("\u00a7cUtilisable uniquement en partie."), true);
            return;
        }
        if (StreakManager.get().activate(player, streak)) {
            stack.shrink(1);
            player.inventoryMenu.broadcastChanges();
        }
    }

    /** Invincibilite au lobby, pendant l'ecran de fin, a la reapparition, et tir allie. */
    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (GameManager.get().isInvulnerable(player.getUUID())) {
            event.setCanceled(true);
            return;
        }
        if (event.getSource().getEntity() instanceof ServerPlayer attacker
                && GameManager.get().isFriendly(attacker.getUUID(), player.getUUID())) {
            event.setCanceled(true);
        }
    }

    /**
     * Degats des balles : multiplicateur du mode (elimination en un coup) et tir allie.
     * PerkEffects applique en plus les atouts sur ce meme evenement.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHurtByGun(EntityHurtByGunEvent.Pre event) {
        if (event.getLogicalSide() != LogicalSide.SERVER) return;
        if (!(event.getAttacker() instanceof ServerPlayer attacker)) return;
        if (event.getHurtEntity() instanceof ServerPlayer victim
                && GameManager.get().isFriendly(attacker.getUUID(), victim.getUUID())) {
            event.setBaseAmount(0);
            event.setHeadshot(false);
            return;
        }
        double multiplier = GameManager.get().damageMultiplier();
        if (multiplier != 1.0) event.setBaseAmount((float) (event.getBaseAmount() * multiplier));
    }

    private WarfareEvents() {}
}
