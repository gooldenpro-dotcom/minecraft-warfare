package com.warfare.client;

import com.warfare.game.GameMode;
import com.warfare.net.WarfareNetwork;
import com.warfare.progression.Rank;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Menu d'accueil du serveur : choix du mode, de la carte, des classes, classement,
 * profil, et sortie du serveur.
 *
 * Mise en page sur une grille centree, calculee a partir de la taille de la fenetre :
 * une zone de contenu bornee, deux colonnes, un bandeau de titre et un pied de page.
 * L'espacement des entrees est deduit de la hauteur disponible, donc le menu ne
 * deborde jamais, quelle que soit la resolution ou l'echelle d'interface.
 *
 * Tout est dessine a plat : pas de cadre, pas de bouton en relief, aucune texture ni
 * logo tiers. Police Rajdhani embarquee (voir Fonts).
 */
public class LobbyScreen extends Screen {
    private enum Panel { PLAYERS, MODES, MAPS, RANKING, PROFILE, SETTINGS }

    /** Largeur maximale de la zone de contenu : au-dela, on centre au lieu d'etaler. */
    private static final int CONTENT_MAX = 1080;
    private static final int COLUMN_GAP = 48;
    private static final int ITEM_STEP_MAX = 26;
    private static final int ITEM_STEP_MIN = 15;

    private Panel panel = Panel.PLAYERS;
    private final List<Integer> separators = new ArrayList<>();

    // Grille, recalculee a chaque init
    private int contentX;
    private int contentW;
    private int menuRight;
    private int panelX;
    private int panelW;
    private int headerY;
    private int bodyY;
    private int footerY;
    private int itemStep;

    public LobbyScreen() {
        super(Component.literal("Minecraft Warfare"));
    }

    /** Echap ferme toujours le menu : sinon le joueur se retrouve coince. */
    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    // ------------------------------------------------------------------ grille

    @Override
    protected void init() {
        int gutter = Math.max(24, Math.round(width * 0.06f));
        contentW = Math.min(CONTENT_MAX, width - gutter * 2);
        contentX = (width - contentW) / 2;

        int menuW = Math.max(190, Math.round(contentW * 0.36f));
        menuRight = contentX + menuW;
        panelX = menuRight + COLUMN_GAP;
        panelW = contentX + contentW - panelX;

        headerY = Math.max(28, Math.round(height * 0.11f));
        bodyY = headerY + 54;
        footerY = height - Math.max(34, Math.round(height * 0.07f));

        separators.clear();
        buildMenu();
        buildPanelWidgets();
    }

    /**
     * Les entrees sont regroupees ; l'ecart entre elles est deduit de la place
     * disponible pour qu'aucune ne sorte de l'ecran.
     */
    private void buildMenu() {
        record Entry(String label, Runnable action, boolean enabled, boolean startsGroup) {}
        List<Entry> entries = List.of(
                new Entry("LANCER LA PARTIE", this::requestStart, ClientMatchState.canStart, false),
                new Entry("MODE DE JEU", () -> show(Panel.MODES), true, false),
                new Entry("CARTE", () -> show(Panel.MAPS), true, false),
                new Entry("CHOISIR UNE CLASSE",
                        () -> minecraft.setScreen(new ChooseClassScreen(this)), true, true),
                new Entry("KILLSTREAKS",
                        () -> minecraft.setScreen(new ClassScreen(ClassScreen.Tab.STREAKS, this)), true, false),
                new Entry("CLASSEMENT", () -> {
                    WarfareNetwork.CHANNEL.sendToServer(new WarfareNetwork.RequestLeaderboard());
                    show(Panel.RANKING);
                }, true, true),
                new Entry("MON PROFIL", () -> show(Panel.PROFILE), true, false),
                new Entry("JOUEURS", () -> show(Panel.PLAYERS), true, false),
                new Entry("REGLAGES DE PARTIE", () -> show(Panel.SETTINGS), true, false),
                new Entry("RETOUR AU JEU", this::onClose, true, true),
                new Entry("QUITTER LE SERVEUR", this::disconnect, true, false));

        int groups = (int) entries.stream().filter(Entry::startsGroup).count();
        int available = footerY - 14 - bodyY;
        int needed = entries.size();
        itemStep = Math.max(ITEM_STEP_MIN,
                Math.min(ITEM_STEP_MAX, (available - groups * 10) / Math.max(1, needed)));
        int itemHeight = Math.max(12, itemStep - 4);

        int menuW = menuRight - contentX;
        int y = bodyY;
        for (Entry entry : entries) {
            if (entry.startsGroup()) {
                separators.add(y + 4);
                y += 10;
            }
            addRenderableWidget(new MenuItem(contentX, y, menuW, itemHeight,
                    Component.literal(entry.label()), entry.action()).enabled(entry.enabled()));
            y += itemStep;
        }
    }

    private void show(Panel next) {
        panel = next;
        rebuildWidgets();
    }

    /** Les panneaux interactifs (modes, cartes, prestige) ont leurs propres boutons. */
    private void buildPanelWidgets() {
        int y = bodyY + 22;
        int buttonW = Math.min(260, panelW);
        switch (panel) {
            case MODES -> {
                for (GameMode mode : GameMode.values()) {
                    final GameMode target = mode;
                    WarButton button = new WarButton(panelX, y, buttonW, 18,
                            Component.literal(mode.title()),
                            () -> WarfareNetwork.CHANNEL.sendToServer(
                                    new WarfareNetwork.RequestMode(target.id())));
                    button.selected = mode.id().equals(ClientMatchState.mode);
                    addRenderableWidget(button);
                    y += 22;
                }
            }
            case MAPS -> {
                for (String arena : ClientMatchState.arenas) {
                    final String target = arena;
                    WarButton button = new WarButton(panelX, y, buttonW, 18,
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
                    addRenderableWidget(new WarButton(panelX, bodyY + 108, Math.min(220, panelW), 18,
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

    // ------------------------------------------------------------------- rendu

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fillGradient(0, 0, width, height, 0xF00A0B09, 0xFA060706);

        // Bandeau de titre
        Fonts.drawTracked(graphics, font, "MINECRAFT WARFARE", contentX, headerY, 0xFFFFFFFF, 3, 1.6f);
        graphics.fill(contentX, headerY + 26, contentX + contentW, headerY + 27, Palette.ACCENT);
        label(graphics, stateLabel(), contentX, headerY + 33, Palette.MUTED);
        String right = ClientMatchState.arena.isEmpty()
                ? "AUCUNE CARTE" : ClientMatchState.arena.toUpperCase(Locale.ROOT);
        graphics.drawString(font, Fonts.ui(right),
                contentX + contentW - Fonts.width(font, right), headerY + 33, Palette.MUTED, false);

        for (int separatorY : separators) {
            graphics.fill(contentX, separatorY, menuRight, separatorY + 1, Palette.RULE);
        }

        switch (panel) {
            case PLAYERS -> renderPlayers(graphics);
            case MODES -> renderModes(graphics);
            case MAPS -> renderMaps(graphics);
            case RANKING -> renderRanking(graphics);
            case PROFILE -> renderProfile(graphics);
            case SETTINGS -> renderSettings(graphics);
        }

        // Pied de page
        graphics.fill(contentX, footerY, contentX + contentW, footerY + 1, 0x55B9922F);
        label(graphics, "ECHAP ferme le menu     M le rouvre     C les classes",
                contentX, footerY + 8, Palette.MUTED);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    // ------------------------------------------------------------- panneaux

    private void renderPlayers(GuiGraphics graphics) {
        int y = panelHeader(graphics, "JOUEURS   " + ClientMatchState.players.size()
                + " / " + ClientMatchState.maxPlayers);
        if (ClientMatchState.players.isEmpty()) {
            body(graphics, "En attente de connexions", panelX, y + 4, Palette.MUTED);
            return;
        }
        for (WarfareNetwork.MatchState.Entry entry : ClientMatchState.players) {
            if (y + 22 > footerY - 6) break;
            boolean me = minecraft != null && minecraft.player != null
                    && entry.name().equals(minecraft.player.getGameProfile().getName());
            graphics.fill(panelX, y, panelX + panelW, y + 20, me ? 0x3AB9922F : 0x40000000);
            graphics.fill(panelX, y, panelX + 3, y + 20, teamColor(entry.team()));
            body(graphics, entry.name(), panelX + 12, y + 6, me ? Palette.ACCENT : Palette.TEXT);
            String value = ClientMatchState.running()
                    ? entry.score() + " ELIM." : "NIV. " + entry.score();
            body(graphics, value, panelX + panelW - 12 - Fonts.width(font, value), y + 6, Palette.MUTED);
            y += 23;
        }
    }

    private void renderModes(GuiGraphics graphics) {
        panelHeader(graphics, "MODE DE JEU");
        GameMode selected = GameMode.byId(ClientMatchState.mode);
        int y = bodyY + 22 + GameMode.values().length * 22 + 14;
        body(graphics, selected.title(), panelX, y, Palette.ACCENT);
        body(graphics, selected.description(), panelX, y + 14, Palette.TEXT);
        body(graphics, ClientMatchState.scoreLimit + " points     "
                + ClientMatchState.matchSeconds / 60 + " min", panelX, y + 28, Palette.MUTED);
    }

    private void renderMaps(GuiGraphics graphics) {
        panelHeader(graphics, "CARTE");
        if (ClientMatchState.arenas.isEmpty()) {
            body(graphics, "Aucune carte enregistree.", panelX, bodyY + 26, Palette.TEXT);
            body(graphics, "Un operateur doit faire", panelX, bodyY + 42, Palette.MUTED);
            body(graphics, "/warfare arena create <nom>", panelX, bodyY + 54, Palette.ACCENT);
            return;
        }
        int y = bodyY + 22 + ClientMatchState.arenas.size() * 22 + 12;
        body(graphics, "Carte active : " + ClientMatchState.arena.toUpperCase(Locale.ROOT),
                panelX, y, Palette.ACCENT);
    }

    private void renderRanking(GuiGraphics graphics) {
        int y = panelHeader(graphics, "CLASSEMENT DU SERVEUR");
        if (ClientMatchState.leaderboard.isEmpty()) {
            body(graphics, "Aucune partie enregistree.", panelX, y + 4, Palette.TEXT);
            return;
        }
        int rank = 1;
        for (WarfareNetwork.Leaderboard.Row row : ClientMatchState.leaderboard) {
            if (y + 16 > footerY - 6) break;
            if (rank % 2 == 1) graphics.fill(panelX, y - 3, panelX + panelW, y + 12, 0x26000000);
            body(graphics, "#" + rank, panelX + 6, y, Palette.MUTED);
            body(graphics, row.name(), panelX + 34, y, Palette.TEXT);

            String score = row.score() + " PTS";
            String kd = row.kills() + "/" + row.deaths();
            String level = "NIV." + row.level() + (row.prestige() > 0 ? " P" + row.prestige() : "");
            int rightEdge = panelX + panelW - 8;
            body(graphics, score, rightEdge - Fonts.width(font, score), y, Palette.MUTED);
            body(graphics, kd, rightEdge - 72 - Fonts.width(font, kd), y, Palette.TEXT);
            body(graphics, level, rightEdge - 130 - Fonts.width(font, level), y, Palette.ACCENT);
            y += 15;
            rank++;
        }
    }

    private void renderProfile(GuiGraphics graphics) {
        int y = panelHeader(graphics, "MON PROFIL");
        Fonts.drawTracked(graphics, font, Rank.label(ClientMatchState.level, ClientMatchState.prestige)
                .toUpperCase(Locale.ROOT), panelX, y, Palette.ACCENT, 1, 1.25f);
        y += 24;

        int barWidth = Math.min(260, panelW);
        long need = Math.max(1, ClientMatchState.xpToNext);
        int filled = (int) Math.min(barWidth, ClientMatchState.xpIntoLevel * barWidth / need);
        graphics.fill(panelX, y, panelX + barWidth, y + 7, 0x60000000);
        graphics.fill(panelX, y, panelX + filled, y + 7, Palette.ACCENT);
        y += 13;
        body(graphics, ClientMatchState.xpIntoLevel + " / " + need + " XP", panelX, y, Palette.MUTED);
        y += 24;
        body(graphics, ClientMatchState.level >= Rank.MAX_LEVEL
                        ? "Niveau maximum atteint."
                        : "Prochain grade : " + Rank.title(ClientMatchState.level + 1),
                panelX, y, Palette.TEXT);
    }

    private void renderSettings(GuiGraphics graphics) {
        int y = panelHeader(graphics, "REGLAGES DE PARTIE");
        String[][] rows = {
                {"Mode", ClientMatchState.modeTitle()},
                {"Carte", ClientMatchState.arena.isEmpty()
                        ? "aucune" : ClientMatchState.arena.toUpperCase(Locale.ROOT)},
                {"Score a atteindre", ClientMatchState.scoreLimit + " points"},
                {"Duree maximale", ClientMatchState.matchSeconds / 60 + " min "
                        + ClientMatchState.matchSeconds % 60 + " s"},
                {"Joueurs connectes", ClientMatchState.players.size() + " / " + ClientMatchState.maxPlayers},
        };
        for (String[] row : rows) {
            body(graphics, row[0], panelX, y, Palette.MUTED);
            body(graphics, row[1], panelX + panelW - Fonts.width(font, row[1]), y, Palette.TEXT);
            y += 13;
            graphics.fill(panelX, y, panelX + panelW, y + 1, 0x33FFFFFF);
            y += 11;
        }
        body(graphics, "Details dans config/warfare-common.toml", panelX, y + 8, Palette.MUTED);
    }

    // -------------------------------------------------------------- helpers

    /** Intitule de panneau : interlettre, souligne, et renvoie le haut du contenu. */
    private int panelHeader(GuiGraphics graphics, String text) {
        Fonts.drawTracked(graphics, font, text, panelX, bodyY - 16, Palette.MUTED, 2, 1.0f);
        graphics.fill(panelX, bodyY - 4, panelX + panelW, bodyY - 3, Palette.RULE);
        return bodyY + 6;
    }

    private void body(GuiGraphics graphics, String text, int x, int y, int color) {
        graphics.drawString(font, Fonts.ui(text), x, y, color, false);
    }

    private void label(GuiGraphics graphics, String text, int x, int y, int color) {
        graphics.drawString(font, Fonts.ui(text), x, y, color, false);
    }

    private static int teamColor(String team) {
        return switch (team) {
            case "alpha" -> 0xFFD24A3A;
            case "bravo" -> 0xFF3FA9C9;
            default -> 0x66FFFFFF;
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
