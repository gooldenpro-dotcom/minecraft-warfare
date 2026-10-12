package com.warfare.game;

public enum GameState {
    /** Personne ne joue : tout le monde attend dans le lobby. */
    LOBBY,
    /** Assez de joueurs : compte a rebours avant le debut. */
    COUNTDOWN,
    /** Partie en cours. */
    RUNNING,
    /** Ecran de fin, avant retour au lobby. */
    ENDED
}
