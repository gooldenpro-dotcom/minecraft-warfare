package com.warfare.loadout;

import com.tacz.guns.api.TimelessAPI;
import com.warfare.WarfareConfig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Armes selectionnables : lues dans les packs TaCZ reellement charges,
 * filtrees par la liste blanche de la config (vide = toutes les armes).
 *
 * Le meme catalogue est calcule sur le serveur (validation) et sur le client (menu),
 * car TaCZ charge ses packs des deux cotes.
 */
public final class GunCatalog {

    /** gunId = "tacz:ak47", type = "rifle", nameKey = cle de traduction TaCZ. */
    public record Entry(String gunId, String type, String nameKey) {}

    public static List<Entry> all() {
        List<Entry> entries = new ArrayList<>();
        for (Map.Entry<net.minecraft.resources.ResourceLocation,
                com.tacz.guns.resource.index.CommonGunIndex> e : TimelessAPI.getAllCommonGunIndex()) {
            String id = e.getKey().toString();
            if (!isWhitelisted(id)) continue;
            String type = e.getValue().getType();
            entries.add(new Entry(id, type == null || type.isBlank() ? "autre" : type,
                    e.getValue().getPojo().getName()));
        }
        entries.sort(Comparator.comparing(Entry::type).thenComparing(Entry::gunId));
        return entries;
    }

    /** Types presents dans le catalogue, dans l'ordre d'affichage. */
    public static List<String> types() {
        LinkedHashSet<String> types = new LinkedHashSet<>();
        for (Entry entry : all()) types.add(entry.type());
        return List.copyOf(types);
    }

    /** Validation serveur : l'arme existe-t-elle et est-elle autorisee ? */
    public static boolean isAllowed(String gunId) {
        if (!isWhitelisted(gunId)) return false;
        var id = net.minecraft.resources.ResourceLocation.tryParse(gunId);
        return id != null && TimelessAPI.getCommonGunIndex(id).isPresent();
    }

    private static boolean isWhitelisted(String gunId) {
        List<? extends String> whitelist = WarfareConfig.gunWhitelist;
        return whitelist.isEmpty() || whitelist.contains(gunId);
    }

    /** Libelle lisible d'un type TaCZ. */
    public static String typeLabel(String type) {
        return switch (type) {
            case "rifle" -> "Fusils d'assaut";
            case "smg" -> "Mitraillettes";
            case "sniper" -> "Fusils de precision";
            case "pistol" -> "Pistolets";
            case "shotgun" -> "Fusils a pompe";
            case "rpg" -> "Lance-roquettes";
            case "mg" -> "Mitrailleuses";
            default -> type.substring(0, 1).toUpperCase() + type.substring(1);
        };
    }

    private GunCatalog() {}
}
