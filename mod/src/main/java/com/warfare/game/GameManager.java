package com.warfare.game;

import com.warfare.WarfareConfig;
import com.warfare.loadout.ClassLoadout;
import com.warfare.loadout.ClassStore;
import com.warfare.net.WarfareNetwork;
import com.warfare.perk.ActivePerks;
import com.warfare.streak.StreakManager;
import com.warfare.util.Txt;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;

import javax.annotation.Nullable;
import java.util.*;

/** Machine a etats du mode Free For All. Tout se passe cote serveur. */
public final class GameManager {
    private static final GameManager INSTANCE = new GameManager();

    public static GameManager get() {
        return INSTANCE;
    }

    /** Une mort en attente d'etre comptabilisee a la fin du tick. */
    private static final class Death {
        final UUID victim;
        @Nullable UUID killer;
        @Nullable ResourceLocation gunId;
        boolean headshot;
        final boolean fromStreak;

        Death(UUID victim, @Nullable UUID killer, boolean fromStreak) {
            this.victim = victim;
            this.killer = killer;
            this.fromStreak = fromStreak;
        }
    }

    private GameState state = GameState.LOBBY;
    private int stateTicks;
    private int matchTicksLeft;
    private long tick;
    private int lastCountdownShown = -1;
    private int lastClockShown = -1;

    private final Map<UUID, Integer> kills = new HashMap<>();
    private final Map<UUID, Integer> deaths = new HashMap<>();
    private final Map<UUID, Long> respawnAt = new HashMap<>();
    private final Map<UUID, Long> protectedUntil = new HashMap<>();
    private final Map<UUID, WarfareData.Spot> deathSpots = new HashMap<>();
    private final List<Death> pending = new ArrayList<>();
    private final Map<UUID, Boolean> menuSynced = new HashMap<>();

    public GameState state() {
        return state;
    }

    public boolean isRunning() {
        return state == GameState.RUNNING;
    }

    /** Le menu "Creer une classe" n'est utilisable qu'au lobby ou entre deux vies. */
    public boolean isMenuAllowed(UUID id) {
        return state != GameState.RUNNING || respawnAt.containsKey(id);
    }

    /** Invincible : lobby, ecran de fin, attente de reapparition ou protection au spawn. */
    public boolean isInvulnerable(UUID id) {
        if (state != GameState.RUNNING) return true;
        if (respawnAt.containsKey(id)) return true;
        Long until = protectedUntil.get(id);
        return until != null && tick < until;
    }

    // ------------------------------------------------------------------ morts

    /** Appele par LivingDeathEvent, avant que TaCZ ne poste son evenement de kill. */
    public void onDeath(ServerPlayer victim, @Nullable UUID killer) {
        deathSpots.put(victim.getUUID(), WarfareData.Spot.of(victim));
        pending.add(new Death(victim.getUUID(), killer, StreakManager.get().isStreakDamage()));
    }

    /** Appele par EntityKillByGunEvent : complete la mort deja enregistree. */
    public void onGunKill(UUID victim, @Nullable UUID killer, ResourceLocation gunId, boolean headshot) {
        for (int i = pending.size() - 1; i >= 0; i--) {
            Death d = pending.get(i);
            if (d.victim.equals(victim)) {
                d.gunId = gunId;
                d.headshot = headshot;
                if (killer != null) d.killer = killer;
                return;
            }
        }
    }

    private void flushDeaths(MinecraftServer server) {
        if (pending.isEmpty()) return;
        List<Death> batch = List.copyOf(pending);
        pending.clear();
        if (state != GameState.RUNNING) return;

        UUID winner = null;
        for (Death d : batch) {
            deaths.merge(d.victim, 1, Integer::sum);
            String victimName = nameOf(server, d.victim);

            StreakManager.get().onDeath(d.victim);

            if (d.killer != null && !d.killer.equals(d.victim)) {
                int score = kills.merge(d.killer, 1, Integer::sum);
                announceKill(server, nameOf(server, d.killer), victimName, d.gunId, d.headshot);
                ServerPlayer killer = server.getPlayerList().getPlayer(d.killer);
                if (killer != null) StreakManager.get().onKill(killer, d.fromStreak);
                if (score >= WarfareConfig.killLimit && winner == null) winner = d.killer;
            } else {
                broadcast(server, Component.empty()
                        .append(Txt.c(victimName, ChatFormatting.WHITE))
                        .append(Txt.c(" s'est elimine tout seul", ChatFormatting.GRAY)));
            }
        }
        if (winner != null) endMatch(server, winner, "limite d'eliminations atteinte");
    }

    private void announceKill(MinecraftServer server, String killer, String victim,
                              @Nullable ResourceLocation gunId, boolean headshot) {
        var msg = Component.empty()
                .append(Txt.c(killer, ChatFormatting.RED))
                .append(Txt.c(" a elimine ", ChatFormatting.GRAY))
                .append(Txt.c(victim, ChatFormatting.WHITE));
        if (gunId != null && WarfareConfig.showGunInKillfeed) {
            msg.append(Txt.c(" [", ChatFormatting.DARK_GRAY))
               .append(Loadout.gunName(gunId).copy().withStyle(ChatFormatting.GOLD))
               .append(Txt.c("]", ChatFormatting.DARK_GRAY));
        }
        if (headshot && WarfareConfig.announceHeadshots) {
            msg.append(Txt.c(" TETE", ChatFormatting.YELLOW, ChatFormatting.BOLD));
        }
        broadcast(server, msg);
    }

    // ------------------------------------------------------------ connexions

    public void onJoin(ServerPlayer player) {
        kills.putIfAbsent(player.getUUID(), 0);
        deaths.putIfAbsent(player.getUUID(), 0);
        ClassStore.get(player.server, player.getUUID());
        pushMenuState(player, true);
        if (state == GameState.RUNNING) {
            spawnIntoMatch(player);
        } else {
            toLobby(player);
        }
    }

    public void onLeave(ServerPlayer player) {
        UUID id = player.getUUID();
        ClassStore.save(player.server, id);
        ClassStore.unload(id);
        ActivePerks.forget(id);
        respawnAt.remove(id);
        protectedUntil.remove(id);
        deathSpots.remove(id);
        menuSynced.remove(id);
        Sidebar.forget(id);
    }

    /** Previent le client qu'il peut (ou non) ouvrir le menu de classes. */
    private void pushMenuState(ServerPlayer player, boolean force) {
        boolean allowed = isMenuAllowed(player.getUUID());
        Boolean last = menuSynced.get(player.getUUID());
        if (!force && last != null && last == allowed) return;
        menuSynced.put(player.getUUID(), allowed);
        WarfareNetwork.sync(player, allowed);
    }

    /** Reapparition vanilla immediate : on bascule en mode spectateur pendant le delai. */
    public void onRespawn(ServerPlayer player) {
        if (state != GameState.RUNNING) {
            toLobby(player);
            return;
        }
        UUID id = player.getUUID();
        player.setGameMode(GameType.SPECTATOR);
        WarfareData.Spot spot = deathSpots.get(id);
        if (spot != null) teleport(player, spot);

        int delay = Math.max(0, WarfareConfig.respawnDelaySeconds);
        respawnAt.put(id, tick + delay * 20L);
        Txt.title(player, Txt.c("ELIMINE", ChatFormatting.RED, ChatFormatting.BOLD),
                Txt.c("Reapparition dans " + delay + " s", ChatFormatting.GRAY), 0, delay * 20 + 10, 5);
    }

    // ------------------------------------------------------------------- tick

    public void tick(MinecraftServer server) {
        tick++;
        stateTicks++;
        flushDeaths(server);
        if (state == GameState.RUNNING) StreakManager.get().tick(server);

        switch (state) {
            case LOBBY -> tickLobby(server);
            case COUNTDOWN -> tickCountdown(server);
            case RUNNING -> tickRunning(server);
            case ENDED -> tickEnded(server);
        }

        if (tick % 20 == 0) {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                p.getFoodData().setFoodLevel(20);
                pushMenuState(p, false);
            }
            broadcastMatchState(server);
        }
    }

    private void tickLobby(MinecraftServer server) {
        if (tick % 20 != 0) return;
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        WarfareData data = WarfareData.get(server);

        if (!data.isReady()) {
            for (ServerPlayer p : players) {
                Txt.actionBar(p, Txt.c("Arene non configuree : /warfare setlobby et /warfare addspawn",
                        ChatFormatting.RED));
            }
            return;
        }
        if (players.size() >= WarfareConfig.minPlayers) {
            startCountdown(server);
            return;
        }
        for (ServerPlayer p : players) {
            Txt.actionBar(p, Txt.c("En attente de joueurs... (" + players.size() + "/"
                    + WarfareConfig.minPlayers + ")", ChatFormatting.YELLOW));
        }
    }

    private void tickCountdown(MinecraftServer server) {
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.size() < WarfareConfig.minPlayers) {
            broadcast(server, Txt.info(Txt.c("Pas assez de joueurs, compte a rebours annule.", ChatFormatting.GRAY)));
            for (ServerPlayer p : players) Txt.clearTitle(p);
            enter(GameState.LOBBY);
            return;
        }
        int total = WarfareConfig.countdownSeconds * 20;
        int left = (int) Math.ceil((total - stateTicks) / 20.0);
        if (left <= 0) {
            startMatch(server);
            return;
        }
        if (left != lastCountdownShown) {
            lastCountdownShown = left;
            ChatFormatting color = left <= 5 ? ChatFormatting.RED
                    : left <= 10 ? ChatFormatting.GOLD : ChatFormatting.YELLOW;
            for (ServerPlayer p : players) {
                Txt.title(p, Txt.c(String.valueOf(left), color, ChatFormatting.BOLD),
                        Txt.c("Debut de la partie", ChatFormatting.GRAY), 0, 25, 5);
            }
        }
    }

    private void tickRunning(MinecraftServer server) {
        matchTicksLeft--;

        // Reapparitions arrivees a echeance
        if (!respawnAt.isEmpty()) {
            for (UUID id : List.copyOf(respawnAt.keySet())) {
                if (tick < respawnAt.get(id)) continue;
                respawnAt.remove(id);
                ServerPlayer p = server.getPlayerList().getPlayer(id);
                if (p != null) spawnIntoMatch(p);
            }
        }

        // Fin de la protection de spawn
        if (!protectedUntil.isEmpty()) {
            for (UUID id : List.copyOf(protectedUntil.keySet())) {
                if (tick < protectedUntil.get(id)) continue;
                protectedUntil.remove(id);
                ServerPlayer p = server.getPlayerList().getPlayer(id);
                if (p != null && !respawnAt.containsKey(id)) p.setInvulnerable(false);
            }
        }

        if (matchTicksLeft <= 0) {
            endMatch(server, bestPlayer(), "temps ecoule");
            return;
        }
        if (tick % 20 == 0) refreshSidebars(server);
    }

    private void tickEnded(MinecraftServer server) {
        if (stateTicks >= WarfareConfig.endScreenSeconds * 20) {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                Txt.clearTitle(p);
                toLobby(p);
            }
            enter(GameState.LOBBY);
        }
    }

    // ---------------------------------------------------------- transitions

    private void enter(GameState next) {
        state = next;
        stateTicks = 0;
        lastCountdownShown = -1;
        lastClockShown = -1;
    }

    public boolean startCountdown(MinecraftServer server) {
        if (state != GameState.LOBBY) return false;
        if (!WarfareData.get(server).isReady()) return false;
        enter(GameState.COUNTDOWN);
        broadcast(server, Txt.info(Txt.c("La partie demarre dans "
                + WarfareConfig.countdownSeconds + " secondes.", ChatFormatting.YELLOW)));
        return true;
    }

    private void startMatch(MinecraftServer server) {
        enter(GameState.RUNNING);
        matchTicksLeft = WarfareConfig.matchSeconds * 20;
        kills.clear();
        deaths.clear();
        respawnAt.clear();
        protectedUntil.clear();
        pending.clear();
        StreakManager.get().reset(server);
        applyGameRules(server);

        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            kills.put(p.getUUID(), 0);
            deaths.put(p.getUUID(), 0);
            spawnIntoMatch(p);
            Txt.title(p, Txt.c("A L'ATTAQUE", ChatFormatting.GREEN, ChatFormatting.BOLD),
                    Txt.c("Chacun pour soi - " + WarfareConfig.killLimit + " eliminations", ChatFormatting.GRAY),
                    0, 35, 10);
        }
        broadcast(server, Txt.info(Txt.c("Partie lancee : premier a "
                + WarfareConfig.killLimit + " eliminations.", ChatFormatting.GREEN)));
        refreshSidebars(server);
    }

    public void endMatch(MinecraftServer server, @Nullable UUID winner, String reason) {
        if (state != GameState.RUNNING) return;
        enter(GameState.ENDED);
        respawnAt.clear();
        StreakManager.get().reset(server);

        String winnerName = winner == null ? null : nameOf(server, winner);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            Sidebar.hide(p);
            if (winnerName == null) {
                Txt.title(p, Txt.c("EGALITE", ChatFormatting.YELLOW, ChatFormatting.BOLD),
                        Txt.c(reason, ChatFormatting.GRAY), 5, WarfareConfig.endScreenSeconds * 20, 10);
            } else {
                boolean isWinner = p.getUUID().equals(winner);
                Txt.title(p,
                        Txt.c(isWinner ? "VICTOIRE" : winnerName + " gagne",
                                isWinner ? ChatFormatting.GREEN : ChatFormatting.GOLD, ChatFormatting.BOLD),
                        Txt.c(kills.getOrDefault(winner, 0) + " eliminations - " + reason, ChatFormatting.GRAY),
                        5, WarfareConfig.endScreenSeconds * 20, 10);
            }
        }
        broadcastRanking(server);
    }

    /** /warfare stop : coupe la partie sans vainqueur et revient directement au lobby. */
    public void abort(MinecraftServer server) {
        enter(GameState.LOBBY);
        respawnAt.clear();
        pending.clear();
        StreakManager.get().reset(server);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            Sidebar.hide(p);
            Txt.clearTitle(p);
            toLobby(p);
        }
    }

    // ------------------------------------------------------------ placement

    /** Lobby : mode aventure, invincible, inventaire vide. */
    public void toLobby(ServerPlayer player) {
        MinecraftServer server = player.server;
        reset(player);
        ActivePerks.clear(player);
        player.setGameMode(GameType.ADVENTURE);
        player.setInvulnerable(true);
        Sidebar.hide(player);
        WarfareData.Spot lobby = WarfareData.get(server).getLobby();
        if (lobby != null) {
            teleport(player, lobby);
            player.setRespawnPosition(lobby.dimension(), player.blockPosition(), lobby.yaw(), true, false);
        }
    }

    /** Entree en jeu : point d'apparition le plus eloigne des ennemis + equipement. */
    public void spawnIntoMatch(ServerPlayer player) {
        reset(player);
        player.setGameMode(GameType.ADVENTURE);
        WarfareData.Spot spot = chooseSpawn(player);
        if (spot != null) teleport(player, spot);
        ClassLoadout loadout = ClassStore.get(player.server, player.getUUID()).current();
        Loadout.give(player, loadout);
        player.sendSystemMessage(Txt.info(Txt.c("Classe : " + loadout.name(), ChatFormatting.GRAY)));
        player.setInvulnerable(WarfareConfig.spawnProtectionSeconds > 0);
        protectedUntil.put(player.getUUID(), tick + WarfareConfig.spawnProtectionSeconds * 20L);
        Txt.clearTitle(player);
        kills.putIfAbsent(player.getUUID(), 0);
        deaths.putIfAbsent(player.getUUID(), 0);
    }

    @Nullable
    private WarfareData.Spot chooseSpawn(ServerPlayer player) {
        List<WarfareData.Spot> spawns = WarfareData.get(player.server).getSpawns();
        if (spawns.isEmpty()) return null;

        List<ServerPlayer> enemies = new ArrayList<>();
        for (ServerPlayer p : player.server.getPlayerList().getPlayers()) {
            if (p.getUUID().equals(player.getUUID())) continue;
            if (respawnAt.containsKey(p.getUUID())) continue;   // en attente de reapparition
            if (p.isSpectator()) continue;
            enemies.add(p);
        }
        if (enemies.isEmpty()) return spawns.get(player.getRandom().nextInt(spawns.size()));

        WarfareData.Spot best = spawns.get(0);
        double bestDistance = -1;
        for (WarfareData.Spot spot : spawns) {
            double nearest = Double.MAX_VALUE;
            for (ServerPlayer enemy : enemies) {
                if (!enemy.level().dimension().equals(spot.dimension())) continue;
                nearest = Math.min(nearest, enemy.position().distanceToSqr(spot.x(), spot.y(), spot.z()));
            }
            if (nearest > bestDistance) {
                bestDistance = nearest;
                best = spot;
            }
        }
        return best;
    }

    private static void teleport(ServerPlayer player, WarfareData.Spot spot) {
        ServerLevel level = spot.level(player.server);
        if (level == null) return;
        player.fallDistance = 0;
        player.teleportTo(level, spot.x(), spot.y(), spot.z(), spot.yaw(), spot.pitch());
    }

    private static void reset(ServerPlayer player) {
        player.getInventory().clearContent();
        player.removeAllEffects();
        player.clearFire();
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        player.fallDistance = 0;
        player.inventoryMenu.broadcastChanges();
    }

    private void applyGameRules(MinecraftServer server) {
        if (!WarfareConfig.manageGameRules) return;
        GameRules rules = server.getGameRules();
        rules.getRule(GameRules.RULE_DO_IMMEDIATE_RESPAWN).set(true, server);   // pas d'ecran de mort
        rules.getRule(GameRules.RULE_KEEPINVENTORY).set(true, server);          // pas d'armes au sol
        rules.getRule(GameRules.RULE_SHOWDEATHMESSAGES).set(false, server);     // killfeed maison
    }

    // ------------------------------------------------------------- affichage

    private void refreshSidebars(MinecraftServer server) {
        List<Map.Entry<UUID, Integer>> ranking = ranking();
        int seconds = Math.max(0, matchTicksLeft / 20);
        String title = "§c§lWARFARE  §f" + Txt.clock(seconds);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Map<String, Integer> lines = new LinkedHashMap<>();
            int myRank = -1;
            for (int i = 0; i < ranking.size(); i++) {
                UUID id = ranking.get(i).getKey();
                if (id.equals(player.getUUID())) myRank = i + 1;
                if (i >= 5) continue;
                String colour = id.equals(player.getUUID()) ? "§a" : "§f";
                lines.put(Sidebar.line("§7#" + (i + 1) + " " + colour + nameOf(server, id)),
                        ranking.get(i).getValue());
            }
            if (myRank > 5) {
                lines.put(Sidebar.line("§a▶ Vous §7(#" + myRank + ")"),
                        kills.getOrDefault(player.getUUID(), 0));
            }
            Sidebar.show(player, title, lines);
        }
        lastClockShown = seconds;
    }

    /** Etat de la partie pour les ecrans client (hub du lobby et ecran de preparation). */
    private void broadcastMatchState(MinecraftServer server) {
        List<WarfareNetwork.MatchState.Entry> entries = new ArrayList<>();
        for (Map.Entry<UUID, Integer> e : ranking()) {
            entries.add(new WarfareNetwork.MatchState.Entry(nameOf(server, e.getKey()), e.getValue()));
        }
        int seconds = switch (state) {
            case COUNTDOWN -> (int) Math.ceil((WarfareConfig.countdownSeconds * 20 - stateTicks) / 20.0);
            case RUNNING -> Math.max(0, matchTicksLeft / 20);
            case ENDED -> Math.max(0, (WarfareConfig.endScreenSeconds * 20 - stateTicks) / 20);
            case LOBBY -> 0;
        };
        boolean ready = WarfareData.get(server).isReady();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            String map = player.level().dimension().location().getPath().toUpperCase(java.util.Locale.ROOT);
            WarfareNetwork.CHANNEL.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                    new WarfareNetwork.MatchState(state.name(), seconds, server.getMaxPlayers(),
                            player.hasPermissions(2) && state == GameState.LOBBY && ready,
                            map, WarfareConfig.killLimit, WarfareConfig.matchSeconds, entries));
        }
    }

    private List<Map.Entry<UUID, Integer>> ranking() {
        List<Map.Entry<UUID, Integer>> list = new ArrayList<>(kills.entrySet());
        list.sort((a, b) -> b.getValue() - a.getValue());
        return list;
    }

    @Nullable
    private UUID bestPlayer() {
        List<Map.Entry<UUID, Integer>> ranking = ranking();
        if (ranking.isEmpty()) return null;
        if (ranking.size() > 1 && ranking.get(0).getValue().equals(ranking.get(1).getValue())) return null;
        return ranking.get(0).getKey();
    }

    private void broadcastRanking(MinecraftServer server) {
        broadcast(server, Txt.c("--- Classement final ---", ChatFormatting.GOLD, ChatFormatting.BOLD));
        int rank = 1;
        for (Map.Entry<UUID, Integer> e : ranking()) {
            broadcast(server, Component.empty()
                    .append(Txt.c("#" + rank + " ", ChatFormatting.GRAY))
                    .append(Txt.c(nameOf(server, e.getKey()), ChatFormatting.WHITE))
                    .append(Txt.c("  " + e.getValue() + " elim.", ChatFormatting.GREEN))
                    .append(Txt.c("  " + deaths.getOrDefault(e.getKey(), 0) + " morts", ChatFormatting.DARK_GRAY)));
            rank++;
        }
    }

    private static String nameOf(MinecraftServer server, UUID id) {
        ServerPlayer p = server.getPlayerList().getPlayer(id);
        if (p != null) return p.getGameProfile().getName();
        return server.getProfileCache() == null ? id.toString().substring(0, 8)
                : server.getProfileCache().get(id).map(com.mojang.authlib.GameProfile::getName)
                        .orElse(id.toString().substring(0, 8));
    }

    private static void broadcast(MinecraftServer server, Component message) {
        server.getPlayerList().broadcastSystemMessage(message, false);
    }

    private GameManager() {}
}
