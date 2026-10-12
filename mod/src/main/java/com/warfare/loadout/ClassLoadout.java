package com.warfare.loadout;

import com.warfare.WarfareConfig;
import net.minecraft.network.FriendlyByteBuf;

import java.util.List;

/**
 * Une classe personnalisee : un nom, deux armes et un atout par categorie.
 * Immuable ; toujours validee cote serveur avant d'etre enregistree.
 */
public record ClassLoadout(String name, String primaryGun, String secondaryGun, List<Perk> perks) {
    public static final int MAX_NAME_LENGTH = 16;

    public static ClassLoadout defaultFor(int slot) {
        return new ClassLoadout("Classe " + (slot + 1), WarfareConfig.primaryGun, WarfareConfig.secondaryGun,
                List.of(Perk.defaultOf(1), Perk.defaultOf(2), Perk.defaultOf(3)));
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

    public ClassLoadout withPerk(Perk perk) {
        List<Perk> next = new java.util.ArrayList<>();
        for (int category = 1; category <= Perk.CATEGORIES; category++) {
            next.add(perk.category() == category ? perk : perk(category));
        }
        return new ClassLoadout(name, primaryGun, secondaryGun, List.copyOf(next));
    }

    public ClassLoadout withName(String newName) {
        return new ClassLoadout(newName, primaryGun, secondaryGun, perks);
    }

    public ClassLoadout withPrimary(String gunId) {
        return new ClassLoadout(name, gunId, secondaryGun, perks);
    }

    public ClassLoadout withSecondary(String gunId) {
        return new ClassLoadout(name, primaryGun, gunId, perks);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(name, MAX_NAME_LENGTH);
        buf.writeUtf(primaryGun, 128);
        buf.writeUtf(secondaryGun, 128);
        for (int category = 1; category <= Perk.CATEGORIES; category++) {
            buf.writeEnum(perk(category));
        }
    }

    public static ClassLoadout read(FriendlyByteBuf buf) {
        String name = buf.readUtf(MAX_NAME_LENGTH);
        String primary = buf.readUtf(128);
        String secondary = buf.readUtf(128);
        List<Perk> perks = List.of(buf.readEnum(Perk.class), buf.readEnum(Perk.class), buf.readEnum(Perk.class));
        return new ClassLoadout(name, primary, secondary, perks);
    }
}
