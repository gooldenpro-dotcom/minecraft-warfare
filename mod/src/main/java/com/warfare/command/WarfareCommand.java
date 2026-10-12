package com.warfare.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.warfare.WarfareConfig;
import com.warfare.game.Arena;
import com.warfare.game.GameManager;
import com.warfare.game.GameMode;
import com.warfare.game.GameState;
import com.warfare.game.Spot;
import com.warfare.game.Team;
import com.warfare.game.WarfareData;
import com.warfare.net.WarfareNetwork;
import com.warfare.progression.PlayerStats;
import com.warfare.progression.Rank;
import com.warfare.progression.StatsStore;
import com.warfare.util.Txt;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

/** /warfare : commandes d'administration (niveau operateur). */
public final class WarfareCommand {

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("warfare")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("start").executes(WarfareCommand::start))
                .then(Commands.literal("stop").executes(WarfareCommand::stop))
                .then(Commands.literal("mode")
                        .then(Commands.argument("mode", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(modeIds(), builder))
                                .executes(WarfareCommand::mode)))
                .then(Commands.literal("arena")
                        .then(Commands.literal("create")
                                .then(Commands.argument("nom", StringArgumentType.word())
                                        .executes(WarfareCommand::arenaCreate)))
                        .then(Commands.literal("select")
                                .then(Commands.argument("nom", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                WarfareData.get(ctx.getSource().getServer()).arenaNames(), builder))
                                        .executes(WarfareCommand::arenaSelect)))
                        .then(Commands.literal("delete")
                                .then(Commands.argument("nom", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                WarfareData.get(ctx.getSource().getServer()).arenaNames(), builder))
                                        .executes(WarfareCommand::arenaDelete)))
                        .then(Commands.literal("list").executes(WarfareCommand::arenaList)))
                .then(Commands.literal("setlobby").executes(WarfareCommand::setLobby))
                .then(Commands.literal("addspawn")
                        .executes(ctx -> addSpawn(ctx, Team.NONE))
                        .then(Commands.literal("alpha").executes(ctx -> addSpawn(ctx, Team.ALPHA)))
                        .then(Commands.literal("bravo").executes(ctx -> addSpawn(ctx, Team.BRAVO))))
                .then(Commands.literal("listspawns").executes(WarfareCommand::listSpawns))
                .then(Commands.literal("removespawn")
                        .then(Commands.argument("index", IntegerArgumentType.integer(1))
                                .executes(WarfareCommand::removeSpawn)))
                .then(Commands.literal("stats")
                        .executes(ctx -> stats(ctx, ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("joueur", EntityArgument.player())
                                .executes(ctx -> stats(ctx, EntityArgument.getPlayer(ctx, "joueur")))))
                .then(Commands.literal("top").executes(WarfareCommand::top))
                .then(Commands.literal("xp")
                        .then(Commands.argument("joueur", EntityArgument.player())
                                .then(Commands.argument("montant", IntegerArgumentType.integer(1))
                                        .executes(WarfareCommand::giveXp))))
                .then(Commands.literal("setlevel")
                        .then(Commands.argument("joueur", EntityArgument.player())
                                .then(Commands.argument("niveau",
                                                IntegerArgumentType.integer(1, Rank.MAX_LEVEL))
                                        .executes(WarfareCommand::setLevel))))
                .then(Commands.literal("reload").executes(WarfareCommand::reload));
        event.getDispatcher().register(root);
    }

    private static List<String> modeIds() {
        List<String> ids = new ArrayList<>();
        for (GameMode mode : GameMode.values()) ids.add(mode.id());
        return ids;
    }

    // ------------------------------------------------------------- parties

    private static int start(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        MinecraftServer server = source.getServer();
        GameManager game = GameManager.get();

        if (game.state() != GameState.LOBBY) {
            source.sendFailure(Component.literal("Une partie est deja en cours (etat : " + game.state() + ")."));
            return 0;
        }
        if (!WarfareData.get(server).isReadyFor(game.mode())) {
            source.sendFailure(Component.literal("Carte incomplete pour le mode " + game.mode().title()
                    + " : il faut /warfare setlobby et au moins un /warfare addspawn"
                    + (game.mode().teamBased() ? " alpha et un bravo." : ".")));
            return 0;
        }
        game.startCountdown(server);
        source.sendSuccess(() -> Txt.info(Txt.c("Compte a rebours lance.", ChatFormatting.GREEN)), true);
        return 1;
    }

    private static int stop(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        GameManager.get().abort(source.getServer());
        source.sendSuccess(() -> Txt.info(Txt.c("Partie arretee, retour au menu.", ChatFormatting.YELLOW)), true);
        return 1;
    }

    private static int mode(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        GameMode mode = GameMode.byId(StringArgumentType.getString(ctx, "mode"));
        return GameManager.get().setMode(source.getServer(), source.getPlayer(), mode) ? 1 : 0;
    }

    // -------------------------------------------------------------- arenes

    private static int arenaCreate(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayerOrException();
        String name = WarfareData.key(StringArgumentType.getString(ctx, "nom"));
        WarfareData data = WarfareData.get(source.getServer());
        Arena arena = data.getOrCreate(name);
        arena.setLobby(Spot.of(player));
        data.select(name);
        data.touch();
        source.sendSuccess(() -> Txt.info(Txt.c("Carte \"" + name
                + "\" creee, lobby place ici. Ajoute des points avec /warfare addspawn.",
                ChatFormatting.GREEN)), true);
        return 1;
    }

    private static int arenaSelect(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String name = StringArgumentType.getString(ctx, "nom");
        return GameManager.get().setArena(source.getServer(), source.getPlayer(), name) ? 1 : 0;
    }

    private static int arenaDelete(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String name = StringArgumentType.getString(ctx, "nom");
        if (!WarfareData.get(source.getServer()).delete(name)) {
            source.sendFailure(Component.literal("Carte inconnue : " + name));
            return 0;
        }
        source.sendSuccess(() -> Txt.info(Txt.c("Carte \"" + name + "\" supprimee.",
                ChatFormatting.YELLOW)), true);
        return 1;
    }

    private static int arenaList(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        WarfareData data = WarfareData.get(source.getServer());
        if (data.arenas().isEmpty()) {
            source.sendSuccess(() -> Txt.info(Txt.c("Aucune carte. /warfare arena create <nom>",
                    ChatFormatting.RED)), false);
            return 0;
        }
        for (Arena arena : data.arenas()) {
            boolean current = arena.name().equals(data.currentName());
            source.sendSuccess(() -> Component.empty()
                    .append(Txt.c(current ? "> " : "  ", ChatFormatting.GOLD))
                    .append(Txt.c(arena.name(), current ? ChatFormatting.GOLD : ChatFormatting.WHITE))
                    .append(Txt.c("  " + arena.spawns().size() + " point(s)", ChatFormatting.GRAY))
                    .append(Txt.c("  alpha " + arena.count(Team.ALPHA)
                            + " / bravo " + arena.count(Team.BRAVO), ChatFormatting.DARK_GRAY))
                    .append(Txt.c(arena.isReady() ? "  pret" : "  incomplete",
                            arena.isReady() ? ChatFormatting.GREEN : ChatFormatting.RED)), false);
        }
        return data.arenas().size();
    }

    private static int setLobby(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayerOrException();
        WarfareData data = WarfareData.get(source.getServer());
        Arena arena = data.current();
        if (arena == null) {
            source.sendFailure(Component.literal("Aucune carte : /warfare arena create <nom>"));
            return 0;
        }
        Spot spot = Spot.of(player);
        arena.setLobby(spot);
        data.touch();
        source.sendSuccess(() -> Txt.info(Txt.c("Lobby de " + arena.name() + " place ici : "
                + spot.describe(), ChatFormatting.GREEN)), true);
        return 1;
    }

    private static int addSpawn(CommandContext<CommandSourceStack> ctx, Team team)
            throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayerOrException();
        WarfareData data = WarfareData.get(source.getServer());
        Arena arena = data.current();
        if (arena == null) {
            source.sendFailure(Component.literal("Aucune carte : /warfare arena create <nom>"));
            return 0;
        }
        Spot spot = Spot.of(player);
        int count = arena.add(team, spot);
        data.touch();
        source.sendSuccess(() -> Txt.info(Txt.c("Point #" + count + " ("
                + (team == Team.NONE ? "libre" : team.title()) + ") ajoute a " + arena.name()
                + " : " + spot.describe(), ChatFormatting.GREEN)), true);
        return 1;
    }

    private static int listSpawns(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        WarfareData data = WarfareData.get(source.getServer());
        Arena arena = data.current();
        if (arena == null) {
            source.sendSuccess(() -> Txt.info(Txt.c("Aucune carte.", ChatFormatting.RED)), false);
            return 0;
        }
        source.sendSuccess(() -> Txt.info(Txt.c("Carte " + arena.name() + " - lobby : "
                + (arena.lobby() == null ? "non defini" : arena.lobby().describe()),
                arena.lobby() == null ? ChatFormatting.RED : ChatFormatting.WHITE)), false);
        List<Arena.Point> spawns = arena.spawns();
        if (spawns.isEmpty()) {
            source.sendSuccess(() -> Txt.info(Txt.c("Aucun point d'apparition.", ChatFormatting.RED)), false);
            return 0;
        }
        for (int i = 0; i < spawns.size(); i++) {
            final int index = i + 1;
            final Arena.Point point = spawns.get(i);
            source.sendSuccess(() -> Component.empty()
                    .append(Txt.c("#" + index + " ", ChatFormatting.GOLD))
                    .append(Txt.c(point.team() == Team.NONE ? "libre" : point.team().title(),
                            point.team() == Team.NONE ? ChatFormatting.GRAY : point.team().chat()))
                    .append(Txt.c("  " + point.spot().describe(), ChatFormatting.GRAY)), false);
        }
        return spawns.size();
    }

    private static int removeSpawn(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        int index = IntegerArgumentType.getInteger(ctx, "index");
        WarfareData data = WarfareData.get(source.getServer());
        Arena arena = data.current();
        if (arena == null || !arena.remove(index - 1)) {
            source.sendFailure(Component.literal("Aucun point d'apparition #" + index + "."));
            return 0;
        }
        data.touch();
        source.sendSuccess(() -> Txt.info(Txt.c("Point d'apparition #" + index + " supprime.",
                ChatFormatting.YELLOW)), true);
        return 1;
    }

    // ---------------------------------------------------------- progression

    private static int stats(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        CommandSourceStack source = ctx.getSource();
        PlayerStats stats = StatsStore.of(target);
        source.sendSuccess(() -> Txt.info(Txt.c(target.getGameProfile().getName() + " - "
                + Rank.label(stats.level, stats.prestige), ChatFormatting.GOLD)), false);
        source.sendSuccess(() -> Txt.c("XP " + stats.xp + "  (" + Rank.intoLevel(stats.xp, stats.level)
                + "/" + Rank.xpToNext(stats.level) + " vers le niveau suivant)", ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Txt.c(stats.kills + " elim.  " + stats.deaths + " morts  ratio "
                + String.format("%.2f", stats.kd()), ChatFormatting.WHITE), false);
        source.sendSuccess(() -> Txt.c(stats.wins + " victoires / " + stats.matches + " parties  -  "
                + "meilleure serie " + stats.bestStreak, ChatFormatting.WHITE), false);
        return 1;
    }

    private static int top(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        List<StatsStore.Row> rows = StatsStore.leaderboard(source.getServer(), 10);
        if (rows.isEmpty()) {
            source.sendSuccess(() -> Txt.info(Txt.c("Aucune statistique enregistree.",
                    ChatFormatting.GRAY)), false);
            return 0;
        }
        source.sendSuccess(() -> Txt.c("--- Classement du serveur ---", ChatFormatting.GOLD,
                ChatFormatting.BOLD), false);
        int rank = 1;
        for (StatsStore.Row row : rows) {
            final int position = rank++;
            source.sendSuccess(() -> Component.empty()
                    .append(Txt.c("#" + position + " ", ChatFormatting.GRAY))
                    .append(Txt.c(row.name(), ChatFormatting.WHITE))
                    .append(Txt.c("  niv." + row.level()
                            + (row.prestige() > 0 ? " P" + row.prestige() : ""), ChatFormatting.GOLD))
                    .append(Txt.c("  " + row.kills() + "/" + row.deaths(), ChatFormatting.GREEN))
                    .append(Txt.c("  " + row.score() + " pts", ChatFormatting.DARK_GRAY)), false);
        }
        return rows.size();
    }

    private static int giveXp(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer target = EntityArgument.getPlayer(ctx, "joueur");
        int amount = IntegerArgumentType.getInteger(ctx, "montant");
        GameManager.get().awardXp(target, amount, "BONUS");
        StatsStore.save(source.getServer(), target.getUUID());
        source.sendSuccess(() -> Txt.info(Txt.c(amount + " XP donnes a "
                + target.getGameProfile().getName() + ".", ChatFormatting.GREEN)), true);
        return 1;
    }

    private static int setLevel(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer target = EntityArgument.getPlayer(ctx, "joueur");
        int level = IntegerArgumentType.getInteger(ctx, "niveau");
        PlayerStats stats = StatsStore.of(target);
        stats.xp = Rank.totalFor(level);
        stats.level = level;
        StatsStore.save(source.getServer(), target.getUUID());
        WarfareNetwork.sync(target, GameManager.get().isMenuAllowed(target.getUUID()));
        source.sendSuccess(() -> Txt.info(Txt.c(target.getGameProfile().getName()
                + " passe au niveau " + level + ".", ChatFormatting.GREEN)), true);
        return 1;
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        WarfareConfig.reload();
        ctx.getSource().sendSuccess(() -> Txt.info(Txt.c("Configuration rechargee (mode "
                + GameManager.get().mode().title() + ", " + GameManager.get().mode().scoreLimit()
                + " points, " + GameManager.get().mode().seconds() + " s).", ChatFormatting.GREEN)), true);
        return 1;
    }

    private WarfareCommand() {}
}
