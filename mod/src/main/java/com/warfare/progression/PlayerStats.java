package com.warfare.progression;

import com.google.gson.JsonObject;

/**
 * Les performances et la progression d'un joueur, conservees d'une partie a l'autre.
 * Un objet par joueur, ecrit dans world/warfare/stats/&lt;uuid&gt;.json.
 */
public final class PlayerStats {
    /** Dernier pseudo connu : permet d'afficher le classement meme hors ligne. */
    public String name = "";

    public long xp;
    public int level = 1;
    public int prestige;

    public int kills;
    public int deaths;
    public int headshots;
    public int wins;
    public int losses;
    public int matches;
    public int bestStreak;
    public int bestKillsInMatch;
    public long score;
    public long secondsPlayed;

    public double kd() {
        return deaths == 0 ? kills : (double) kills / deaths;
    }

    /** Ajoute de l'XP et met le niveau a jour. @return le nombre de niveaux gagnes. */
    public int addXp(long amount) {
        if (amount <= 0) return 0;
        int before = level;
        xp += amount;
        score += amount;
        level = Rank.levelFor(xp);
        return level - before;
    }

    /** Passage au prestige : niveau remis a 1, performances conservees. */
    public boolean prestige() {
        if (!Rank.canPrestige(level, prestige)) return false;
        prestige++;
        level = 1;
        xp = 0;
        return true;
    }

    /** Niveau qui sert aux deblocages (voir Unlocks). */
    public int unlockLevel() {
        return level;
    }

    public JsonObject toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("name", name);
        object.addProperty("xp", xp);
        object.addProperty("level", level);
        object.addProperty("prestige", prestige);
        object.addProperty("kills", kills);
        object.addProperty("deaths", deaths);
        object.addProperty("headshots", headshots);
        object.addProperty("wins", wins);
        object.addProperty("losses", losses);
        object.addProperty("matches", matches);
        object.addProperty("bestStreak", bestStreak);
        object.addProperty("bestKillsInMatch", bestKillsInMatch);
        object.addProperty("score", score);
        object.addProperty("secondsPlayed", secondsPlayed);
        return object;
    }

    public static PlayerStats fromJson(JsonObject object) {
        PlayerStats stats = new PlayerStats();
        stats.name = string(object, "name");
        stats.xp = number(object, "xp");
        stats.prestige = (int) number(object, "prestige");
        stats.kills = (int) number(object, "kills");
        stats.deaths = (int) number(object, "deaths");
        stats.headshots = (int) number(object, "headshots");
        stats.wins = (int) number(object, "wins");
        stats.losses = (int) number(object, "losses");
        stats.matches = (int) number(object, "matches");
        stats.bestStreak = (int) number(object, "bestStreak");
        stats.bestKillsInMatch = (int) number(object, "bestKillsInMatch");
        stats.score = number(object, "score");
        stats.secondsPlayed = number(object, "secondsPlayed");
        // Le niveau se recalcule depuis l'XP : une seule source de verite.
        stats.level = Rank.levelFor(stats.xp);
        return stats;
    }

    private static long number(JsonObject object, String key) {
        try {
            return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsLong() : 0;
        } catch (RuntimeException e) {
            return 0;
        }
    }

    private static String string(JsonObject object, String key) {
        try {
            return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsString() : "";
        } catch (RuntimeException e) {
            return "";
        }
    }
}
