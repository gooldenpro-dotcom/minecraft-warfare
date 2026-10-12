package com.warfare.progression;

/**
 * Courbe de niveaux et grades. 70 niveaux, 10 prestiges.
 *
 * La courbe est fixe ; c'est la quantite d'XP gagnee en jeu qui se regle
 * (xpMultiplier dans la config), pour que les niveaux deja atteints gardent leur sens.
 */
public final class Rank {
    public static final int MAX_LEVEL = 70;
    public static final int MAX_PRESTIGE = 10;

    private static final long BASE = 400;
    private static final long STEP = 120;

    /** XP necessaire pour passer de {@code level} a {@code level + 1}. */
    public static long xpToNext(int level) {
        if (level >= MAX_LEVEL) return 0;
        int clamped = Math.max(1, level);
        return BASE + STEP * (clamped - 1);
    }

    /** XP cumulee necessaire pour atteindre {@code level}. */
    public static long totalFor(int level) {
        long total = 0;
        for (int i = 1; i < Math.min(level, MAX_LEVEL); i++) total += xpToNext(i);
        return total;
    }

    /** Niveau correspondant a une XP cumulee. */
    public static int levelFor(long xp) {
        int level = 1;
        long left = Math.max(0, xp);
        while (level < MAX_LEVEL && left >= xpToNext(level)) {
            left -= xpToNext(level);
            level++;
        }
        return level;
    }

    /** XP acquise a l'interieur du niveau courant. */
    public static long intoLevel(long xp, int level) {
        return Math.max(0, xp - totalFor(level));
    }

    /**
     * Grades, du plus bas au plus haut. Hierarchie militaire francophone generique :
     * rien n'est repris d'un jeu existant.
     */
    private static final String[][] TITLES = {
            {"1", "Recrue"},
            {"4", "Soldat"},
            {"8", "Soldat de 1re classe"},
            {"12", "Caporal"},
            {"16", "Caporal-chef"},
            {"20", "Sergent"},
            {"25", "Sergent-chef"},
            {"30", "Adjudant"},
            {"35", "Adjudant-chef"},
            {"40", "Major"},
            {"45", "Sous-lieutenant"},
            {"50", "Lieutenant"},
            {"55", "Capitaine"},
            {"60", "Commandant"},
            {"64", "Lieutenant-colonel"},
            {"67", "Colonel"},
            {"70", "General"},
    };

    public static String title(int level) {
        String found = TITLES[0][1];
        for (String[] row : TITLES) {
            if (level >= Integer.parseInt(row[0])) found = row[1];
        }
        return found;
    }

    /** Etiquette complete : grade, niveau et prestige. */
    public static String label(int level, int prestige) {
        String base = title(level) + " " + level;
        return prestige > 0 ? base + " - Prestige " + prestige : base;
    }

    public static boolean canPrestige(int level, int prestige) {
        return level >= MAX_LEVEL && prestige < MAX_PRESTIGE;
    }

    private Rank() {}
}
