package com.warfare.loadout;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

/**
 * Atouts, repartis en 3 categories exclusives (un seul atout par categorie).
 *
 * Les deux atouts demandes a l'origine qui n'ont pas pu etre branches sur TaCZ 1.1.8
 * sont documentes dans docs/SETUP.md :
 *   - "Mains rapides" (vitesse de rechargement) : aucune propriete de rechargement dans
 *     com.tacz.guns.api.GunProperties, et GunReloadEvent est poste AVANT que TaCZ
 *     n'ecrive reloadTimestamp -> remplace par "Visee eclair" (ADS_TIME).
 *   - "Commando" (portee au corps a corps) : GunMeleeData.getDistance() est lu directement
 *     depuis les donnees de l'arme, sans modificateur ni evenement
 *     -> remplace par "Cadence soutenue" (ROUNDS_PER_MINUTE).
 */
public enum Perk {
    SPRINTER("sprinteur", 1, "Sprinteur", "+10% de vitesse de deplacement"),
    QUICK_AIM("visee_eclair", 1, "Visee eclair", "-30% de temps de mise en joue"),

    STOPPING_POWER("puissance_arret", 2, "Puissance d'arret", "+20% de degats par balle"),
    FLAK("blinde", 2, "Blinde", "-30% de degats d'explosion"),

    GHOST("pas_de_loup", 3, "Pas de loup", "Tes pas ne font aucun bruit"),
    RAPID_FIRE("cadence", 3, "Cadence soutenue", "+15% de cadence de tir");

    public static final int CATEGORIES = 3;

    private final String id;
    private final int category;
    private final String title;
    private final String description;

    Perk(String id, int category, String title, String description) {
        this.id = id;
        this.category = category;
        this.title = title;
        this.description = description;
    }

    public String id() {
        return id;
    }

    /** 1, 2 ou 3. */
    public int category() {
        return category;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public static List<Perk> ofCategory(int category) {
        return Arrays.stream(values()).filter(p -> p.category == category).toList();
    }

    /** Premier atout de la categorie, utilise comme valeur par defaut. */
    public static Perk defaultOf(int category) {
        return ofCategory(category).get(0);
    }

    @Nullable
    public static Perk byId(String id) {
        for (Perk p : values()) {
            if (p.id.equals(id)) return p;
        }
        return null;
    }
}
