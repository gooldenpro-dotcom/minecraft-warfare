package com.warfare.client;

import com.warfare.net.WarfareNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Menu d'accueil du serveur. Il s'ouvre des la connexion et reste ouvert tant que
 * la partie n'a pas demarre : il n'y a pas de lobby dans lequel se deplacer.
 *
 * Mise en page a plat, dessinee entierement : pas de cadre, pas de bouton en relief,
 * aucune texture ni logo tiers.
 */
public class LobbyScreen extends Screen {
    private enum Panel { PLAYERS, SETTINGS }

    private Panel panel = Panel.PLAYERS;
    private int menuRight;
    private int menuTop;
    private int rightX;

    public LobbyScreen() {
        super(Component.literal("Partie privee"));
    }

    /** Tant qu'on est au menu, Echap ne doit pas lacher le joueur dans le decor. */
    @Override
    public boolean shouldCloseOnEsc() {
        return !ClientMatchState.inLobby();
    }

    @Override
    protected void init() {
        int margin = Math.max(44, width / 9);
        menuRight = margin + 200;
        menuTop = Math.max(70, height / 4);
        rightX = Math.max(menuRight + 90, width / 2);

        int y = menuTop + 34;
        addRenderableWidget(new MenuItem(menuRight - 200, y, 200, 20,
                Component.literal("LANCER LA PARTIE"), this::requestStart)
                .enabled(ClientMatchState.canStart));
        y += 24;
        addRenderableWidget(new MenuItem(menuRight - 200, y, 200, 20,
                Component.literal("REGLAGES DE PARTIE"), () -> {
            panel = Panel.SETTINGS;
            rebuildWidgets();
        }));
        y += 24;
        addRenderableWidget(new MenuItem(menuRight - 200, y, 200, 20,
                Component.literal("CREER UNE CLASSE"),
                () -> minecraft.setScreen(new ClassScreen(ClassScreen.Tab.WEAPONS, this))));
        y += 24;
        addRenderableWidget(new MenuItem(menuRight - 200, y, 200, 20,
                Component.literal("KILLSTREAKS"),
                () -> minecraft.setScreen(new ClassScreen(ClassScreen.Tab.STREAKS, this))));
        y += 24;
        addRenderableWidget(new MenuItem(menuRight - 200, y, 200, 20,
                Component.literal("JOUEURS"), () -> {
            panel = Panel.PLAYERS;
            rebuildWidgets();
        }));
    }

    private void requestStart() {
        WarfareNetwork.CHANNEL.sendToServer(new WarfareNetwork.RequestStart());
    }

    // ---------------------------------------------------------------- rendu

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fillGradient(0, 0, width, height, 0xE8090B07, 0xF4050604);

        int margin = menuRight - 200;

        // Titre
        graphics.drawString(font, "PARTIE PRIVEE", margin, menuTop, Palette.TEXT, false);
        graphics.fill(margin, menuTop + 14, menuRight + 14, menuTop + 15, Palette.ACCENT);
        graphics.drawString(font, stateLabel(), margin, menuTop + 20, Palette.MUTED, false);

        // Vignette de carte, en bas a gauche
        int cardW = 190;
        int cardH = 104;
        int cardY = Math.min(height - cardH - 60, menuTop + 190);
        drawMapCard(graphics, margin, cardY, cardW, cardH);

        // Panneau de droite
        if (panel == Panel.PLAYERS) renderPlayers(graphics);
        else renderSettings(graphics);

        // Pied
        String hint = shouldCloseOnEsc()
                ? "ECHAP  retour au jeu"
                : "La partie demarre depuis ce menu";
        graphics.fill(margin, height - 46, width - margin, height - 45, 0x40B9922F);
        graphics.drawString(font, hint, margin, height - 38, Palette.MUTED, false);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderPlayers(GuiGraphics graphics) {
        int y = menuTop;
        String header = "JOUEURS : " + ClientMatchState.players.size() + " / " + ClientMatchState.maxPlayers;
        graphics.drawString(font, header, rightX, y, Palette.MUTED, false);
        y += 14;

        if (ClientMatchState.players.isEmpty()) {
            graphics.drawString(font, "En attente de connexions", rightX, y + 6, Palette.MUTED, false);
            return;
        }
        int rowW = Math.min(300, width - rightX - 44);
        for (WarfareNetwork.MatchState.Entry entry : ClientMatchState.players) {
            if (y > height - 110) break;
            boolean me = minecraft.player != null
                    && entry.name().equals(minecraft.player.getGameProfile().getName());
            graphics.fill(rightX, y, rightX + rowW, y + 18, me ? 0x40B9922F : 0x55101309);
            if (me) graphics.fill(rightX, y, rightX + 2, y + 18, Palette.ACCENT);
            graphics.drawString(font, entry.name(), rightX + 8, y + 5,
                    me ? Palette.ACCENT : Palette.TEXT, false);
            String kills = String.valueOf(entry.kills());
            graphics.drawString(font, kills, rightX + rowW - 10 - font.width(kills), y + 5,
                    Palette.MUTED, false);
            y += 20;
        }
    }

    private void renderSettings(GuiGraphics graphics) {
        int y = menuTop;
        graphics.drawString(font, "REGLAGES DE PARTIE", rightX, y, Palette.MUTED, false);
        y += 16;
        String[][] rows = {
                {"Mode", "Chacun pour soi"},
                {"Carte", ClientMatchState.mapName},
                {"Score a atteindre", ClientMatchState.killLimit + " eliminations"},
                {"Duree maximale", ClientMatchState.matchSeconds / 60 + " min "
                        + ClientMatchState.matchSeconds % 60 + " s"},
                {"Joueurs connectes", ClientMatchState.players.size() + " / " + ClientMatchState.maxPlayers},
        };
        int rowW = Math.min(300, width - rightX - 44);
        for (String[] row : rows) {
            graphics.drawString(font, row[0], rightX, y, Palette.MUTED, false);
            graphics.drawString(font, row[1], rightX + rowW - font.width(row[1]), y, Palette.TEXT, false);
            y += 13;
            graphics.fill(rightX, y, rightX + rowW, y + 1, 0x3346523F);
            y += 9;
        }
        graphics.drawString(font, "Modifiable dans config/warfare-common.toml", rightX, y + 6,
                Palette.MUTED, false);
    }

    /**
     * Vignette de la carte : un paysage dessine a la main (ciel, relief, sol),
     * surmonte du nom de la dimension et du mode. Aucune image importee.
     */
    private void drawMapCard(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.fillGradient(x, y, x + w, y + h - 32, 0xFF5B86B5, 0xFF9FC4DE);
        int ground = y + h - 46;
        graphics.fill(x, ground, x + w, y + h - 14, 0xFF4E6B33);
        graphics.fill(x, ground + 6, x + w, y + h - 14, 0xFF6B5033);
        // relief
        for (int i = 0; i < 5; i++) {
            int hillW = w / 5;
            int hx = x + i * hillW;
            int hh = 14 + ((i * 7) % 18);
            graphics.fill(hx + 4, ground - hh, hx + hillW - 4, ground, 0xFF3F5A2B);
            graphics.fill(hx + 4, ground - hh, hx + hillW - 4, ground - hh + 3, 0xFF597A3C);
        }
        graphics.fill(x, y + h - 14, x + w, y + h, 0xE6101309);
        Palette.outline(graphics, x, y, w, h, Palette.BORDER);

        String map = ClientMatchState.mapName;
        graphics.drawString(font, map, x + w - 6 - font.width(map), y + 5, 0xFFFFFFFF, true);
        graphics.drawString(font, "CHACUN POUR SOI", x + 6, y + h - 11, Palette.ACCENT, false);
    }

    private static String stateLabel() {
        return switch (ClientMatchState.state) {
            case "COUNTDOWN" -> "Demarrage dans " + ClientMatchState.secondsLeft + " s";
            case "RUNNING" -> "Partie en cours";
            case "ENDED" -> "Fin de partie";
            default -> ClientMatchState.canStart
                    ? "Pret a lancer" : "En attente de l'hote";
        };
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
