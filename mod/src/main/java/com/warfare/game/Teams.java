package com.warfare.game;

import com.warfare.WarfareConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

/**
 * Les camps cote scoreboard vanilla : couleur du pseudo au-dessus de la tete
 * et tir allie desactive. Les equipes sont recreees au demarrage du serveur.
 */
public final class Teams {
    private static final String PREFIX = "warfare_";

    /** Cree (ou met a jour) les equipes du scoreboard. */
    public static void ensure(MinecraftServer server) {
        for (Team team : Team.values()) {
            if (team == Team.NONE) continue;
            PlayerTeam entry = team(server, team);
            entry.setColor(team.chat());
            entry.setAllowFriendlyFire(WarfareConfig.friendlyFire);
            entry.setSeeFriendlyInvisibles(true);
            entry.setDisplayName(Component.literal(team.title()));
        }
    }

    private static PlayerTeam team(MinecraftServer server, Team team) {
        Scoreboard scoreboard = server.getScoreboard();
        String name = PREFIX + team.id();
        PlayerTeam existing = scoreboard.getPlayerTeam(name);
        return existing != null ? existing : scoreboard.addPlayerTeam(name);
    }

    public static void assign(ServerPlayer player, Team team) {
        Scoreboard scoreboard = player.server.getScoreboard();
        if (team == Team.NONE) {
            scoreboard.removePlayerFromTeam(player.getScoreboardName());
            return;
        }
        ensure(player.server);
        scoreboard.addPlayerToTeam(player.getScoreboardName(), team(player.server, team));
    }

    public static void clear(ServerPlayer player) {
        player.server.getScoreboard().removePlayerFromTeam(player.getScoreboardName());
    }

    private Teams() {}
}
