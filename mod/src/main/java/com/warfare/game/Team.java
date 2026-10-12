package com.warfare.game;

import net.minecraft.ChatFormatting;

/** Les deux camps des modes par equipe. NONE = chacun pour soi. */
public enum Team {
    NONE("libre", "Libre", ChatFormatting.GRAY, 0xFF9AA293),
    ALPHA("alpha", "ALPHA", ChatFormatting.RED, 0xFFD24A3A),
    BRAVO("bravo", "BRAVO", ChatFormatting.AQUA, 0xFF3FA9C9);

    private final String id;
    private final String title;
    private final ChatFormatting chat;
    private final int rgb;

    Team(String id, String title, ChatFormatting chat, int rgb) {
        this.id = id;
        this.title = title;
        this.chat = chat;
        this.rgb = rgb;
    }

    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    public ChatFormatting chat() {
        return chat;
    }

    /** Couleur ARGB pour l'ATH et les menus. */
    public int rgb() {
        return rgb;
    }

    public Team other() {
        return switch (this) {
            case ALPHA -> BRAVO;
            case BRAVO -> ALPHA;
            case NONE -> NONE;
        };
    }

    public static Team byId(String id) {
        for (Team team : values()) {
            if (team.id.equalsIgnoreCase(id)) return team;
        }
        return NONE;
    }
}
