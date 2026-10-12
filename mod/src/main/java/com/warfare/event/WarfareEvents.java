package com.warfare.event;

import com.tacz.guns.api.event.common.EntityKillByGunEvent;
import com.warfare.Warfare;
import com.warfare.game.GameManager;
import com.warfare.game.WarfareData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
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
        WarfareData data = WarfareData.get(server);
        Warfare.LOGGER.info("Warfare pret : lobby={} points d'apparition={}",
                data.getLobby() == null ? "non defini" : data.getLobby().describe(), data.getSpawns().size());
        if (!data.isReady()) {
            Warfare.LOGGER.warn("Arene incomplete : utiliser /warfare setlobby puis /warfare addspawn");
        }
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

    /** Invincibilite au lobby, pendant l'ecran de fin et a la reapparition. */
    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (GameManager.get().isInvulnerable(player.getUUID())) event.setCanceled(true);
    }

    private WarfareEvents() {}
}
