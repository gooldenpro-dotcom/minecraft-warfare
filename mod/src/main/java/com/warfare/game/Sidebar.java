package com.warfare.game;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Scoreboard lateral par joueur.
 *
 * Le scoreboard du serveur est global : pour afficher une ligne "Vous" differente
 * a chaque joueur, on envoie les paquets a la main a partir d'un Scoreboard local
 * qui n'est jamais enregistre cote serveur. Le client maintient sa propre copie.
 */
public final class Sidebar {
    private static final String OBJECTIVE_NAME = "warfare";
    private static final int MAX_LINE = 40;

    private static final Scoreboard DUMMY = new Scoreboard();
    private static final Objective OBJECTIVE = new Objective(DUMMY, OBJECTIVE_NAME, ObjectiveCriteria.DUMMY,
            Component.literal("WARFARE"), ObjectiveCriteria.RenderType.INTEGER);

    private static final int METHOD_ADD = 0;
    private static final int METHOD_REMOVE = 1;
    private static final int METHOD_CHANGE = 2;

    /** Lignes actuellement affichees chez chaque joueur : texte -> score. */
    private static final Map<UUID, Map<String, Integer>> SHOWN = new HashMap<>();
    private static final Map<UUID, String> TITLES = new HashMap<>();

    /** (Re)affiche le scoreboard d'un joueur. */
    public static void show(ServerPlayer player, String title, Map<String, Integer> lines) {
        UUID id = player.getUUID();
        boolean fresh = !SHOWN.containsKey(id);

        if (fresh) {
            OBJECTIVE.setDisplayName(Component.literal(title));
            player.connection.send(new ClientboundSetObjectivePacket(OBJECTIVE, METHOD_ADD));
            player.connection.send(new ClientboundSetDisplayObjectivePacket(Scoreboard.DISPLAY_SLOT_SIDEBAR, OBJECTIVE));
            TITLES.put(id, title);
        } else if (!title.equals(TITLES.get(id))) {
            OBJECTIVE.setDisplayName(Component.literal(title));
            player.connection.send(new ClientboundSetObjectivePacket(OBJECTIVE, METHOD_CHANGE));
            TITLES.put(id, title);
        }

        Map<String, Integer> current = SHOWN.computeIfAbsent(id, k -> new LinkedHashMap<>());

        // Lignes disparues
        for (String old : Map.copyOf(current).keySet()) {
            if (!lines.containsKey(old)) {
                player.connection.send(new ClientboundSetScorePacket(
                        ServerScoreboard.Method.REMOVE, OBJECTIVE_NAME, old, 0));
                current.remove(old);
            }
        }
        // Lignes nouvelles ou changees
        for (Map.Entry<String, Integer> e : lines.entrySet()) {
            Integer previous = current.get(e.getKey());
            if (previous == null || !previous.equals(e.getValue())) {
                player.connection.send(new ClientboundSetScorePacket(
                        ServerScoreboard.Method.CHANGE, OBJECTIVE_NAME, e.getKey(), e.getValue()));
                current.put(e.getKey(), e.getValue());
            }
        }
    }

    public static void hide(ServerPlayer player) {
        if (SHOWN.remove(player.getUUID()) == null) return;
        TITLES.remove(player.getUUID());
        player.connection.send(new ClientboundSetObjectivePacket(OBJECTIVE, METHOD_REMOVE));
    }

    public static void forget(UUID id) {
        SHOWN.remove(id);
        TITLES.remove(id);
    }

    /** Les entrees du scoreboard sont des identifiants : uniques et bornees. */
    public static String line(String text) {
        return text.length() > MAX_LINE ? text.substring(0, MAX_LINE) : text;
    }

    private Sidebar() {}
}
