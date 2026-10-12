package com.warfare.client;

import com.warfare.loadout.GunCatalog;
import com.warfare.loadout.PlayerClasses;
import com.warfare.net.WarfareNetwork;

import java.util.ArrayList;
import java.util.List;

/** Copie cliente des classes du joueur. Le serveur reste la source de verite. */
public final class ClientClassData {
    public static PlayerClasses classes = new PlayerClasses();
    public static List<String> allowedGuns = List.of();
    public static boolean menuAllowed = false;

    public static void accept(WarfareNetwork.SyncClasses msg) {
        classes = msg.classes();
        allowedGuns = msg.allowedGuns();
        menuAllowed = msg.menuAllowed();
    }

    /** Catalogue local filtre par la liste blanche recue du serveur. */
    public static List<GunCatalog.Entry> catalog() {
        List<GunCatalog.Entry> entries = new ArrayList<>();
        for (GunCatalog.Entry entry : GunCatalog.all()) {
            if (allowedGuns.isEmpty() || allowedGuns.contains(entry.gunId())) entries.add(entry);
        }
        if (entries.isEmpty()) {
            for (String gunId : allowedGuns) entries.add(new GunCatalog.Entry(gunId, "autre", gunId));
        }
        return entries;
    }

    private ClientClassData() {}
}
