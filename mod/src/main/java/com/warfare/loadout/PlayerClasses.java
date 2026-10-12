package com.warfare.loadout;

import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

/** Les 5 emplacements de classe d'un joueur, plus celui qui est equipe. */
public final class PlayerClasses {
    public static final int SLOTS = 5;

    private final List<ClassLoadout> slots = new ArrayList<>(SLOTS);
    private int selected;

    public PlayerClasses() {
        for (int i = 0; i < SLOTS; i++) slots.add(ClassLoadout.defaultFor(i));
    }

    public ClassLoadout get(int slot) {
        return slots.get(Math.floorMod(slot, SLOTS));
    }

    public void set(int slot, ClassLoadout loadout) {
        if (slot < 0 || slot >= SLOTS) return;
        slots.set(slot, loadout);
    }

    public int selected() {
        return selected;
    }

    public void select(int slot) {
        if (slot >= 0 && slot < SLOTS) selected = slot;
    }

    public ClassLoadout current() {
        return get(selected);
    }

    public List<ClassLoadout> all() {
        return List.copyOf(slots);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(selected);
        for (ClassLoadout loadout : slots) loadout.write(buf);
    }

    public static PlayerClasses read(FriendlyByteBuf buf) {
        PlayerClasses classes = new PlayerClasses();
        classes.selected = buf.readVarInt();
        for (int i = 0; i < SLOTS; i++) classes.slots.set(i, ClassLoadout.read(buf));
        classes.select(classes.selected);
        return classes;
    }
}
