package com.warfare.loadout;

import com.warfare.WarfareConfig;
import com.warfare.streak.Killstreak;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

/**
 * Une classe personnalisee : un nom, deux armes, un atout par categorie et 3 killstreaks.
 * Immuable ; toujours validee cote serveur avant d'etre enregistree.
 *
 * La liste de killstreaks peut etre incomplete pendant l'edition dans le menu ;
 * streaksNormalized() ramene toujours a exactement {@link Killstreak#EQUIPPED} entrees.
 */
public record ClassLoadout(String name, String primaryGun, String secondaryGun,
                           List<Perk> perks, List<Killstreak> streaks) {
    public static final int MAX_NAME_LENGTH = 16;

    public static ClassLoadout defaultFor(int slot) {
        return new ClassLoadout("Classe " + (slot + 1), WarfareConfig.primaryGun, WarfareConfig.secondaryGun,
                List.of(Perk.defaultOf(1), Perk.defaultOf(2), Perk.defaultOf(3)),
                Killstreak.defaults());
    }

    /** L'atout choisi dans une categorie (1, 2 ou 3). */
    public Perk perk(int category) {
        for (Perk p : perks) {
            if (p.category() == category) return p;
        }
        return Perk.defaultOf(category);
    }

    public boolean has(Perk perk) {
        return perks.contains(perk);
    }

    public boolean has(Killstreak streak) {
        return streaks.contains(streak);
    }

    /** Exactement 3 killstreaks : les choix du joueur, completes par les seuils les plus bas. */
    public List<Killstreak> streaksNormalized() {
        List<Killstreak> out = new ArrayList<>();
        for (Killstreak streak : streaks) {
            if (out.size() < Killstreak.EQUIPPED && !out.contains(streak)) out.add(streak);
        }
        for (Killstreak streak : Killstreak.sorted()) {
            if (out.size() >= Killstreak.EQUIPPED) break;
            if (!out.contains(streak)) out.add(streak);
        }
        return List.copyOf(out);
    }

    public ClassLoadout withPerk(Perk perk) {
        List<Perk> next = new ArrayList<>();
        for (int category = 1; category <= Perk.CATEGORIES; category++) {
            next.add(perk.category() == category ? perk : perk(category));
        }
        return new ClassLoadout(name, primaryGun, secondaryGun, List.copyOf(next), streaks);
    }

    /** Coche ou decoche une killstreak ; au-dela de 3, la plus ancienne sort. */
    public ClassLoadout withStreakToggled(Killstreak streak) {
        List<Killstreak> next = new ArrayList<>(streaks);
        if (next.remove(streak)) return new ClassLoadout(name, primaryGun, secondaryGun, perks, List.copyOf(next));
        next.add(streak);
        while (next.size() > Killstreak.EQUIPPED) next.remove(0);
        return new ClassLoadout(name, primaryGun, secondaryGun, perks, List.copyOf(next));
    }

    public ClassLoadout withName(String newName) {
        return new ClassLoadout(newName, primaryGun, secondaryGun, perks, streaks);
    }

    public ClassLoadout withPrimary(String gunId) {
        return new ClassLoadout(name, gunId, secondaryGun, perks, streaks);
    }

    public ClassLoadout withSecondary(String gunId) {
        return new ClassLoadout(name, primaryGun, gunId, perks, streaks);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(name, MAX_NAME_LENGTH);
        buf.writeUtf(primaryGun, 128);
        buf.writeUtf(secondaryGun, 128);
        for (int category = 1; category <= Perk.CATEGORIES; category++) {
            buf.writeEnum(perk(category));
        }
        for (Killstreak streak : streaksNormalized()) buf.writeEnum(streak);
    }

    public static ClassLoadout read(FriendlyByteBuf buf) {
        String name = buf.readUtf(MAX_NAME_LENGTH);
        String primary = buf.readUtf(128);
        String secondary = buf.readUtf(128);
        List<Perk> perks = List.of(buf.readEnum(Perk.class), buf.readEnum(Perk.class), buf.readEnum(Perk.class));
        List<Killstreak> streaks = new ArrayList<>();
        for (int i = 0; i < Killstreak.EQUIPPED; i++) streaks.add(buf.readEnum(Killstreak.class));
        return new ClassLoadout(name, primary, secondary, perks, List.copyOf(streaks));
    }
}
