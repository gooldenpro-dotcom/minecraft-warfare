package com.warfare.client;

import com.warfare.net.WarfareNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Hub du lobby : etat de la partie, liste des joueurs, acces aux classes et aux killstreaks.
 * Habillage maison, aucun element graphique tiers.
 */
public class LobbyScreen extends Screen {
    private static final int PANEL_W = 400;
    private static final int PANEL_H = 230;
    private static final int MENU_W = 170;

    private int left;
    private int top;

    public LobbyScreen() {
        super(Component.literal("Partie privee"));
    }

    @Override
    protected void init() {
        left = (width - PANEL_W) / 2;
        top = (height - PANEL_H) / 2;

        int x = left + 12;
        int y = top + 34;

        addRenderableWidget(new WarButton(x, y, MENU_W, 22,
                Component.literal("LANCER LA PARTIE"), this::requestStart)
                .enabled(ClientMatchState.canStart));
        y += 26;
        addRenderableWidget(new WarButton(x, y, MENU_W, 22,
                Component.literal("CREER UNE CLASSE"),
                () -> minecraft.setScreen(new ClassScreen(ClassScreen.Tab.WEAPONS, this))));
        y += 26;
        addRenderableWidget(new WarButton(x, y, MENU_W, 22,
                Component.literal("KILLSTREAKS"),
                () -> minecraft.setScreen(new ClassScreen(ClassScreen.Tab.STREAKS, this))));
        y += 26;
        addRenderableWidget(new WarButton(x, y, MENU_W, 22,
                Component.literal("RETOUR AU JEU"), this::onClose));

        addRenderableWidget(new WarButton(left + PANEL_W - 24, top + 4, 19, 14,
                Component.literal("X"), this::onClose));
    }

    private void requestStart() {
        WarfareNetwork.CHANNEL.sendToServer(new WarfareNetwork.RequestStart());
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, Palette.SHADE);
        Palette.frame(graphics, font, left, top, PANEL_W, PANEL_H, "PARTIE PRIVEE", stateLabel());

        // Vignette mode et carte, sous le menu
        int infoX = left + 12;
        int infoY = top + 146;
        graphics.fill(infoX, infoY, infoX + MENU_W, infoY + 72, Palette.INSET);
        Palette.outline(graphics, infoX, infoY, MENU_W, 72, Palette.BORDER);
        graphics.fill(infoX + 1, infoY + 1, infoX + 4, infoY + 71, Palette.ACCENT);
        graphics.drawString(font, "CHACUN POUR SOI", infoX + 10, infoY + 9, Palette.ACCENT, false);
        graphics.drawString(font, ClientMatchState.mapName, infoX + 10, infoY + 23, Palette.TEXT, false);
        graphics.drawString(font, "Classe equipee", infoX + 10, infoY + 43, Palette.MUTED, false);
        graphics.drawString(font, trim(ClientClassData.classes.current().name(), 22),
                infoX + 10, infoY + 55, Palette.TEXT, false);

        // Liste des joueurs
        int listX = left + 194;
        int listY = top + 28;
        int listW = PANEL_W - (listX - left) - 12;
        int listH = PANEL_H - 40;
        graphics.fill(listX, listY, listX + listW, listY + listH, Palette.INSET);
        Palette.outline(graphics, listX, listY, listW, listH, Palette.BORDER);

        String header = "JOUEURS : " + ClientMatchState.players.size() + " / " + ClientMatchState.maxPlayers;
        graphics.drawString(font, header, listX + 8, listY + 7, Palette.ACCENT, false);
        graphics.hLine(listX + 6, listX + listW - 7, listY + 19, Palette.BORDER);

        int row = listY + 25;
        for (WarfareNetwork.MatchState.Entry entry : ClientMatchState.players) {
            if (row > listY + listH - 14) break;
            boolean me = minecraft.player != null
                    && entry.name().equals(minecraft.player.getGameProfile().getName());
            graphics.drawString(font, trim(entry.name(), 18), listX + 10, row,
                    me ? Palette.ACCENT : Palette.TEXT, false);
            String kills = String.valueOf(entry.kills());
            graphics.drawString(font, kills, listX + listW - 12 - font.width(kills), row,
                    Palette.MUTED, false);
            row += 13;
        }
        if (ClientMatchState.players.isEmpty()) {
            graphics.drawString(font, "Aucun joueur connecte", listX + 10, row, Palette.MUTED, false);
        }

        if (!ClientMatchState.canStart && ClientMatchState.inLobby()) {
            graphics.drawString(font, "Lancement reserve aux operateurs",
                    left + 12, top + PANEL_H - 14, Palette.MUTED, false);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private static String stateLabel() {
        return switch (ClientMatchState.state) {
            case "COUNTDOWN" -> "demarrage dans " + ClientMatchState.secondsLeft + " s";
            case "RUNNING" -> "partie en cours";
            case "ENDED" -> "fin de partie";
            default -> "en attente";
        };
    }

    private static String trim(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
