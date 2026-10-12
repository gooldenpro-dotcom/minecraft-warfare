package com.warfare.streak;

import com.warfare.WarfareConfig;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Les 5 series d'eliminations disponibles. Chaque joueur en choisit 3 dans le menu de classe.
 * Les seuils viennent de la config, donc l'ordre d'affichage est recalcule a chaque lecture.
 */
public enum Killstreak {
    DRONE("drone", "Drone de reperage", Items.SPYGLASS),
    RESUPPLY("ravitaillement", "Ravitaillement", Items.CHEST),
    AIRSTRIKE("frappe_aerienne", "Frappe aerienne", Items.FIREWORK_ROCKET),
    TURRET("tourelle", "Tourelle sentinelle", Items.DISPENSER),
    CARPET("bombardement", "Bombardement", Items.TNT);

    /** Nombre de killstreaks equipees par classe. */
    public static final int EQUIPPED = 3;

    private final String id;
    private final String title;
    private final Item item;

    Killstreak(String id, String title, Item item) {
        this.id = id;
        this.title = title;
        this.item = item;
    }

    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    public Item item() {
        return item;
    }

    public int threshold() {
        return switch (this) {
            case DRONE -> WarfareConfig.droneThreshold;
            case RESUPPLY -> WarfareConfig.resupplyThreshold;
            case AIRSTRIKE -> WarfareConfig.airstrikeThreshold;
            case TURRET -> WarfareConfig.turretThreshold;
            case CARPET -> WarfareConfig.carpetThreshold;
        };
    }

    public String description() {
        return switch (this) {
            case DRONE -> "Ennemis en surbrillance pendant " + WarfareConfig.droneSeconds
                    + " s, visible par toi seul";
            case RESUPPLY -> "Munitions pleines et un bonus aleatoire";
            case AIRSTRIKE -> "Vise un point : salve de " + WarfareConfig.airstrikeBombs
                    + " explosions " + WarfareConfig.airstrikeDelay + " s plus tard";
            case TURRET -> "Tourelle posee, tire " + WarfareConfig.turretSeconds + " s sur "
                    + WarfareConfig.turretRange + " blocs";
            case CARPET -> WarfareConfig.carpetBombs + " frappes sur toute l'arene en "
                    + WarfareConfig.carpetSeconds + " s";
        };
    }

    /** Toutes les killstreaks, du seuil le plus bas au plus haut. */
    public static List<Killstreak> sorted() {
        List<Killstreak> list = new ArrayList<>(List.of(values()));
        list.sort(Comparator.comparingInt(Killstreak::threshold));
        return list;
    }

    /** Selection par defaut : les 3 seuils les plus bas. */
    public static List<Killstreak> defaults() {
        return List.copyOf(sorted().subList(0, EQUIPPED));
    }

    @Nullable
    public static Killstreak byId(String id) {
        for (Killstreak streak : values()) {
            if (streak.id.equals(id)) return streak;
        }
        return null;
    }
}
