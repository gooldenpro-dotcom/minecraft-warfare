package com.warfare.client;

import com.warfare.game.GameMode;
import com.warfare.net.WarfareNetwork;
import com.warfare.progression.Rank;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * Menu d'accueil du serveur : choix du mode, de la carte, des classes, classement,
 * profil, et sortie du serveur.
 *
 * Il s'ouvre a la connexion et entre deux parties. Echap le ferme (et ne le rouvre
 * pas tout seul) : le joueur garde toujours la main sur son jeu.
 *
 * Mise en page a plat, dessinee entierement : pas de cadre, pas de bouton en relief,
 * aucune texture ni logo tiers.
 */
public class LobbyScreen extends Screen {
    private enum Panel { PLAYERS, MODES, MAPS, RANKING, PROFILE, SETTINGS }

    private Panel panel = Panel.PLAYERS;
    private int menuRight;
    private int menuTop;
    private int rightX;
    private int rightWidth;

    public LobbyScreen() {
        super(Component.literal("Minecraft Warfare"));
    }

    /** Echap ferme toujours le menu : sinon le joueur se retrouve coince. */
    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    protected void init() {
        int margin = Math.max(30, width / 10);
        menuRight = margin + 190;
        menuTop = Math.max(50, height / 6);
        rightX = Math.max(menuRight + 70, width / 2 - 20);
        rightWidth = Math.max(160, width - rightX - margin);

        int y = menuTop + 34;
        y = addItem(y, "LANCER LA PARTIE", this::requestStart, ClientMatchState.canStart);
        y = addItem(y, "MODE DE JEU", () -> show(Panel.MODES), true);
        y = addItem(y, "CARTE", () -> show(Panel.MAPS), true);
        y = addItem(y, "CREER UNE CLASSE",
                () -> minecraft.setScreen(new ClassScreen(ClassScreen.Tab.WEAPONS, this)), true);
        y = addItem(y, "KILLSTREAKS",
                () -> minecraft.setScreen(new ClassScreen(ClassScreen.Tab.STREAKS, this)), true);
        y = addItem(y, "CLASSEMENT", () -> {
            WarfareNetwork.CHANNEL.sendToServer(new WarfareNetwork.RequestLeaderboard());
            show(Panel.RANKING);
        }, true);
        y = addItem(y, "MON PROFIL", () -> show(Panel.PROFILE), true);
        y = addItem(y, "JOUEURS", () -> show(Panel.PLAYERS), true);
        y = addItem(y, "REGLAGES DE PARTIE", () -> show(Panel.SETTINGS), true);
        y += 10;
        y = addItem(y, "RETOUR AU JEU", this::onClose, true);
        addItem(y, "QUITTER LE SERVEUR", this::disconnect, true);

        buildPanelWidgets();
    }

    private int addItem(int y, String label, Runnable action, boolean enabled) {
        addRenderableWidget(new MenuItem(menuRight - 190, y, 190, 18,
                Component.literal(label), action).enabled(enabled));
        return y + 21;
    }

    private void show(Panel next) {
        panel = next;
        rebuildWidgets();
    }

    /** Les panneaux interactifs (modes, cartes, prestige) ont leurs propres boutons. */
    private void buildPanelWidgets() {
        switch (panel) {
            case MODES -> {
                int y = menuTop + 16;
                for (GameMode mode : GameMode.values()) {
                    final GameMode target = mode;
                    WarButton button = new WarButton(rightX, y, Math.min(240, rightWidth), 18,
                            Component.literal(mode.title()),
                            () -> WarfareNetwork.CHANNEL.sendToServer(
                                    new WarfareNetwork.RequestMode(target.id())));
                    button.selected = mode.id().equals(ClientMatchState.mode);
                    addRenderableWidget(button);
                    y += 22;
                }
            }
            case MAPS -> {
                int y = menuTop + 16;
                for (String arena : ClientMatchState.arenas) {
                    final String target = arena;
                    WarButton button = new WarButton(rightX, y, Math.min(240, rightWidth), 18,
                            Component.literal(arena.toUpperCase(Locale.ROOT)),
                            () -> WarfareNetwork.CHANNEL.sendToServer(
                                    new WarfareNetwork.RequestArena(target)));
                    button.selected = arena.equals(ClientMatchState.arena);
                    addRenderableWidget(button);
                    y += 22;
                }
            }
            case PROFILE -> {
                if (ClientMatchState.level >= Rank.MAX_LEVEL
                        && ClientMatchState.prestige < Rank.MAX_PRESTIGE) {
                    addRenderableWidget(new WarButton(rightX, menuTop + 90, Math.min(200, rightWidth), 18,
                            Component.literal("PASSER AU PRESTIGE"),
                            () -> WarfareNetwork.CHANNEL.sendToServer(
                                    new WarfareNetwork.RequestPrestige())));
                }
            }
            default -> {
            }
        }
    }

    private void requestStart() {
        WarfareNetwork.CHANNEL.sendToServer(new WarfareNetwork.RequestStart());
    }

    /** Sortie propre du serveur, comme le bouton du menu pause. */
    private void disconnect() {
        if (minecraft == null) return;
        ClientMatchState.reset();
        if (minecraft.level != null) minecraft.level.disconnect();
        minecraft.clearLevel();
        minecraft.setScreen(new TitleScreen());
    }

    @Override
    public void onClose() {
        ClientMatchState.menuDismissed = true;
        if (minecraft != null) minecraft.setScreen(null);
    }

    // ---------------------------------------------------------------- rendu

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fillGradient(0, 0, width, height, 0xE8090B07, 0xF4050604);

        int margin = menuRight - 190;

        // Titre
        graphics.drawString(font, "MINECRAFT WARFARE", margin, menuTop, Palette.TEXT, false);
        graphics.fill(margin, menuTop + 14, menuRight + 14, menuTop + 15, Palette.ACCENT);
        graphics.drawString(font, stateLabel(), margin, menuTop + 20, Palette.MUTED, false);

        switch (panel) {
            case PLAYERS -> renderPlayers(graphics);
            case MODES -> renderModes(graphics);
            case MAPS -> renderMaps(graphics);
            case RANKING -> renderRanking(graphics);
            case PROFILE -> renderProfile(graphics);
            case SETTINGS -> renderSettings(graphics);
        }

        graphics.fill(margin, height - 30, width - margin, height - 29, 0x40B9922F);
        graphics.drawString(font, "ECHAP ferme le menu  -  M le rouvre  -  C les classes",
                margin, height - 22, Palette.MUTED, false);

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
        int rowW = Math.min(300, rightWidth);
        for (WarfareNetwork.MatchState.Entry entry : ClientMatchState.players) {
            if (y > height - 60) break;
            boolean me = minecraft != null && minecraft.player != null
                    && entry.name().equals(minecraft.player.getGameProfile().getName());
            graphics.fill(rightX, y, rightX + rowW, y + 18, me ? 0x40B9922F : 0x55101309);
            int accent = teamColor(entry.team());
            graphics.fill(rightX, y, rightX + 2, y + 18, accent);
            graphics.drawString(font, entry.name(), rightX + 8, y + 5,
                    me ? Palette.ACCENT : Palette.TEXT, false);
            String right = ClientMatchState.running()
                    ? entry.score() + " elim."
                    : "niv. " + entry.score();
            graphics.drawString(font, right, rightX + rowW - 10 - font.width(right), y + 5,
                    Palette.MUTED, false);
            y += 20;
        }
    }

    private void renderModes(GuiGraphics graphics) {
        graphics.drawString(font, "MODE DE JEU", rightX, menuTop, Palette.MUTED, false);
        GameMode selected = GameMode.byId(ClientMatchState.mode);
        int y = menuTop + 16 + GameMode.values().length * 22 + 6;
        graphics.drawString(font, selected.title(), rightX, y, Palette.ACCENT, false);
        graphics.drawString(font, selected.description(), rightX, y + 12, Palette.TEXT, false);
        graphics.drawString(font, ClientMatchState.scoreLimit + " points  -  "
                        + ClientMatchState.matchSeconds / 60 + " min",
                rightX, y + 24, Palette.MUTED, false);
    }

    private void renderMaps(GuiGraphics graphics) {
        graphics.drawString(font, "CARTE", rightX, menuTop, Palette.MUTED, false);
        if (ClientMatchState.arenas.isEmpty()) {
            graphics.drawString(font, "Aucune carte enregistree.", rightX, menuTop + 20,
                    Palette.TEXT, false);
            graphics.drawString(font, "Un operateur doit faire", rightX, menuTop + 34,
                    Palette.MUTED, false);
            graphics.drawString(font, "/warfare arena create <nom>", rightX, menuTop + 46,
                    Palette.MUTED, false);
            return;
        }
        int y = menuTop + 16 + ClientMatchState.arenas.size() * 22 + 6;
        graphics.drawString(font, "Carte active : " + ClientMatchState.arena.toUpperCase(Locale.ROOT),
                rightX, y, Palette.ACCENT, false);
    }

    private void renderRanking(GuiGraphics graphics) {
        int y = menuTop;
        graphics.drawString(font, "CLASSEMENT DU SERVEUR", rightX, y, Palette.MUTED, false);
        y += 16;
        if (ClientMatchState.leaderboard.isEmpty()) {
            graphics.drawString(font, "Aucune partie enregistree.", rightX, y, Palette.TEXT, false);
            return;
        }
        int rowW = Math.min(320, rightWidth);
        int rank = 1;
        for (WarfareNetwork.Leaderboard.Row row : ClientMatchState.leaderboard) {
            if (y > height - 50) break;
            graphics.drawString(font, "#" + rank, rightX, y, Palette.MUTED, false);
            graphics.drawString(font, row.name(), rightX + 22, y, Palette.TEXT, false);
            String level = "niv." + row.level() + (row.prestige() > 0 ? " P" + row.prestige() : "");
            graphics.drawString(font, level, rightX + rowW - 150 - font.width(level), y,
                    Palette.ACCENT, false);
            String kd = row.kills() + "/" + row.deaths();
            graphics.drawString(font, kd, rightX + rowW - 90 - font.width(kd), y, Palette.TEXT, false);
            String score = row.score() + " pts";
            graphics.drawString(font, score, rightX + rowW - font.width(score), y, Palette.MUTED, false);
            y += 12;
            rank++;
        }
    }

    private void renderProfile(GuiGraphics graphics) {
        int y = menuTop;
        graphics.drawString(font, "MON PROFIL", rightX, y, Palette.MUTED, false);
        y += 16;
        graphics.drawString(font, Rank.label(ClientMatchState.level, ClientMatchState.prestige),
                rightX, y, Palette.ACCENT, false);
        y += 16;

        int barWidth = Math.min(220, rightWidth);
        long need = Math.max(1, ClientMatchState.xpToNext);
        int filled = (int) Math.min(barWidth, ClientMatchState.xpIntoLevel * barWidth / need);
        graphics.fill(rightX, y, rightX + barWidth, y + 6, 0xFF101309);
        graphics.fill(rightX, y, rightX + filled, y + 6, Palette.ACCENT);
        y += 12;
        graphics.drawString(font, ClientMatchState.xpIntoLevel + " / " + need + " XP",
                rightX, y, Palette.MUTED, false);
        y += 20;
        if (ClientMatchState.level >= Rank.MAX_LEVEL) {
            graphics.drawString(font, "Niveau maximum atteint.", rightX, y, Palette.TEXT, false);
        } else {
            graphics.drawString(font, "Prochain grade : " + Rank.title(ClientMatchState.level + 1),
                    rightX, y, Palette.TEXT, false);
        }
    }

    private void renderSettings(GuiGraphics graphics) {
        int y = menuTop;
        graphics.drawString(font, "REGLAGES DE PARTIE", rightX, y, Palette.MUTED, false);
        y += 16;
        String[][] rows = {
                {"Mode", ClientMatchState.modeTitle()},
                {"Carte", ClientMatchState.arena.isEmpty()
                        ? "aucune" : ClientMatchState.arena.toUpperCase(Locale.ROOT)},
                {"Score a atteindre", ClientMatchState.scoreLimit + " points"},
                {"Duree maximale", ClientMatchState.matchSeconds / 60 + " min "
                        + ClientMatchState.matchSeconds % 60 + " s"},
                {"Joueurs connectes", ClientMatchState.players.size() + " / " + ClientMatchState.maxPlayers},
        };
        int rowW = Math.min(300, rightWidth);
        for (String[] row : rows) {
            graphics.drawString(font, row[0], rightX, y, Palette.MUTED, false);
            graphics.drawString(font, row[1], rightX + rowW - font.width(row[1]), y, Palette.TEXT, false);
            y += 13;
            graphics.fill(rightX, y, rightX + rowW, y + 1, 0x3346523F);
            y += 9;
        }
        graphics.drawString(font, "Details dans config/warfare-common.toml", rightX, y + 6,
                Palette.MUTED, false);
    }

    private static int teamColor(String team) {
        return switch (team) {
            case "alpha" -> 0xFFD24A3A;
            case "bravo" -> 0xFF3FA9C9;
            default -> Palette.BORDER;
        };
    }

    private static String stateLabel() {
        return switch (ClientMatchState.state) {
            case "COUNTDOWN" -> "Demarrage dans " + ClientMatchState.secondsLeft + " s";
            case "RUNNING" -> "Partie en cours - " + ClientMatchState.modeTitle();
            case "ENDED" -> "Fin de partie";
            default -> ClientMatchState.modeTitle() + " - "
                    + (ClientMatchState.canStart ? "pret a lancer" : "en attente");
        };
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
