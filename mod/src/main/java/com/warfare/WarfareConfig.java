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

    // --- Affichage ---
    private static final ForgeConfigSpec.BooleanValue SHOW_GUN_IN_KILLFEED;
    private static final ForgeConfigSpec.BooleanValue ANNOUNCE_HEADSHOTS;
    private static final ForgeConfigSpec.BooleanValue MANAGE_GAMERULES;

    public static final ForgeConfigSpec SPEC;

    static {
        B.comment("Reglages de la partie Free For All").push("match");
        MIN_PLAYERS = B.comment("Nombre de joueurs connectes a partir duquel le compte a rebours demarre")
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
        showGunInKillfeed = SHOW_GUN_IN_KILLFEED.get();
        announceHeadshots = ANNOUNCE_HEADSHOTS.get();
        manageGameRules = MANAGE_GAMERULES.get();
    }

    private WarfareConfig() {}
}
