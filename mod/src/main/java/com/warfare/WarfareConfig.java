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

    // --- Affichage ---
    private static final ForgeConfigSpec.BooleanValue SHOW_GUN_IN_KILLFEED;
    private static final ForgeConfigSpec.BooleanValue ANNOUNCE_HEADSHOTS;
    private static final ForgeConfigSpec.BooleanValue MANAGE_GAMERULES;

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

        B.comment("Affichage et messages").push("display");
        SHOW_GUN_IN_KILLFEED = B.define("showGunInKillfeed", true);
        ANNOUNCE_HEADSHOTS = B.define("announceHeadshots", true);
        MANAGE_GAMERULES = B.comment("Laisse le mod imposer doImmediateRespawn / keepInventory / showDeathMessages",
                        "Necessaire pour supprimer l'ecran de mort. Mettre a false pour gerer les gamerules a la main.")
                .define("manageGameRules", true);
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
    }

    private WarfareConfig() {}
}
