package com.warfare.game;

import com.warfare.WarfareConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Les modes de jeu multijoueur. Les limites de score et de duree viennent de la config
 * ([modes] dans warfare-common.toml), donc tout est relu a chaque appel.
 */
public enum GameMode {
    TDM("tdm", "Match a mort par equipe", "Deux equipes. Le premier camp au score gagne."),
    FFA("ffa", "Melee generale", "Chacun pour soi. Le premier au score gagne."),
    GUN_GAME("jeu_d_armes", "Jeu d'armes", "Une arme par elimination. Termine l'echelle pour gagner."),
    ONE_IN_CHAMBER("une_balle", "Une balle chargee", "Une balle, un mort. Chaque elimination rend une balle."),
    SNIPER_DUEL("1v1_sniper", "1VS1 Sniper", "Duel au fusil de precision, sur petite carte."),
    DUEL("1v1", "1VS1", "Duel a armes libres, avec tes classes.");

    /** Comment l'equipement est distribue dans ce mode. */
    public enum Kit {
        /** La classe choisie par le joueur. */
        CLASS,
        /** Une echelle d'armes qui monte a chaque elimination (jeu d'armes). */
        LADDER,
        /** Une arme imposee par le mode. */
        FIXED
    }

    private final String id;
    private final String title;
    private final String description;

    GameMode(String id, String title, String description) {
        this.id = id;
        this.title = title;
        this.description = description;
    }

    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public boolean teamBased() {
        return this == TDM;
    }

    public Kit kit() {
        return switch (this) {
            case GUN_GAME -> Kit.LADDER;
            case ONE_IN_CHAMBER, SNIPER_DUEL -> Kit.FIXED;
            case TDM, FFA, DUEL -> Kit.CLASS;
        };
    }

    /** Les killstreaks n'ont pas de sens dans les modes a equipement impose. */
    public boolean allowsStreaks() {
        return WarfareConfig.streaksEnabled && kit() == Kit.CLASS;
    }

    /** Les atouts suivent la classe : seulement la ou la classe est utilisee. */
    public boolean allowsPerks() {
        return kit() == Kit.CLASS;
    }

    /** Score a atteindre pour gagner. Pour le jeu d'armes, la taille de l'echelle. */
    public int scoreLimit() {
        return switch (this) {
            case TDM -> WarfareConfig.tdmScoreLimit;
            case FFA -> WarfareConfig.killLimit;
            case GUN_GAME -> ladder().size();
            case ONE_IN_CHAMBER -> WarfareConfig.oneChamberScoreLimit;
            case SNIPER_DUEL -> WarfareConfig.sniperDuelScoreLimit;
            case DUEL -> WarfareConfig.duelScoreLimit;
        };
    }

    public int seconds() {
        return switch (this) {
            case TDM -> WarfareConfig.tdmSeconds;
            case FFA -> WarfareConfig.matchSeconds;
            case GUN_GAME -> WarfareConfig.gunGameSeconds;
            case ONE_IN_CHAMBER -> WarfareConfig.oneChamberSeconds;
            case SNIPER_DUEL -> WarfareConfig.sniperDuelSeconds;
            case DUEL -> WarfareConfig.duelSeconds;
        };
    }

    /** Nombre de vies par joueur ; 0 = illimite. */
    public int lives() {
        return this == ONE_IN_CHAMBER ? WarfareConfig.oneChamberLives : 0;
    }

    /** Multiplicateur de degats des balles, pour les modes a elimination en un coup. */
    public double damageMultiplier() {
        return switch (this) {
            case ONE_IN_CHAMBER -> WarfareConfig.oneChamberDamage;
            case SNIPER_DUEL -> WarfareConfig.sniperDuelDamage;
            default -> 1.0;
        };
    }

    /** 0 = pas de limite (la limite du serveur s'applique). */
    public int maxPlayers() {
        return switch (this) {
            case SNIPER_DUEL, DUEL -> 2;
            default -> 0;
        };
    }

    public int minPlayers() {
        return switch (this) {
            case SNIPER_DUEL, DUEL -> Math.min(2, Math.max(1, WarfareConfig.minPlayers));
            default -> Math.max(1, WarfareConfig.minPlayers);
        };
    }

    /** Arme imposee (Kit.FIXED). */
    public String fixedGun() {
        return switch (this) {
            case ONE_IN_CHAMBER -> WarfareConfig.oneChamberGun;
            case SNIPER_DUEL -> WarfareConfig.sniperDuelGun;
            default -> WarfareConfig.primaryGun;
        };
    }

    /** Chargeurs de rechange pour l'arme imposee. 0 pour "une balle chargee". */
    public int fixedMagazines() {
        return this == ONE_IN_CHAMBER ? 0 : WarfareConfig.primaryMagazines;
    }

    /** Munitions chambrees au depart (une seule balle en "Une balle chargee"). */
    public int fixedStartAmmo() {
        return this == ONE_IN_CHAMBER ? 1 : -1;   // -1 = chargeur plein
    }

    /** L'echelle d'armes du jeu d'armes, nettoyee des entrees vides. */
    public static List<String> ladder() {
        List<String> out = new ArrayList<>();
        for (String gunId : WarfareConfig.gunGameLadder) {
            if (gunId != null && !gunId.isBlank() && !out.contains(gunId)) out.add(gunId.trim());
        }
        if (out.isEmpty()) out.add(WarfareConfig.primaryGun);
        return List.copyOf(out);
    }

    public static GameMode byId(String id) {
        for (GameMode mode : values()) {
            if (mode.id.equalsIgnoreCase(id) || mode.name().equalsIgnoreCase(id)) return mode;
        }
        return defaultMode();
    }

    public static GameMode defaultMode() {
        String wanted = WarfareConfig.defaultMode;
        for (GameMode mode : values()) {
            if (mode.id.equalsIgnoreCase(wanted) || mode.name().equalsIgnoreCase(wanted)) return mode;
        }
        return TDM;
    }
}
