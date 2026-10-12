package com.warfare.progression;

import com.warfare.WarfareConfig;
import com.warfare.loadout.GunCatalog;
import com.warfare.loadout.Perk;
import com.warfare.streak.Killstreak;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Niveau requis pour chaque arme, atout et killstreak.
 *
 * Par defaut les niveaux sont deduits du catalogue TaCZ charge : les armes d'un meme
 * type se debloquent les unes apres les autres. La config peut forcer n'importe quelle
 * entree ([progression] unlockOverrides, format "tacz:ak47=1").
 */
public final class Unlocks {

    /** Niveau de depart par type d'arme. */
    private static int base(String type) {
        return switch (type) {
            case "pistol" -> 1;
            case "rifle" -> 1;
            case "smg" -> 2;
            case "shotgun" -> 5;
            case "sniper" -> 8;
            case "mg" -> 12;
            case "rpg" -> 30;
            default -> 10;
        };
    }

    private static int step(String type) {
        return switch (type) {
            case "pistol" -> 2;
            case "rpg" -> 10;
            default -> 3;
        };
    }

    // --- cache : le catalogue ne change qu'au rechargement des packs TaCZ ---
    private static Map<String, Integer> gunLevels = Map.of();
    private static int cachedSize = -1;
    private static int cachedOverrides = -1;

    private static Map<String, Integer> levels() {
        List<GunCatalog.Entry> catalog = GunCatalog.all();
        if (catalog.size() == cachedSize && WarfareConfig.unlockOverrides.size() == cachedOverrides) {
            return gunLevels;
        }
        Map<String, Integer> overrides = overrides();
        Map<String, Integer> out = new HashMap<>();
        Map<String, Integer> seenPerType = new HashMap<>();
        for (GunCatalog.Entry entry : catalog) {
            int index = seenPerType.merge(entry.type(), 1, Integer::sum) - 1;
            Integer forced = overrides.get(entry.gunId());
            int level = forced != null ? forced : base(entry.type()) + index * step(entry.type());
            out.put(entry.gunId(), clamp(level));
        }
        // Les surcharges qui ne correspondent a aucune arme chargee restent utilisables.
        for (Map.Entry<String, Integer> entry : overrides.entrySet()) {
            out.putIfAbsent(entry.getKey(), clamp(entry.getValue()));
        }
        gunLevels = Map.copyOf(out);
        cachedSize = catalog.size();
        cachedOverrides = WarfareConfig.unlockOverrides.size();
        return gunLevels;
    }

    private static Map<String, Integer> overrides() {
        Map<String, Integer> out = new HashMap<>();
        for (String raw : WarfareConfig.unlockOverrides) {
            if (raw == null) continue;
            int split = raw.lastIndexOf('=');
            if (split <= 0) continue;
            try {
                out.put(raw.substring(0, split).trim(),
                        Integer.parseInt(raw.substring(split + 1).trim()));
            } catch (NumberFormatException ignored) {
                // entree mal ecrite : ignoree
            }
        }
        return out;
    }

    public static int gunLevel(String gunId) {
        if (!WarfareConfig.progressionEnabled) return 1;
        Integer level = levels().get(gunId);
        return level == null ? 1 : level;
    }

    public static int perkLevel(Perk perk) {
        if (!WarfareConfig.progressionEnabled) return 1;
        Integer forced = overrides().get(perk.id());
        if (forced != null) return clamp(forced);
        return switch (perk) {
            case SPRINTER -> 1;
            case STOPPING_POWER -> 4;
            case QUICK_AIM -> 8;
            case FLAK -> 12;
            case GHOST -> 16;
            case RAPID_FIRE -> 22;
        };
    }

    public static int streakLevel(Killstreak streak) {
        if (!WarfareConfig.progressionEnabled) return 1;
        Integer forced = overrides().get(streak.id());
        if (forced != null) return clamp(forced);
        return switch (streak) {
            case DRONE -> 1;
            case RESUPPLY -> 6;
            case AIRSTRIKE -> 10;
            case TURRET -> 18;
            case CARPET -> 26;
        };
    }

    /**
     * Niveau a comparer aux deblocages. Apres un prestige, tout reste debloque
     * sauf si la config demande explicitement une remise a zero.
     */
    public static int effectiveLevel(PlayerStats stats) {
        if (!WarfareConfig.progressionEnabled) return Rank.MAX_LEVEL;
        if (stats.prestige > 0 && !WarfareConfig.prestigeResetsUnlocks) return Rank.MAX_LEVEL;
        return stats.level;
    }

    public static boolean gunUnlocked(String gunId, int level) {
        return !WarfareConfig.progressionEnabled || level >= gunLevel(gunId);
    }

    public static boolean perkUnlocked(Perk perk, int level) {
        return !WarfareConfig.progressionEnabled || level >= perkLevel(perk);
    }

    public static boolean streakUnlocked(Killstreak streak, int level) {
        return !WarfareConfig.progressionEnabled || level >= streakLevel(streak);
    }

    /** Premiere arme utilisable a ce niveau, pour remplacer un choix verrouille. */
    public static String firstUnlockedGun(int level, String fallback) {
        if (gunUnlocked(fallback, level)) return fallback;
        for (GunCatalog.Entry entry : GunCatalog.all()) {
            if (gunUnlocked(entry.gunId(), level)) return entry.gunId();
        }
        return fallback;
    }

    private static int clamp(int level) {
        return Math.max(1, Math.min(Rank.MAX_LEVEL, level));
    }

    private Unlocks() {}
}
