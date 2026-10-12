package com.warfare.client;

import com.warfare.net.WarfareNetwork;
import net.minecraft.client.Minecraft;

import java.util.List;

/** Copie cliente de l'etat de la partie, poussee par le serveur une fois par seconde. */
public final class ClientMatchState {
    public static String state = "LOBBY";
    public static int secondsLeft;
    public static int maxPlayers = 18;
    public static boolean canStart;
    public static String mapName = "OVERWORLD";
    public static int killLimit = 30;
    public static int matchSeconds = 600;
    /** Vrai des que ce serveur a envoye son premier etat : il fait tourner le mod. */
    public static boolean synced;
    public static List<WarfareNetwork.MatchState.Entry> players = List.of();

    private static String previous = "LOBBY";

    public static void accept(WarfareNetwork.MatchState msg) {
        state = msg.state();
        secondsLeft = msg.secondsLeft();
        maxPlayers = msg.maxPlayers();
        canStart = msg.canStart();
        mapName = msg.mapName();
        killLimit = msg.killLimit();
        matchSeconds = msg.matchSeconds();
        players = msg.players();
        synced = true;

        if (!state.equals(previous)) {
            onChange(previous, state);
            previous = state;
        }
    }

    /** Le compte a rebours ouvre l'ecran de preparation ; le debut de partie le referme. */
    private static void onChange(String from, String to) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;

        if (to.equals("COUNTDOWN")) {
            boolean busy = minecraft.screen != null
                    && !(minecraft.screen instanceof LobbyScreen)
                    && !(minecraft.screen instanceof ClassScreen);
            if (!busy) minecraft.setScreen(new MatchIntroScreen());
        } else if (minecraft.screen instanceof MatchIntroScreen) {
            minecraft.setScreen(null);
        }
    }

    /** A la deconnexion, on oublie tout : le menu ne doit pas s'ouvrir ailleurs. */
    public static void reset() {
        synced = false;
        state = "LOBBY";
        previous = "LOBBY";
        players = List.of();
        canStart = false;
    }

    public static boolean inLobby() {
        return state.equals("LOBBY") || state.equals("ENDED");
    }

    private ClientMatchState() {}
}
