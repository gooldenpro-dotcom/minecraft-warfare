package com.warfare.game;

import com.warfare.WarfareConfig;
import com.warfare.loadout.ClassLoadout;
import com.warfare.loadout.ClassStore;
import com.warfare.net.WarfareNetwork;
import com.warfare.perk.ActivePerks;
import com.warfare.progression.PlayerStats;
import com.warfare.progression.Rank;
import com.warfare.progression.StatsStore;
import com.warfare.progression.Unlocks;
import com.warfare.streak.Killstreak;
import com.warfare.streak.StreakManager;
import com.warfare.util.Txt;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;

import javax.annotation.Nullable;
import java.util.*;

/** Machine a etats des parties multijoueur. Tout se passe cote serveur. */
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
    private GameMode mode = GameMode.TDM;
    private boolean modeLoaded;
    private int stateTicks;
    private int matchTicksLeft;
    private long tick;
    private int lastCountdownShown = -1;

    private final Map<UUID, Integer> scores = new HashMap<>();
    private final Map<UUID, Integer> matchKills = new HashMap<>();
    private final Map<UUID, Integer> matchDeaths = new HashMap<>();
    private final Map<UUID, Integer> streaks = new HashMap<>();
    private final Map<UUID, Team> teams = new HashMap<>();
    private final Map<UUID, Integer> lives = new HashMap<>();
    private final Map<UUID, Integer> ladder = new HashMap<>();
    private final EnumMap<Team, Integer> teamScores = new EnumMap<>(Team.class);

    private final Map<UUID, Long> respawnAt = new HashMap<>();
    private final Map<UUID, Long> protectedUntil = new HashMap<>();
    private final Map<UUID, Spot> deathSpots = new HashMap<>();
    private final List<Death> pending = new ArrayList<>();
    private final Map<UUID, Boolean> menuSynced = new HashMap<>();

    // ------------------------------------------------------------------ etat

    public GameState state() {
        return state;
    }

    public GameMode mode() {
        return mode;
    }

    public boolean isRunning() {
        return state == GameState.RUNNING;
    }

    /** Le menu de classes n'est utilisable qu'au lobby ou entre deux vies. */
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

    public Team teamOf(UUID id) {
        return teams.getOrDefault(id, Team.NONE);
    }

    /** Tir allie : vrai si les degats doivent etre annules. */
    public boolean isFriendly(UUID attacker, UUID victim) {
        if (WarfareConfig.friendlyFire || !mode.teamBased() || attacker.equals(victim)) return false;
        Team a = teamOf(attacker);
        Team b = teamOf(victim);
        return a != Team.NONE && a == b;
    }

    /** Multiplicateur de degats du mode en cours (elimination en un coup). */
    public double damageMultiplier() {
        return state == GameState.RUNNING ? mode.damageMultiplier() : 1.0;
    }

    // ------------------------------------------------------------------ morts

    /** Appele par LivingDeathEvent, avant que TaCZ ne poste son evenement de kill. */
    public void onDeath(ServerPlayer victim, @Nullable UUID killer) {
        deathSpots.put(victim.getUUID(), Spot.of(victim));
        pending.add(new Death(victim.getUUID(), killer, StreakManager.get().isStreakDamage()));
    }

    /** Appele par EntityKillByGunEvent : complete la mort deja enregistree. */
    public void onGunKill(UUID victim, @Nullable UUID killer, ResourceLocation gunId, boolean headshot) {
        for (int i = pending.size() - 1; i >= 0; i--) {
            Death death = pending.get(i);
            if (death.victim.equals(victim)) {
                death.gunId = gunId;
                death.headshot = headshot;
                if (killer != null) death.killer = killer;
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
        Team winningTeam = null;

        for (Death death : batch) {
            matchDeaths.merge(death.victim, 1, Integer::sum);
            streaks.remove(death.victim);
            String victimName = nameOf(server, death.victim);
            ServerPlayer victim = server.getPlayerList().getPlayer(death.victim);
            if (victim != null) {
                PlayerStats stats = StatsStore.of(victim);
                stats.deaths++;
            }
            if (mode.allowsStreaks()) StreakManager.get().onDeath(death.victim);
            consumeLife(server, death.victim);

            if (death.killer == null || death.killer.equals(death.victim)) {
                broadcast(server, Component.empty()
                        .append(Txt.c(victimName, ChatFormatting.WHITE))
                        .append(Txt.c(" s'est elimine tout seul", ChatFormatting.GRAY)));
                continue;
            }

            ServerPlayer killer = server.getPlayerList().getPlayer(death.killer);
            matchKills.merge(death.killer, 1, Integer::sum);
            announceKill(server, nameOf(server, death.killer), victimName, death.gunId, death.headshot,
                    teamOf(death.killer));

            int score = advanceScore(server, death.killer, killer);
            if (killer != null) rewardKill(killer, death.headshot, death.fromStreak);

            if (mode.teamBased()) {
                Team team = teamOf(death.killer);
                if (team != Team.NONE) {
                    int teamScore = teamScores.merge(team, 1, Integer::sum);
                    if (teamScore >= mode.scoreLimit() && winningTeam == null) winningTeam = team;
                }
            } else if (score >= mode.scoreLimit() && winner == null) {
                winner = death.killer;
            }
        }

        if (winningTeam != null) {
            endMatch(server, null, winningTeam, "score atteint");
        } else if (winner != null) {
            endMatch(server, winner, null, mode == GameMode.GUN_GAME
                    ? "echelle d'armes terminee" : "score atteint");
        }
    }

    /** Fait monter le score du tueur. Pour le jeu d'armes, l'echelle avance d'un cran. */
    private int advanceScore(MinecraftServer server, UUID killerId, @Nullable ServerPlayer killer) {
        int score = scores.merge(killerId, 1, Integer::sum);
        if (mode == GameMode.GUN_GAME && killer != null) {
            int step = Math.min(score, GameMode.ladder().size());
            ladder.put(killerId, step);
            if (step < GameMode.ladder().size()) {
                giveKit(killer);
                Txt.actionBar(killer, Txt.c("Arme " + (step + 1) + "/" + GameMode.ladder().size(),
                        ChatFormatting.GOLD));
            }
        }
        if (mode == GameMode.ONE_IN_CHAMBER && killer != null) refillChamber(killer);
        return score;
    }

    /** XP, serie et statistiques d'une elimination. */
    private void rewardKill(ServerPlayer killer, boolean headshot, boolean fromStreak) {
        PlayerStats stats = StatsStore.of(killer);
        stats.kills++;
        if (headshot) stats.headshots++;

        int streak = streaks.merge(killer.getUUID(), 1, Integer::sum);
        if (streak > stats.bestStreak) stats.bestStreak = streak;

        int points = WarfareConfig.xpPerKill + (headshot ? WarfareConfig.xpHeadshotBonus : 0);
        awardXp(killer, points, headshot ? "TIR A LA TETE" : "ELIMINATION");

        if (mode.allowsStreaks()) {
            StreakManager.get().onKill(killer, fromStreak);
            for (Killstreak candidate : ClassStore.get(killer.server, killer.getUUID()).current().streaks()) {
                if (candidate.threshold() == streak) {
                    awardXp(killer, WarfareConfig.xpPerStreak, candidate.title().toUpperCase(Locale.ROOT));
                }
            }
        }
    }

    /** Verse de l'XP, affiche les points a l'ecran et annonce les montees de niveau. */
    public void awardXp(ServerPlayer player, int base, String label) {
        if (base <= 0) return;
        int points = (int) Math.round(base * WarfareConfig.xpMultiplier);
        if (points <= 0) return;

        PlayerStats stats = StatsStore.of(player);
        int before = stats.level;
        stats.addXp(points);
        WarfareNetwork.score(player, points, label);

        if (stats.level > before) {
            Txt.title(player, Txt.c("NIVEAU " + stats.level, ChatFormatting.GOLD, ChatFormatting.BOLD),
                    Txt.c(Rank.title(stats.level), ChatFormatting.GRAY), 0, 40, 10);
            player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f, 1.0f);
            // Nouveau palier : le client doit connaitre ses nouveaux deblocages.
            WarfareNetwork.sync(player, isMenuAllowed(player.getUUID()));
        }
    }

    /** Modes a vies limitees : passe en spectateur quand il n'en reste plus. */
    private void consumeLife(MinecraftServer server, UUID id) {
        if (mode.lives() <= 0) return;
        int left = lives.merge(id, -1, Integer::sum);
        ServerPlayer player = server.getPlayerList().getPlayer(id);
        if (left > 0 || player == null) return;
        lives.put(id, 0);
        respawnAt.remove(id);
        player.setGameMode(GameType.SPECTATOR);
        Txt.title(player, Txt.c("ELIMINE", ChatFormatting.RED, ChatFormatting.BOLD),
                Txt.c("Plus de vies", ChatFormatting.GRAY), 0, 60, 10);
    }

    /** Une balle chargee : une elimination rend une balle. */
    private void refillChamber(ServerPlayer player) {
        var inventory = player.getInventory();
        for (int slot = 0; slot < 9; slot++) {
            var stack = inventory.getItem(slot);
            var gun = com.tacz.guns.api.item.IGun.getIGunOrNull(stack);
            if (gun == null) continue;
            gun.setCurrentAmmoCount(stack, gun.getCurrentAmmoCount(stack) + 1);
            inventory.setItem(slot, stack);
            player.inventoryMenu.broadcastChanges();
            return;
        }
    }

    private void announceKill(MinecraftServer server, String killer, String victim,
                              @Nullable ResourceLocation gunId, boolean headshot, Team team) {
        var message = Component.empty()
                .append(Txt.c(killer, team == Team.NONE ? ChatFormatting.RED : team.chat()))
                .append(Txt.c(" a elimine ", ChatFormatting.GRAY))
                .append(Txt.c(victim, ChatFormatting.WHITE));
        if (gunId != null && WarfareConfig.showGunInKillfeed) {
            message.append(Txt.c(" [", ChatFormatting.DARK_GRAY))
                    .append(Loadout.gunName(gunId).copy().withStyle(ChatFormatting.GOLD))
                    .append(Txt.c("]", ChatFormatting.DARK_GRAY));
        }
        if (headshot && WarfareConfig.announceHeadshots) {
            message.append(Txt.c(" TETE", ChatFormatting.YELLOW, ChatFormatting.BOLD));
        }
        broadcast(server, message);
    }

    // ------------------------------------------------------------ connexions

    public void onJoin(ServerPlayer player) {
        UUID id = player.getUUID();
        scores.putIfAbsent(id, 0);
        ClassStore.get(player.server, id);
        StatsStore.of(player);
        loadMode(player.server);
        pushMenuState(player, true);
        WarfareNetwork.leaderboard(player);

        if (state == GameState.RUNNING) {
            if (mode.teamBased()) Teams.assign(player, assignTeam(player.server, id));
            if (mode.lives() > 0) lives.putIfAbsent(id, mode.lives());
            spawnIntoMatch(player);
        } else {
            toLobby(player);
        }
    }

    public void onLeave(ServerPlayer player) {
        UUID id = player.getUUID();
        ClassStore.save(player.server, id);
        ClassStore.unload(id);
        StatsStore.unload(player.server, id);
        ActivePerks.forget(id);
        Teams.clear(player);
        respawnAt.remove(id);
        protectedUntil.remove(id);
        deathSpots.remove(id);
        menuSynced.remove(id);
        streaks.remove(id);
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

    /** Reapparition vanilla immediate : on bascule en spectateur pendant le delai. */
    public void onRespawn(ServerPlayer player) {
        if (state != GameState.RUNNING) {
            toLobby(player);
            return;
        }
        UUID id = player.getUUID();
        if (mode.lives() > 0 && lives.getOrDefault(id, 1) <= 0) {
            player.setGameMode(GameType.SPECTATOR);
            return;
        }
        player.setGameMode(GameType.SPECTATOR);
        Spot spot = deathSpots.get(id);
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
        loadMode(server);
        flushDeaths(server);
        if (state == GameState.RUNNING && mode.allowsStreaks()) StreakManager.get().tick(server);

        switch (state) {
            case LOBBY -> tickLobby(server);
            case COUNTDOWN -> tickCountdown(server);
            case RUNNING -> tickRunning(server);
            case ENDED -> tickEnded(server);
        }

        if (tick % 20 == 0) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                player.getFoodData().setFoodLevel(20);
                pushMenuState(player, false);
            }
            keepWorldQuiet(server);
        }
        if (tick % 10 == 0) broadcastMatchState(server);
    }

    private void tickLobby(MinecraftServer server) {
        if (tick % 20 != 0) return;
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        WarfareData data = WarfareData.get(server);

        if (!data.isReady()) {
            for (ServerPlayer player : players) {
                Txt.actionBar(player, Txt.c("Arene non configuree : /warfare arena create <nom>",
                        ChatFormatting.RED));
            }
            return;
        }
        if (!WarfareConfig.autoStart) return;
        if (players.size() >= mode.minPlayers()) {
            startCountdown(server);
        }
    }

    private void tickCountdown(MinecraftServer server) {
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.size() < mode.minPlayers()) {
            broadcast(server, Txt.info(Txt.c("Pas assez de joueurs, compte a rebours annule.",
                    ChatFormatting.GRAY)));
            for (ServerPlayer player : players) Txt.clearTitle(player);
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
            for (ServerPlayer player : players) {
                Txt.title(player, Txt.c(String.valueOf(left), color, ChatFormatting.BOLD),
                        Txt.c(mode.title(), ChatFormatting.GRAY), 0, 25, 5);
            }
        }
    }

    private void tickRunning(MinecraftServer server) {
        matchTicksLeft--;

        if (!respawnAt.isEmpty()) {
            for (UUID id : List.copyOf(respawnAt.keySet())) {
                if (tick < respawnAt.get(id)) continue;
                respawnAt.remove(id);
                ServerPlayer player = server.getPlayerList().getPlayer(id);
                if (player != null) spawnIntoMatch(player);
            }
        }

        if (!protectedUntil.isEmpty()) {
            for (UUID id : List.copyOf(protectedUntil.keySet())) {
                if (tick < protectedUntil.get(id)) continue;
                protectedUntil.remove(id);
                ServerPlayer player = server.getPlayerList().getPlayer(id);
                if (player != null && !respawnAt.containsKey(id)) player.setInvulnerable(false);
            }
        }

        if (matchTicksLeft <= 0) {
            if (mode.teamBased()) endMatch(server, null, bestTeam(), "temps ecoule");
            else endMatch(server, bestPlayer(), null, "temps ecoule");
            return;
        }
        if (tick % 20 == 0) refreshSidebars(server);
    }

    private void tickEnded(MinecraftServer server) {
        if (stateTicks >= WarfareConfig.endScreenSeconds * 20) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                Txt.clearTitle(player);
                toLobby(player);
            }
            enter(GameState.LOBBY);
        }
    }

    // ---------------------------------------------------------- transitions

    private void enter(GameState next) {
        state = next;
        stateTicks = 0;
        lastCountdownShown = -1;
    }

    /** Le mode est conserve avec le monde : on le relit une fois le serveur pret. */
    private void loadMode(MinecraftServer server) {
        if (modeLoaded) return;
        mode = WarfareData.get(server).mode();
        modeLoaded = true;
    }

    /** Bouton LANCER LA PARTIE : messages d'erreur explicites pour le joueur. */
    public void requestStart(ServerPlayer player) {
        MinecraftServer server = player.server;
        if (state != GameState.LOBBY) {
            player.sendSystemMessage(Txt.info(Txt.c("Une partie est deja en cours.", ChatFormatting.RED)));
            return;
        }
        WarfareData data = WarfareData.get(server);
        Arena arena = data.current();
        if (arena == null) {
            player.sendSystemMessage(Txt.info(Txt.c(
                    "Aucune carte configuree : /warfare arena create <nom>", ChatFormatting.RED)));
            return;
        }
        if (!arena.isReady()) {
            player.sendSystemMessage(Txt.info(Txt.c("Carte " + arena.title()
                    + " incomplete : il faut un lobby et au moins un point d'apparition.",
                    ChatFormatting.RED)));
            return;
        }
        if (!arena.isReadyFor(mode)) {
            player.sendSystemMessage(Txt.info(Txt.c("Mode par equipe : il faut des points "
                    + "alpha et bravo (/warfare addspawn alpha).", ChatFormatting.RED)));
            return;
        }
        int players = server.getPlayerList().getPlayers().size();
        if (players < mode.minPlayers()) {
            player.sendSystemMessage(Txt.info(Txt.c("Il faut " + mode.minPlayers()
                    + " joueur(s) ; il y en a " + players + ".", ChatFormatting.RED)));
            return;
        }
        startCountdown(server);
    }

    public boolean startCountdown(MinecraftServer server) {
        if (state != GameState.LOBBY) return false;
        WarfareData data = WarfareData.get(server);
        if (!data.isReadyFor(mode)) return false;
        enter(GameState.COUNTDOWN);
        broadcast(server, Txt.info(Component.empty()
                .append(Txt.c(mode.title(), ChatFormatting.GOLD))
                .append(Txt.c(" sur ", ChatFormatting.GRAY))
                .append(Txt.c(data.currentName().toUpperCase(Locale.ROOT), ChatFormatting.WHITE))
                .append(Txt.c(" dans " + WarfareConfig.countdownSeconds + " s.", ChatFormatting.GRAY))));
        return true;
    }

    /** @param actor le joueur a l'origine du changement, ou null depuis la console. */
    public boolean setMode(MinecraftServer server, @Nullable ServerPlayer actor, GameMode next) {
        if (state != GameState.LOBBY) {
            reply(actor, Txt.c("Changement de mode impossible en partie.", ChatFormatting.RED));
            return false;
        }
        mode = next;
        modeLoaded = true;
        WarfareData.get(server).setMode(next);
        broadcast(server, Txt.info(Component.empty()
                .append(Txt.c(actorName(actor), ChatFormatting.WHITE))
                .append(Txt.c(" choisit ", ChatFormatting.GRAY))
                .append(Txt.c(next.title(), ChatFormatting.GOLD))));
        return true;
    }

    public boolean setArena(MinecraftServer server, @Nullable ServerPlayer actor, String name) {
        if (state != GameState.LOBBY) {
            reply(actor, Txt.c("Changement de carte impossible en partie.", ChatFormatting.RED));
            return false;
        }
        WarfareData data = WarfareData.get(server);
        if (!data.select(name)) {
            reply(actor, Txt.c("Carte inconnue : " + name, ChatFormatting.RED));
            return false;
        }
        broadcast(server, Txt.info(Component.empty()
                .append(Txt.c(actorName(actor), ChatFormatting.WHITE))
                .append(Txt.c(" choisit la carte ", ChatFormatting.GRAY))
                .append(Txt.c(data.currentName().toUpperCase(Locale.ROOT), ChatFormatting.GOLD))));
        for (ServerPlayer other : server.getPlayerList().getPlayers()) toLobby(other);
        return true;
    }

    private static String actorName(@Nullable ServerPlayer actor) {
        return actor == null ? "La console" : actor.getGameProfile().getName();
    }

    private static void reply(@Nullable ServerPlayer actor, Component message) {
        if (actor != null) actor.sendSystemMessage(Txt.info(message));
    }

    private void startMatch(MinecraftServer server) {
        enter(GameState.RUNNING);
        matchTicksLeft = mode.seconds() * 20;
        scores.clear();
        matchKills.clear();
        matchDeaths.clear();
        streaks.clear();
        respawnAt.clear();
        protectedUntil.clear();
        lives.clear();
        ladder.clear();
        teams.clear();
        teamScores.clear();
        pending.clear();
        StreakManager.get().reset(server);
        applyGameRules(server);
        cleanMobs(server);

        List<ServerPlayer> players = new ArrayList<>(server.getPlayerList().getPlayers());
        Collections.shuffle(players);
        int index = 0;
        for (ServerPlayer player : players) {
            UUID id = player.getUUID();
            scores.put(id, 0);
            if (mode.lives() > 0) lives.put(id, mode.lives());
            if (mode == GameMode.GUN_GAME) ladder.put(id, 0);
            if (mode.teamBased()) {
                Team team = index++ % 2 == 0 ? Team.ALPHA : Team.BRAVO;
                teams.put(id, team);
                Teams.assign(player, team);
            } else {
                Teams.clear(player);
            }
        }
        for (ServerPlayer player : players) {
            spawnIntoMatch(player);
            Component subtitle = mode.teamBased()
                    ? Txt.c("Equipe " + teamOf(player.getUUID()).title(),
                            teamOf(player.getUUID()).chat())
                    : Txt.c(mode.title() + " - " + mode.scoreLimit() + " points", ChatFormatting.GRAY);
            Txt.title(player, Txt.c("A L'ATTAQUE", ChatFormatting.GREEN, ChatFormatting.BOLD),
                    subtitle, 0, 35, 10);
        }
        broadcast(server, Txt.info(Component.empty()
                .append(Txt.c(mode.title(), ChatFormatting.GOLD))
                .append(Txt.c(" : " + mode.scoreLimit() + " points, "
                        + mode.seconds() / 60 + " min.", ChatFormatting.GREEN))));
        refreshSidebars(server);
    }

    public void endMatch(MinecraftServer server, @Nullable UUID winner, @Nullable Team winningTeam,
                         String reason) {
        if (state != GameState.RUNNING) return;
        enter(GameState.ENDED);
        respawnAt.clear();
        StreakManager.get().reset(server);

        String winnerName = winner == null ? null : nameOf(server, winner);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Sidebar.hide(player);
            boolean won = mode.teamBased()
                    ? winningTeam != null && teamOf(player.getUUID()) == winningTeam
                    : winner != null && player.getUUID().equals(winner);

            Component title;
            if (mode.teamBased()) {
                title = winningTeam == null
                        ? Txt.c("EGALITE", ChatFormatting.YELLOW, ChatFormatting.BOLD)
                        : Txt.c(won ? "VICTOIRE" : "DEFAITE",
                                won ? ChatFormatting.GREEN : ChatFormatting.RED, ChatFormatting.BOLD);
            } else if (winnerName == null) {
                title = Txt.c("EGALITE", ChatFormatting.YELLOW, ChatFormatting.BOLD);
            } else {
                title = Txt.c(won ? "VICTOIRE" : winnerName + " gagne",
                        won ? ChatFormatting.GREEN : ChatFormatting.GOLD, ChatFormatting.BOLD);
            }
            Txt.title(player, title, Txt.c(reason, ChatFormatting.GRAY), 5,
                    WarfareConfig.endScreenSeconds * 20, 10);
            closeMatchStats(player, won);
        }
        broadcastRanking(server, winningTeam);
        StatsStore.saveAll(server);
        WarfareNetwork.leaderboardToAll(server);
    }

    /** Bilan de fin de partie d'un joueur : statistiques et XP de participation. */
    private void closeMatchStats(ServerPlayer player, boolean won) {
        PlayerStats stats = StatsStore.of(player);
        stats.matches++;
        if (won) stats.wins++;
        else stats.losses++;
        int kills = matchKills.getOrDefault(player.getUUID(), 0);
        if (kills > stats.bestKillsInMatch) stats.bestKillsInMatch = kills;
        stats.secondsPlayed += Math.max(0, mode.seconds() - matchTicksLeft / 20);

        awardXp(player, WarfareConfig.xpPerMatch, "PARTIE TERMINEE");
        awardXp(player, won ? WarfareConfig.xpPerWin : WarfareConfig.xpPerLoss,
                won ? "VICTOIRE" : "DEFAITE");
    }

    /** /warfare stop : coupe la partie sans vainqueur et revient directement au lobby. */
    public void abort(MinecraftServer server) {
        enter(GameState.LOBBY);
        respawnAt.clear();
        pending.clear();
        StreakManager.get().reset(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Sidebar.hide(player);
            Txt.clearTitle(player);
            Teams.clear(player);
            toLobby(player);
        }
        StatsStore.saveAll(server);
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
        Spot lobby = WarfareData.get(server).getLobby();
        if (lobby != null) {
            teleport(player, lobby);
            player.setRespawnPosition(lobby.dimension(), player.blockPosition(), lobby.yaw(), true, false);
        }
    }

    /** Entree en jeu : point d'apparition le plus eloigne des ennemis + equipement du mode. */
    public void spawnIntoMatch(ServerPlayer player) {
        reset(player);
        player.setGameMode(GameType.ADVENTURE);
        Spot spot = chooseSpawn(player);
        if (spot != null) teleport(player, spot);
        giveKit(player);
        player.setInvulnerable(WarfareConfig.spawnProtectionSeconds > 0);
        protectedUntil.put(player.getUUID(), tick + WarfareConfig.spawnProtectionSeconds * 20L);
        Txt.clearTitle(player);
        scores.putIfAbsent(player.getUUID(), 0);
    }

    /** L'equipement depend du mode : classe du joueur, arme imposee ou echelle d'armes. */
    private void giveKit(ServerPlayer player) {
        switch (mode.kit()) {
            case CLASS -> {
                ClassLoadout loadout = ClassStore.get(player.server, player.getUUID()).current();
                Loadout.give(player, loadout);
                player.sendSystemMessage(Txt.info(Txt.c("Classe : " + loadout.name(), ChatFormatting.GRAY)));
            }
            case FIXED -> Loadout.giveSingle(player, mode.fixedGun(), mode.fixedMagazines(),
                    mode.fixedStartAmmo());
            case LADDER -> {
                List<String> rungs = GameMode.ladder();
                int step = Math.min(ladder.getOrDefault(player.getUUID(), 0), rungs.size() - 1);
                Loadout.giveSingle(player, rungs.get(step), 3, -1);
            }
        }
    }

    @Nullable
    private Spot chooseSpawn(ServerPlayer player) {
        Team team = teamOf(player.getUUID());
        List<Spot> spawns = WarfareData.get(player.server).spawnsFor(team);
        if (spawns.isEmpty()) return null;

        List<ServerPlayer> enemies = new ArrayList<>();
        for (ServerPlayer other : player.server.getPlayerList().getPlayers()) {
            if (other.getUUID().equals(player.getUUID())) continue;
            if (respawnAt.containsKey(other.getUUID())) continue;
            if (other.isSpectator()) continue;
            if (mode.teamBased() && team != Team.NONE && teamOf(other.getUUID()) == team) continue;
            enemies.add(other);
        }
        if (enemies.isEmpty()) return spawns.get(player.getRandom().nextInt(spawns.size()));

        Spot best = spawns.get(0);
        double bestDistance = -1;
        for (Spot spot : spawns) {
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

    /** Equipe la moins nombreuse, pour un joueur qui arrive en cours de partie. */
    private Team assignTeam(MinecraftServer server, UUID id) {
        Team existing = teams.get(id);
        if (existing != null && existing != Team.NONE) return existing;
        int alpha = 0;
        int bravo = 0;
        for (Team team : teams.values()) {
            if (team == Team.ALPHA) alpha++;
            else if (team == Team.BRAVO) bravo++;
        }
        Team team = alpha <= bravo ? Team.ALPHA : Team.BRAVO;
        teams.put(id, team);
        return team;
    }

    private static void teleport(ServerPlayer player, Spot spot) {
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

    // ------------------------------------------------------------ le monde

    private void applyGameRules(MinecraftServer server) {
        if (!WarfareConfig.manageGameRules) return;
        GameRules rules = server.getGameRules();
        rules.getRule(GameRules.RULE_DO_IMMEDIATE_RESPAWN).set(true, server);   // pas d'ecran de mort
        rules.getRule(GameRules.RULE_KEEPINVENTORY).set(true, server);          // pas d'armes au sol
        rules.getRule(GameRules.RULE_SHOWDEATHMESSAGES).set(false, server);     // killfeed maison
        rules.getRule(GameRules.RULE_ANNOUNCE_ADVANCEMENTS).set(false, server);
        rules.getRule(GameRules.RULE_MOBGRIEFING).set(false, server);
        rules.getRule(GameRules.RULE_DOFIRETICK).set(false, server);
        if (WarfareConfig.disableMobs) {
            rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
            rules.getRule(GameRules.RULE_DOINSOMNIA).set(false, server);
            rules.getRule(GameRules.RULE_DO_PATROL_SPAWNING).set(false, server);
            rules.getRule(GameRules.RULE_DO_TRADER_SPAWNING).set(false, server);
        }
        if (WarfareConfig.fixedDaytime >= 0) {
            rules.getRule(GameRules.RULE_DAYLIGHT).set(false, server);
        }
        if (WarfareConfig.disableWeather) {
            rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
        }
    }

    /** Heure fixe, pas de meteo, pas de monstres : une arene stable d'une partie a l'autre. */
    public void keepWorldQuiet(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            if (WarfareConfig.fixedDaytime >= 0 && level.getDayTime() % 24000 != WarfareConfig.fixedDaytime) {
                level.setDayTime(WarfareConfig.fixedDaytime);
            }
            if (WarfareConfig.disableWeather && (level.isRaining() || level.isThundering())) {
                level.setWeatherParameters(6000, 0, false, false);
            }
        }
        if (WarfareConfig.disableMobs && tick % 100 == 0) cleanMobs(server);
    }

    /**
     * Au premier demarrage sur un monde, le serveur se configure tout seul : une arene
     * au point d'apparition du monde, avec quatre points repartis en deux camps.
     * Les positions se corrigent ensuite en jeu (/warfare setlobby, /warfare addspawn).
     */
    public void ensureDefaultArena(MinecraftServer server) {
        WarfareData data = WarfareData.get(server);
        Arena existing = data.current();
        if (existing != null && existing.isReady()) return;

        ServerLevel level = server.overworld();
        String name = WarfareData.key(server.getWorldData().getLevelName());
        Arena arena = data.getOrCreate(name.isBlank() ? "defaut" : name);
        net.minecraft.core.BlockPos spawn = level.getSharedSpawnPos();

        if (arena.lobby() == null) arena.setLobby(safeSpot(level, spawn, 0, 0, 180f));
        if (arena.spawns().isEmpty()) {
            arena.add(Team.ALPHA, safeSpot(level, spawn, 10, 0, -90f));
            arena.add(Team.ALPHA, safeSpot(level, spawn, 10, 6, -90f));
            arena.add(Team.BRAVO, safeSpot(level, spawn, -10, 0, 90f));
            arena.add(Team.BRAVO, safeSpot(level, spawn, -10, 6, 90f));
        }
        data.select(arena.name());
        data.touch();
        com.warfare.Warfare.LOGGER.info("Arene \"{}\" creee automatiquement autour de {} : "
                + "a corriger en jeu avec /warfare setlobby et /warfare addspawn", arena.name(), spawn);
    }

    /** Cherche, autour de la position voulue, un sol avec deux blocs d'air au-dessus. */
    private static Spot safeSpot(ServerLevel level, net.minecraft.core.BlockPos center,
                                 int dx, int dz, float yaw) {
        net.minecraft.core.BlockPos base = center.offset(dx, 0, dz);
        for (int dy = 0; dy <= 10; dy++) {
            for (int sign = 1; sign >= -1; sign -= 2) {
                net.minecraft.core.BlockPos pos = base.above(sign * dy);
                if (pos.getY() <= level.getMinBuildHeight() + 1) continue;
                if (level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()
                        && !level.getBlockState(pos.below()).isAir()) {
                    return new Spot(level.dimension(), pos.getX() + 0.5, pos.getY(),
                            pos.getZ() + 0.5, yaw, 0f);
                }
            }
        }
        return new Spot(level.dimension(), base.getX() + 0.5, base.getY(), base.getZ() + 0.5, yaw, 0f);
    }

    /** Supprime les monstres deja presents (les mods peuvent en poser hors spawn naturel). */
    public void cleanMobs(MinecraftServer server) {
        if (!WarfareConfig.disableMobs) return;
        for (ServerLevel level : server.getAllLevels()) {
            List<Entity> doomed = new ArrayList<>();
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof Enemy) doomed.add(entity);
            }
            for (Entity entity : doomed) entity.discard();
        }
    }

    // ------------------------------------------------------------- affichage

    private void refreshSidebars(MinecraftServer server) {
        int seconds = Math.max(0, matchTicksLeft / 20);
        String title = "§c§l" + shortTitle() + "  §f" + Txt.clock(seconds);

        if (mode.teamBased()) {
            Map<String, Integer> lines = new LinkedHashMap<>();
            lines.put(Sidebar.line("§c" + Team.ALPHA.title()), teamScores.getOrDefault(Team.ALPHA, 0));
            lines.put(Sidebar.line("§b" + Team.BRAVO.title()), teamScores.getOrDefault(Team.BRAVO, 0));
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                Map<String, Integer> mine = new LinkedHashMap<>(lines);
                mine.put(Sidebar.line("§7Mes eliminations"), scores.getOrDefault(player.getUUID(), 0));
                Sidebar.show(player, title, mine);
            }
            return;
        }

        List<Map.Entry<UUID, Integer>> ranking = ranking();
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
                        scores.getOrDefault(player.getUUID(), 0));
            }
            Sidebar.show(player, title, lines);
        }
    }

    private String shortTitle() {
        return switch (mode) {
            case TDM -> "EQUIPES";
            case FFA -> "MELEE";
            case GUN_GAME -> "JEU D'ARMES";
            case ONE_IN_CHAMBER -> "UNE BALLE";
            case SNIPER_DUEL -> "1V1 SNIPER";
            case DUEL -> "1V1";
        };
    }

    /** Etat de la partie pour les menus et l'ATH. */
    private void broadcastMatchState(MinecraftServer server) {
        WarfareData data = WarfareData.get(server);
        List<WarfareNetwork.MatchState.Entry> entries = new ArrayList<>();
        for (Map.Entry<UUID, Integer> entry : ranking()) {
            entries.add(new WarfareNetwork.MatchState.Entry(nameOf(server, entry.getKey()),
                    entry.getValue(), teamOf(entry.getKey()).id()));
        }
        if (state != GameState.RUNNING) {
            entries.clear();
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                entries.add(new WarfareNetwork.MatchState.Entry(player.getGameProfile().getName(),
                        StatsStore.of(player).level, teamOf(player.getUUID()).id()));
            }
        }

        int seconds = switch (state) {
            case COUNTDOWN -> (int) Math.ceil((WarfareConfig.countdownSeconds * 20 - stateTicks) / 20.0);
            case RUNNING -> Math.max(0, matchTicksLeft / 20);
            case ENDED -> Math.max(0, (WarfareConfig.endScreenSeconds * 20 - stateTicks) / 20);
            case LOBBY -> 0;
        };
        boolean ready = data.isReadyFor(mode);
        List<String> arenas = data.arenaNames();
        int ladderSize = GameMode.ladder().size();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID id = player.getUUID();
            PlayerStats stats = StatsStore.of(player);
            boolean canStart = WarfareNetwork.canControl(player) && state == GameState.LOBBY && ready;
            WarfareNetwork.CHANNEL.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                    new WarfareNetwork.MatchState(state.name(), mode.id(), data.currentName(), arenas,
                            seconds, server.getMaxPlayers(), canStart, mode.scoreLimit(), mode.seconds(),
                            teamScores.getOrDefault(Team.ALPHA, 0), teamScores.getOrDefault(Team.BRAVO, 0),
                            teamOf(id).id(), scores.getOrDefault(id, 0),
                            mode.allowsStreaks() ? StreakManager.get().streakOf(id) : 0,
                            nextStreakAt(player), lives.getOrDefault(id, 0),
                            ladder.getOrDefault(id, 0), mode == GameMode.GUN_GAME ? ladderSize : 0,
                            stats.level, stats.prestige,
                            Rank.intoLevel(stats.xp, stats.level), Rank.xpToNext(stats.level),
                            entries));
        }
    }

    /** Prochain seuil de killstreak du joueur, 0 s'il n'y en a plus. */
    private int nextStreakAt(ServerPlayer player) {
        if (!mode.allowsStreaks()) return 0;
        int current = StreakManager.get().streakOf(player.getUUID());
        int best = 0;
        for (Killstreak streak : ClassStore.get(player.server, player.getUUID()).current().streaks()) {
            int threshold = streak.threshold();
            if (threshold > current && (best == 0 || threshold < best)) best = threshold;
        }
        return best;
    }

    private List<Map.Entry<UUID, Integer>> ranking() {
        List<Map.Entry<UUID, Integer>> list = new ArrayList<>(scores.entrySet());
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

    @Nullable
    private Team bestTeam() {
        int alpha = teamScores.getOrDefault(Team.ALPHA, 0);
        int bravo = teamScores.getOrDefault(Team.BRAVO, 0);
        if (alpha == bravo) return null;
        return alpha > bravo ? Team.ALPHA : Team.BRAVO;
    }

    private void broadcastRanking(MinecraftServer server, @Nullable Team winningTeam) {
        broadcast(server, Txt.c("--- Classement final ---", ChatFormatting.GOLD, ChatFormatting.BOLD));
        if (mode.teamBased()) {
            broadcast(server, Component.empty()
                    .append(Txt.c(Team.ALPHA.title() + " " + teamScores.getOrDefault(Team.ALPHA, 0),
                            Team.ALPHA.chat()))
                    .append(Txt.c("  -  ", ChatFormatting.DARK_GRAY))
                    .append(Txt.c(Team.BRAVO.title() + " " + teamScores.getOrDefault(Team.BRAVO, 0),
                            Team.BRAVO.chat())));
            if (winningTeam != null) {
                broadcast(server, Txt.c("Victoire de " + winningTeam.title(), winningTeam.chat(),
                        ChatFormatting.BOLD));
            }
        }
        int rank = 1;
        for (Map.Entry<UUID, Integer> entry : ranking()) {
            UUID id = entry.getKey();
            broadcast(server, Component.empty()
                    .append(Txt.c("#" + rank + " ", ChatFormatting.GRAY))
                    .append(Txt.c(nameOf(server, id), teamOf(id) == Team.NONE
                            ? ChatFormatting.WHITE : teamOf(id).chat()))
                    .append(Txt.c("  " + matchKills.getOrDefault(id, 0) + " elim.", ChatFormatting.GREEN))
                    .append(Txt.c("  " + matchDeaths.getOrDefault(id, 0) + " morts",
                            ChatFormatting.DARK_GRAY)));
            rank++;
        }
    }

    private static String nameOf(MinecraftServer server, UUID id) {
        ServerPlayer player = server.getPlayerList().getPlayer(id);
        if (player != null) return player.getGameProfile().getName();
        return server.getProfileCache() == null ? id.toString().substring(0, 8)
                : server.getProfileCache().get(id).map(com.mojang.authlib.GameProfile::getName)
                        .orElse(id.toString().substring(0, 8));
    }

    private static void broadcast(MinecraftServer server, Component message) {
        server.getPlayerList().broadcastSystemMessage(message, false);
    }

    private GameManager() {}
}
