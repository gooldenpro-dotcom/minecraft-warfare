package com.warfare.client;

import com.warfare.net.WarfareNetwork;
import net.minecraft.client.Minecraft;

import java.util.List;

/** Copie cliente de l'etat de la partie, poussee par le serveur plusieurs fois par seconde. */
public final class ClientMatchState {
    public static String state = "LOBBY";
    public static String mode = "tdm";
    public static String arena = "";
    public static List<String> arenas = List.of();
    public static int secondsLeft;
    public static int maxPlayers = 18;
    public static boolean canStart;
    public static int scoreLimit = 30;
    public static int matchSeconds = 600;
    public static int alphaScore;
    public static int bravoScore;
    public static String myTeam = "libre";
    public static int myScore;
    public static int myStreak;
    public static int nextStreakAt;
    public static int myLives;
    public static int ladderStep;
    public static int ladderSize;
    public static int level = 1;
    public static int prestige;
    public static long xpIntoLevel;
    public static long xpToNext = 400;
    public static List<WarfareNetwork.MatchState.Entry> players = List.of();
    public static List<WarfareNetwork.Leaderboard.Row> leaderboard = List.of();

    /** Vrai des que ce serveur a envoye son premier etat : il fait tourner le mod. */
    public static boolean synced;

    /**
     * Le joueur a ferme le menu d'accueil lui-meme : on ne le rouvre pas tout seul
     * tant qu'une nouvelle partie n'a pas commence. Sans ca, impossible de quitter.
     */
    public static boolean menuDismissed;

    private static String previous = "LOBBY";

    public static void accept(WarfareNetwork.MatchState msg) {
        state = msg.state();
        mode = msg.mode();
        arena = msg.arena();
        arenas = msg.arenas();
        secondsLeft = msg.secondsLeft();
        maxPlayers = msg.maxPlayers();
        canStart = msg.canStart();
        scoreLimit = msg.scoreLimit();
        matchSeconds = msg.matchSeconds();
        alphaScore = msg.alphaScore();
        bravoScore = msg.bravoScore();
        myTeam = msg.myTeam();
        myScore = msg.myScore();
        myStreak = msg.myStreak();
        nextStreakAt = msg.nextStreakAt();
        myLives = msg.myLives();
        ladderStep = msg.ladderStep();
        ladderSize = msg.ladderSize();
        level = msg.level();
        prestige = msg.prestige();
        xpIntoLevel = msg.xpIntoLevel();
        xpToNext = msg.xpToNext();
        players = msg.players();
        synced = true;

        if (!state.equals(previous)) {
            onChange(state);
            previous = state;
        }
    }

    public static void acceptLeaderboard(List<WarfareNetwork.Leaderboard.Row> rows) {
        leaderboard = rows;
    }

    /**
     * Le compte a rebours ouvre l'ecran de preparation ; le debut de partie le referme.
     * Le retour au menu apres une partie rouvre le menu d'accueil.
     */
    private static void onChange(String to) {
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
        if (to.equals("LOBBY")) menuDismissed = false;
    }

    /** A la deconnexion, on oublie tout : le menu ne doit pas s'ouvrir ailleurs. */
    public static void reset() {
        synced = false;
        menuDismissed = false;
        state = "LOBBY";
        previous = "LOBBY";
        players = List.of();
        leaderboard = List.of();
        arenas = List.of();
        canStart = false;
        myScore = 0;
        myStreak = 0;
    }

    public static boolean inLobby() {
        return state.equals("LOBBY") || state.equals("ENDED");
    }

    public static boolean running() {
        return state.equals("RUNNING");
    }

    public static boolean teamMode() {
        return mode.equals("tdm");
    }

    /** Libelle du mode, lu localement : les identifiants sont partages avec le serveur. */
    public static String modeTitle() {
        return com.warfare.game.GameMode.byId(mode).title();
    }

    private ClientMatchState() {}
}
