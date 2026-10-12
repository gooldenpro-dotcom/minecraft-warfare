package com.warfare;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.List;

/**
 * Tous les reglages du mode de jeu, dans config/warfare-common.toml.
 * Les valeurs sont relues dans un cache statique a chaque (re)chargement du fichier.
 */
public final class WarfareConfig {
    private static final ForgeConfigSpec.Builder B = new ForgeConfigSpec.Builder();

    // --- Partie ---
    private static final ForgeConfigSpec.BooleanValue AUTO_START;
    private static final ForgeConfigSpec.IntValue MIN_PLAYERS;
    private static final ForgeConfigSpec.IntValue COUNTDOWN_SECONDS;
    private static final ForgeConfigSpec.IntValue KILL_LIMIT;
    private static final ForgeConfigSpec.IntValue MATCH_SECONDS;
    private static final ForgeConfigSpec.IntValue RESPAWN_DELAY_SECONDS;
    private static final ForgeConfigSpec.IntValue SPAWN_PROTECTION_SECONDS;
    private static final ForgeConfigSpec.IntValue END_SCREEN_SECONDS;

    // --- Equipement par defaut ---
    private static final ForgeConfigSpec.ConfigValue<String> PRIMARY_GUN;
    private static final ForgeConfigSpec.ConfigValue<String> SECONDARY_GUN;
    private static final ForgeConfigSpec.IntValue PRIMARY_MAGAZINES;
    private static final ForgeConfigSpec.IntValue SECONDARY_MAGAZINES;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> EXTRA_ITEMS;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> GUN_WHITELIST;

    // --- Killstreaks ---
    private static final ForgeConfigSpec.BooleanValue STREAKS_ENABLED;
    private static final ForgeConfigSpec.BooleanValue STREAK_KILLS_COUNT;
    private static final ForgeConfigSpec.IntValue DRONE_THRESHOLD;
    private static final ForgeConfigSpec.IntValue DRONE_SECONDS;
    private static final ForgeConfigSpec.IntValue RESUPPLY_THRESHOLD;
    private static final ForgeConfigSpec.IntValue AIRSTRIKE_THRESHOLD;
    private static final ForgeConfigSpec.IntValue AIRSTRIKE_DELAY;
    private static final ForgeConfigSpec.IntValue AIRSTRIKE_BOMBS;
    private static final ForgeConfigSpec.IntValue AIRSTRIKE_SPREAD;
    private static final ForgeConfigSpec.DoubleValue AIRSTRIKE_POWER;
    private static final ForgeConfigSpec.IntValue TURRET_THRESHOLD;
    private static final ForgeConfigSpec.IntValue TURRET_SECONDS;
    private static final ForgeConfigSpec.IntValue TURRET_RANGE;
    private static final ForgeConfigSpec.IntValue TURRET_INTERVAL;
    private static final ForgeConfigSpec.DoubleValue TURRET_DAMAGE;
    private static final ForgeConfigSpec.IntValue CARPET_THRESHOLD;
    private static final ForgeConfigSpec.IntValue CARPET_SECONDS;
    private static final ForgeConfigSpec.IntValue CARPET_BOMBS;
    private static final ForgeConfigSpec.IntValue CARPET_MARGIN;
    private static final ForgeConfigSpec.DoubleValue CARPET_POWER;

    // --- Modes de jeu ---
    private static final ForgeConfigSpec.ConfigValue<String> DEFAULT_MODE;
    private static final ForgeConfigSpec.BooleanValue PLAYERS_CAN_START;
    private static final ForgeConfigSpec.BooleanValue FRIENDLY_FIRE;
    private static final ForgeConfigSpec.IntValue TDM_SCORE;
    private static final ForgeConfigSpec.IntValue TDM_SECONDS;
    private static final ForgeConfigSpec.IntValue GUN_GAME_SECONDS;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> GUN_GAME_LADDER;
    private static final ForgeConfigSpec.IntValue ONE_CHAMBER_SCORE;
    private static final ForgeConfigSpec.IntValue ONE_CHAMBER_SECONDS;
    private static final ForgeConfigSpec.IntValue ONE_CHAMBER_LIVES;
    private static final ForgeConfigSpec.ConfigValue<String> ONE_CHAMBER_GUN;
    private static final ForgeConfigSpec.DoubleValue ONE_CHAMBER_DAMAGE;
    private static final ForgeConfigSpec.IntValue SNIPER_DUEL_SCORE;
    private static final ForgeConfigSpec.IntValue SNIPER_DUEL_SECONDS;
    private static final ForgeConfigSpec.ConfigValue<String> SNIPER_DUEL_GUN;
    private static final ForgeConfigSpec.DoubleValue SNIPER_DUEL_DAMAGE;
    private static final ForgeConfigSpec.IntValue DUEL_SCORE;
    private static final ForgeConfigSpec.IntValue DUEL_SECONDS;

    // --- Progression ---
    private static final ForgeConfigSpec.BooleanValue PROGRESSION_ENABLED;
    private static final ForgeConfigSpec.DoubleValue XP_MULTIPLIER;
    private static final ForgeConfigSpec.IntValue XP_KILL;
    private static final ForgeConfigSpec.IntValue XP_HEADSHOT;
    private static final ForgeConfigSpec.IntValue XP_STREAK;
    private static final ForgeConfigSpec.IntValue XP_WIN;
    private static final ForgeConfigSpec.IntValue XP_LOSS;
    private static final ForgeConfigSpec.IntValue XP_MATCH;
    private static final ForgeConfigSpec.BooleanValue PRESTIGE_RESETS_UNLOCKS;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> UNLOCK_OVERRIDES;

    // --- Monde ---
    private static final ForgeConfigSpec.BooleanValue DISABLE_MOBS;
    private static final ForgeConfigSpec.IntValue FIXED_DAYTIME;
    private static final ForgeConfigSpec.BooleanValue DISABLE_WEATHER;

    // --- Affichage ---
    private static final ForgeConfigSpec.BooleanValue SHOW_GUN_IN_KILLFEED;
    private static final ForgeConfigSpec.BooleanValue ANNOUNCE_HEADSHOTS;
    private static final ForgeConfigSpec.BooleanValue MANAGE_GAMERULES;
    private static final ForgeConfigSpec.BooleanValue HUD_ENABLED;
    private static final ForgeConfigSpec.BooleanValue SCORE_POPUPS;

    public static final ForgeConfigSpec SPEC;

    static {
        B.comment("Reglages de la partie Free For All").push("match");
        AUTO_START = B.comment("Lancer la partie automatiquement des que minPlayers est atteint.",
                        "false = la partie se lance depuis le menu d'accueil ou avec /warfare start.")
                .define("autoStart", false);
        MIN_PLAYERS = B.comment("Nombre de joueurs necessaires pour lancer une partie",
                        "Sert de seuil au demarrage automatique, et empeche le compte a rebours",
                        "de continuer s'il ne reste plus assez de monde.")
                .defineInRange("minPlayers", 2, 1, 100);
        COUNTDOWN_SECONDS = B.comment("Duree du compte a rebours avant le debut de la partie, en secondes")
                .defineInRange("countdownSeconds", 30, 3, 300);
        KILL_LIMIT = B.comment("Nombre d'eliminations qui met fin a la partie")
                .defineInRange("killLimit", 30, 1, 1000);
        MATCH_SECONDS = B.comment("Duree maximale d'une partie, en secondes (600 = 10 minutes)")
                .defineInRange("matchSeconds", 600, 30, 7200);
        RESPAWN_DELAY_SECONDS = B.comment("Delai avant reapparition apres une mort, en secondes")
                .defineInRange("respawnDelaySeconds", 3, 0, 30);
        SPAWN_PROTECTION_SECONDS = B.comment("Duree d'invincibilite a la reapparition, en secondes")
                .defineInRange("spawnProtectionSeconds", 2, 0, 30);
        END_SCREEN_SECONDS = B.comment("Duree de l'ecran de fin avant le retour au lobby, en secondes")
                .defineInRange("endScreenSeconds", 10, 3, 120);
        B.pop();

        B.comment("Equipement donne a chaque reapparition").push("loadout");
        PRIMARY_GUN = B.comment("Identifiant TaCZ de l'arme principale (ex: tacz:ak47, tacz:m4a1, tacz:hk416d)")
                .define("primaryGun", "tacz:ak47");
        SECONDARY_GUN = B.comment("Identifiant TaCZ de l'arme secondaire (ex: tacz:glock_17, tacz:deagle)")
                .define("secondaryGun", "tacz:glock_17");
        PRIMARY_MAGAZINES = B.comment("Chargeurs de rechange pour l'arme principale (l'arme est deja chargee en plus)")
                .defineInRange("primaryMagazines", 5, 0, 64);
        SECONDARY_MAGAZINES = B.comment("Chargeurs de rechange pour l'arme secondaire")
                .defineInRange("secondaryMagazines", 4, 0, 64);
        EXTRA_ITEMS = B.comment("Items supplementaires, au format \"identifiant quantite\" (ex: \"minecraft:golden_apple 1\")")
                .defineListAllowEmpty(List.of("extraItems"), List::of, o -> o instanceof String);
        GUN_WHITELIST = B.comment("Armes selectionnables dans le menu \"Creer une classe\".",
                        "Liste vide = toutes les armes TaCZ chargees.",
                        "Exemple : [\"tacz:ak47\", \"tacz:m4a1\", \"tacz:glock_17\"]")
                .defineListAllowEmpty(List.of("gunWhitelist"), List::of, o -> o instanceof String);
        B.pop();

        B.comment("Series d'eliminations (killstreaks).",
                "Chaque joueur en choisit 3 dans le menu de classe.",
                "Aucune explosion du mod ne casse de blocs.").push("killstreaks");
        STREAKS_ENABLED = B.define("enabled", true);
        STREAK_KILLS_COUNT = B.comment("Les eliminations faites AVEC une killstreak comptent-elles dans la serie ?",
                        "false = comportement attendu : seules les eliminations a l'arme font monter la serie.")
                .define("streakKillsCountTowardStreak", false);

        DRONE_THRESHOLD = B.comment("Drone de reperage : ennemis en surbrillance").defineInRange("droneThreshold", 3, 1, 100);
        DRONE_SECONDS = B.defineInRange("droneSeconds", 15, 1, 300);

        RESUPPLY_THRESHOLD = B.comment("Ravitaillement : munitions pleines + bonus aleatoire")
                .defineInRange("resupplyThreshold", 4, 1, 100);

        AIRSTRIKE_THRESHOLD = B.comment("Frappe aerienne : salve d'explosions sur un point designe")
                .defineInRange("airstrikeThreshold", 5, 1, 100);
        AIRSTRIKE_DELAY = B.comment("Delai entre la designation et l'impact, en secondes")
                .defineInRange("airstrikeDelaySeconds", 5, 1, 60);
        AIRSTRIKE_BOMBS = B.comment("Nombre d'explosions dans la salve").defineInRange("airstrikeBombs", 9, 1, 100);
        AIRSTRIKE_SPREAD = B.comment("Rayon de dispersion de la salve, en blocs").defineInRange("airstrikeSpread", 8, 1, 64);
        AIRSTRIKE_POWER = B.comment("Puissance de chaque explosion (4.0 = TNT)").defineInRange("airstrikePower", 3.0, 0.5, 20.0);

        TURRET_THRESHOLD = B.comment("Tourelle sentinelle : tire sur les ennemis proches")
                .defineInRange("turretThreshold", 7, 1, 100);
        TURRET_SECONDS = B.defineInRange("turretSeconds", 30, 1, 300);
        TURRET_RANGE = B.comment("Portee de detection, en blocs").defineInRange("turretRange", 20, 1, 128);
        TURRET_INTERVAL = B.comment("Intervalle entre deux tirs, en ticks (20 ticks = 1 s)")
                .defineInRange("turretIntervalTicks", 15, 5, 200);
        TURRET_DAMAGE = B.comment("Degats par tir").defineInRange("turretDamage", 4.0, 0.5, 40.0);

        CARPET_THRESHOLD = B.comment("Bombardement : frappes sur toute l'arene")
                .defineInRange("carpetThreshold", 11, 1, 100);
        CARPET_SECONDS = B.defineInRange("carpetSeconds", 10, 1, 120);
        CARPET_BOMBS = B.comment("Nombre total d'explosions etalees sur la duree")
                .defineInRange("carpetBombs", 30, 1, 400);
        CARPET_MARGIN = B.comment("Marge ajoutee autour de la zone des points d'apparition, en blocs")
                .defineInRange("carpetMargin", 20, 0, 200);
        CARPET_POWER = B.comment("Puissance de chaque explosion").defineInRange("carpetPower", 3.0, 0.5, 20.0);
        B.pop();

        B.comment("Modes de jeu multijoueur.",
                "Identifiants : tdm, ffa, jeu_d_armes, une_balle, 1v1_sniper, 1v1").push("modes");
        DEFAULT_MODE = B.comment("Mode selectionne au premier demarrage").define("default", "tdm");
        PLAYERS_CAN_START = B.comment("Permettre a n'importe quel joueur de lancer la partie depuis le menu.",
                        "false = reserve aux operateurs.")
                .define("playersCanStart", true);
        FRIENDLY_FIRE = B.comment("Tir allie dans les modes par equipe").define("friendlyFire", false);

        TDM_SCORE = B.comment("Match a mort par equipe : score a atteindre")
                .defineInRange("tdmScoreLimit", 75, 1, 1000);
        TDM_SECONDS = B.defineInRange("tdmSeconds", 600, 30, 7200);

        GUN_GAME_SECONDS = B.comment("Jeu d'armes : duree maximale")
                .defineInRange("gunGameSeconds", 900, 30, 7200);
        GUN_GAME_LADDER = B.comment("Jeu d'armes : l'echelle d'armes, dans l'ordre.",
                        "Une elimination = l'arme suivante. Terminer l'echelle gagne la partie.")
                .defineListAllowEmpty(List.of("gunGameLadder"), () -> List.of(
                        "tacz:glock_17", "tacz:m1911", "tacz:deagle", "tacz:uzi", "tacz:hk_mp5a5",
                        "tacz:p90", "tacz:ump45", "tacz:vector45", "tacz:ak47", "tacz:m4a1",
                        "tacz:scar_l", "tacz:aug", "tacz:m870", "tacz:spas_12", "tacz:rpk",
                        "tacz:m700", "tacz:ai_awp"), o -> o instanceof String);

        ONE_CHAMBER_SCORE = B.comment("Une balle chargee : score a atteindre")
                .defineInRange("oneChamberScoreLimit", 20, 1, 1000);
        ONE_CHAMBER_SECONDS = B.defineInRange("oneChamberSeconds", 600, 30, 7200);
        ONE_CHAMBER_LIVES = B.comment("Vies par joueur ; 0 = illimite")
                .defineInRange("oneChamberLives", 0, 0, 50);
        ONE_CHAMBER_GUN = B.comment("Arme imposee").define("oneChamberGun", "tacz:deagle");
        ONE_CHAMBER_DAMAGE = B.comment("Multiplicateur de degats (eleve = elimination en un coup)")
                .defineInRange("oneChamberDamage", 10.0, 1.0, 50.0);

        SNIPER_DUEL_SCORE = B.comment("1VS1 Sniper : score a atteindre")
                .defineInRange("sniperDuelScoreLimit", 10, 1, 1000);
        SNIPER_DUEL_SECONDS = B.defineInRange("sniperDuelSeconds", 300, 30, 7200);
        SNIPER_DUEL_GUN = B.define("sniperDuelGun", "tacz:m700");
        SNIPER_DUEL_DAMAGE = B.comment("Multiplicateur de degats du sniper")
                .defineInRange("sniperDuelDamage", 2.5, 1.0, 50.0);

        DUEL_SCORE = B.comment("1VS1 : score a atteindre").defineInRange("duelScoreLimit", 10, 1, 1000);
        DUEL_SECONDS = B.defineInRange("duelSeconds", 300, 30, 7200);
        B.pop();

        B.comment("Progression : XP, niveaux (1-70), prestiges (0-10) et deblocages.").push("progression");
        PROGRESSION_ENABLED = B.comment("false = tout est debloque pour tout le monde")
                .define("enabled", true);
        XP_MULTIPLIER = B.comment("Multiplie tous les gains d'XP. Monter pour progresser plus vite.")
                .defineInRange("xpMultiplier", 1.0, 0.1, 100.0);
        XP_KILL = B.defineInRange("xpPerKill", 100, 0, 10000);
        XP_HEADSHOT = B.comment("Bonus ajoute pour un tir a la tete")
                .defineInRange("xpHeadshotBonus", 50, 0, 10000);
        XP_STREAK = B.comment("Bonus a l'obtention d'une killstreak")
                .defineInRange("xpPerStreak", 75, 0, 10000);
        XP_WIN = B.defineInRange("xpPerWin", 500, 0, 100000);
        XP_LOSS = B.defineInRange("xpPerLoss", 150, 0, 100000);
        XP_MATCH = B.comment("XP de participation, versee a la fin de chaque partie")
                .defineInRange("xpPerMatch", 100, 0, 100000);
        PRESTIGE_RESETS_UNLOCKS = B.comment("true = le prestige reverrouille les armes (comme Call of Duty).",
                        "false (defaut) = tout reste debloque apres un prestige.")
                .define("prestigeResetsUnlocks", false);
        UNLOCK_OVERRIDES = B.comment("Forcer le niveau requis d'une arme, d'un atout ou d'une killstreak.",
                        "Format \"identifiant=niveau\", ex : [\"tacz:ak47=1\", \"frappe_aerienne=5\"]")
                .defineListAllowEmpty(List.of("unlockOverrides"), List::of, o -> o instanceof String);
        B.pop();

        B.comment("Reglages du monde pendant les parties").push("world");
        DISABLE_MOBS = B.comment("Desactive l'apparition des monstres et supprime ceux qui restent")
                .define("disableMobs", true);
        FIXED_DAYTIME = B.comment("Heure fixe du monde en ticks (6000 = midi) ; -1 = cycle normal")
                .defineInRange("fixedDaytime", 6000, -1, 24000);
        DISABLE_WEATHER = B.comment("Coupe la pluie et les orages").define("disableWeather", true);
        B.pop();

        B.comment("Affichage et messages").push("display");
        SHOW_GUN_IN_KILLFEED = B.define("showGunInKillfeed", true);
        ANNOUNCE_HEADSHOTS = B.define("announceHeadshots", true);
        MANAGE_GAMERULES = B.comment("Laisse le mod imposer doImmediateRespawn / keepInventory / showDeathMessages",
                        "Necessaire pour supprimer l'ecran de mort. Mettre a false pour gerer les gamerules a la main.")
                .define("manageGameRules", true);
        HUD_ENABLED = B.comment("ATH du mod : score, serie, chrono, barre d'XP")
                .define("hudEnabled", true);
        SCORE_POPUPS = B.comment("Points qui montent a l'ecran a chaque elimination (+100)")
                .define("scorePopups", true);
        B.pop();

        SPEC = B.build();
    }

    // --- Cache relu a chaque chargement ---
    public static boolean autoStart = false;
    public static int minPlayers = 2;
    public static int countdownSeconds = 30;
    public static int killLimit = 30;
    public static int matchSeconds = 600;
    public static int respawnDelaySeconds = 3;
    public static int spawnProtectionSeconds = 2;
    public static int endScreenSeconds = 10;
    public static String primaryGun = "tacz:ak47";
    public static String secondaryGun = "tacz:glock_17";
    public static int primaryMagazines = 5;
    public static int secondaryMagazines = 4;
    public static List<? extends String> extraItems = List.of();
    public static List<? extends String> gunWhitelist = List.of();
    public static boolean streaksEnabled = true;
    public static boolean streakKillsCount = false;
    public static int droneThreshold = 3;
    public static int droneSeconds = 15;
    public static int resupplyThreshold = 4;
    public static int airstrikeThreshold = 5;
    public static int airstrikeDelay = 5;
    public static int airstrikeBombs = 9;
    public static int airstrikeSpread = 8;
    public static double airstrikePower = 3.0;
    public static int turretThreshold = 7;
    public static int turretSeconds = 30;
    public static int turretRange = 20;
    public static int turretInterval = 15;
    public static double turretDamage = 4.0;
    public static int carpetThreshold = 11;
    public static int carpetSeconds = 10;
    public static int carpetBombs = 30;
    public static int carpetMargin = 20;
    public static double carpetPower = 3.0;
    public static boolean showGunInKillfeed = true;
    public static boolean announceHeadshots = true;
    public static boolean manageGameRules = true;
    public static boolean hudEnabled = true;
    public static boolean scorePopups = true;
    public static String defaultMode = "tdm";
    public static boolean playersCanStart = true;
    public static boolean friendlyFire = false;
    public static int tdmScoreLimit = 75;
    public static int tdmSeconds = 600;
    public static int gunGameSeconds = 900;
    public static List<? extends String> gunGameLadder = List.of();
    public static int oneChamberScoreLimit = 20;
    public static int oneChamberSeconds = 600;
    public static int oneChamberLives = 0;
    public static String oneChamberGun = "tacz:deagle";
    public static double oneChamberDamage = 10.0;
    public static int sniperDuelScoreLimit = 10;
    public static int sniperDuelSeconds = 300;
    public static String sniperDuelGun = "tacz:m700";
    public static double sniperDuelDamage = 2.5;
    public static int duelScoreLimit = 10;
    public static int duelSeconds = 300;
    public static boolean progressionEnabled = true;
    public static double xpMultiplier = 1.0;
    public static int xpPerKill = 100;
    public static int xpHeadshotBonus = 50;
    public static int xpPerStreak = 75;
    public static int xpPerWin = 500;
    public static int xpPerLoss = 150;
    public static int xpPerMatch = 100;
    public static boolean prestigeResetsUnlocks = false;
    public static List<? extends String> unlockOverrides = List.of();
    public static boolean disableMobs = true;
    public static int fixedDaytime = 6000;
    public static boolean disableWeather = true;

    @SubscribeEvent
    public static void onLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SPEC) reload();
    }

    @SubscribeEvent
    public static void onReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == SPEC) reload();
    }

    /** Recharge le cache depuis la spec. Appele par Forge et par /warfare reload. */
    public static void reload() {
        autoStart = AUTO_START.get();
        minPlayers = MIN_PLAYERS.get();
        countdownSeconds = COUNTDOWN_SECONDS.get();
        killLimit = KILL_LIMIT.get();
        matchSeconds = MATCH_SECONDS.get();
        respawnDelaySeconds = RESPAWN_DELAY_SECONDS.get();
        spawnProtectionSeconds = SPAWN_PROTECTION_SECONDS.get();
        endScreenSeconds = END_SCREEN_SECONDS.get();
        primaryGun = PRIMARY_GUN.get();
        secondaryGun = SECONDARY_GUN.get();
        primaryMagazines = PRIMARY_MAGAZINES.get();
        secondaryMagazines = SECONDARY_MAGAZINES.get();
        extraItems = EXTRA_ITEMS.get();
        gunWhitelist = GUN_WHITELIST.get();
        streaksEnabled = STREAKS_ENABLED.get();
        streakKillsCount = STREAK_KILLS_COUNT.get();
        droneThreshold = DRONE_THRESHOLD.get();
        droneSeconds = DRONE_SECONDS.get();
        resupplyThreshold = RESUPPLY_THRESHOLD.get();
        airstrikeThreshold = AIRSTRIKE_THRESHOLD.get();
        airstrikeDelay = AIRSTRIKE_DELAY.get();
        airstrikeBombs = AIRSTRIKE_BOMBS.get();
        airstrikeSpread = AIRSTRIKE_SPREAD.get();
        airstrikePower = AIRSTRIKE_POWER.get();
        turretThreshold = TURRET_THRESHOLD.get();
        turretSeconds = TURRET_SECONDS.get();
        turretRange = TURRET_RANGE.get();
        turretInterval = TURRET_INTERVAL.get();
        turretDamage = TURRET_DAMAGE.get();
        carpetThreshold = CARPET_THRESHOLD.get();
        carpetSeconds = CARPET_SECONDS.get();
        carpetBombs = CARPET_BOMBS.get();
        carpetMargin = CARPET_MARGIN.get();
        carpetPower = CARPET_POWER.get();
        showGunInKillfeed = SHOW_GUN_IN_KILLFEED.get();
        announceHeadshots = ANNOUNCE_HEADSHOTS.get();
        manageGameRules = MANAGE_GAMERULES.get();
        hudEnabled = HUD_ENABLED.get();
        scorePopups = SCORE_POPUPS.get();
        defaultMode = DEFAULT_MODE.get();
        playersCanStart = PLAYERS_CAN_START.get();
        friendlyFire = FRIENDLY_FIRE.get();
        tdmScoreLimit = TDM_SCORE.get();
        tdmSeconds = TDM_SECONDS.get();
        gunGameSeconds = GUN_GAME_SECONDS.get();
        gunGameLadder = GUN_GAME_LADDER.get();
        oneChamberScoreLimit = ONE_CHAMBER_SCORE.get();
        oneChamberSeconds = ONE_CHAMBER_SECONDS.get();
        oneChamberLives = ONE_CHAMBER_LIVES.get();
        oneChamberGun = ONE_CHAMBER_GUN.get();
        oneChamberDamage = ONE_CHAMBER_DAMAGE.get();
        sniperDuelScoreLimit = SNIPER_DUEL_SCORE.get();
        sniperDuelSeconds = SNIPER_DUEL_SECONDS.get();
        sniperDuelGun = SNIPER_DUEL_GUN.get();
        sniperDuelDamage = SNIPER_DUEL_DAMAGE.get();
        duelScoreLimit = DUEL_SCORE.get();
        duelSeconds = DUEL_SECONDS.get();
        progressionEnabled = PROGRESSION_ENABLED.get();
        xpMultiplier = XP_MULTIPLIER.get();
        xpPerKill = XP_KILL.get();
        xpHeadshotBonus = XP_HEADSHOT.get();
        xpPerStreak = XP_STREAK.get();
        xpPerWin = XP_WIN.get();
        xpPerLoss = XP_LOSS.get();
        xpPerMatch = XP_MATCH.get();
        prestigeResetsUnlocks = PRESTIGE_RESETS_UNLOCKS.get();
        unlockOverrides = UNLOCK_OVERRIDES.get();
        disableMobs = DISABLE_MOBS.get();
        fixedDaytime = FIXED_DAYTIME.get();
        disableWeather = DISABLE_WEATHER.get();
    }

    private WarfareConfig() {}
}
