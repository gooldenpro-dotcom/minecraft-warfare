package com.warfare.game;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Une carte jouable : un point de depart de menu (lobby) et ses points d'apparition,
 * eventuellement etiquetes par equipe pour les modes a deux camps.
 *
 * Plusieurs arenes cohabitent dans le meme monde : c'est ce qui permet de choisir
 * sa carte depuis le menu sans redemarrer le serveur.
 */
public final class Arena {

    /** Un point d'apparition et son camp (NONE = utilisable par tout le monde). */
    public record Point(Team team, Spot spot) {
        public CompoundTag write() {
            CompoundTag tag = spot.write();
            tag.putString("team", team.id());
            return tag;
        }

        public static Point read(CompoundTag tag) {
            Team team = tag.contains("team") ? Team.byId(tag.getString("team")) : Team.NONE;
            return new Point(team, Spot.read(tag));
        }
    }

    private final String name;
    @Nullable
    private Spot lobby;
    private final List<Point> spawns = new ArrayList<>();

    public Arena(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    /** Nom affiche dans les menus. */
    public String title() {
        String cleaned = name.replace('_', ' ').trim();
        return cleaned.isEmpty() ? name : cleaned.toUpperCase(java.util.Locale.ROOT);
    }

    @Nullable
    public Spot lobby() {
        return lobby;
    }

    public void setLobby(@Nullable Spot spot) {
        this.lobby = spot;
    }

    public List<Point> spawns() {
        return List.copyOf(spawns);
    }

    /** Points utilisables par un camp : ceux du camp, plus les points neutres. */
    public List<Spot> spawnsFor(Team team) {
        List<Spot> out = new ArrayList<>();
        for (Point point : spawns) {
            if (team == Team.NONE || point.team() == Team.NONE || point.team() == team) out.add(point.spot());
        }
        if (out.isEmpty()) {
            for (Point point : spawns) out.add(point.spot());
        }
        return out;
    }

    public int add(Team team, Spot spot) {
        spawns.add(new Point(team, spot));
        return spawns.size();
    }

    public boolean remove(int index) {
        if (index < 0 || index >= spawns.size()) return false;
        spawns.remove(index);
        return true;
    }

    public void clearSpawns() {
        spawns.clear();
    }

    /** Jouable : un lobby et au moins un point d'apparition. */
    public boolean isReady() {
        return lobby != null && !spawns.isEmpty();
    }

    /** Jouable dans ce mode : les modes par equipe demandent un point par camp. */
    public boolean isReadyFor(GameMode mode) {
        if (!isReady()) return false;
        if (!mode.teamBased()) return true;
        return !spawnsFor(Team.ALPHA).isEmpty() && !spawnsFor(Team.BRAVO).isEmpty();
    }

    public int count(Team team) {
        int total = 0;
        for (Point point : spawns) {
            if (point.team() == team) total++;
        }
        return total;
    }

    public CompoundTag write() {
        CompoundTag tag = new CompoundTag();
        tag.putString("name", name);
        if (lobby != null) tag.put("lobby", lobby.write());
        ListTag list = new ListTag();
        for (Point point : spawns) list.add(point.write());
        tag.put("spawns", list);
        return tag;
    }

    public static Arena read(CompoundTag tag) {
        Arena arena = new Arena(tag.getString("name"));
        if (tag.contains("lobby")) arena.lobby = Spot.read(tag.getCompound("lobby"));
        ListTag list = tag.getList("spawns", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) arena.spawns.add(Point.read(list.getCompound(i)));
        return arena;
    }
}
