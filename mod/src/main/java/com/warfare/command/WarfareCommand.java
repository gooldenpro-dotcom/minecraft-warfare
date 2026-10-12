package com.warfare.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.warfare.WarfareConfig;
import com.warfare.game.GameManager;
import com.warfare.game.GameState;
import com.warfare.game.WarfareData;
import com.warfare.util.Txt;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;

/** /warfare : commandes d'administration (niveau operateur). */
public final class WarfareCommand {

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("warfare")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("start").executes(WarfareCommand::start))
                .then(Commands.literal("stop").executes(WarfareCommand::stop))
                .then(Commands.literal("setlobby").executes(WarfareCommand::setLobby))
                .then(Commands.literal("addspawn").executes(WarfareCommand::addSpawn))
                .then(Commands.literal("listspawns").executes(WarfareCommand::listSpawns))
                .then(Commands.literal("removespawn")
                        .then(Commands.argument("index", IntegerArgumentType.integer(1))
                                .executes(WarfareCommand::removeSpawn)))
                .then(Commands.literal("reload").executes(WarfareCommand::reload));
        event.getDispatcher().register(root);
    }

    private static int start(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        MinecraftServer server = source.getServer();
        GameManager game = GameManager.get();

        if (game.state() != GameState.LOBBY) {
            source.sendFailure(Component.literal("Une partie est deja en cours (etat : " + game.state() + ")."));
            return 0;
        }
        if (!WarfareData.get(server).isReady()) {
            source.sendFailure(Component.literal(
                    "Arene incomplete : il faut /warfare setlobby et au moins un /warfare addspawn."));
            return 0;
        }
        game.startCountdown(server);
        source.sendSuccess(() -> Txt.info(Txt.c("Compte a rebours lance.", ChatFormatting.GREEN)), true);
        return 1;
    }

    private static int stop(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        GameManager.get().abort(source.getServer());
        source.sendSuccess(() -> Txt.info(Txt.c("Partie arretee, retour au lobby.", ChatFormatting.YELLOW)), true);
        return 1;
    }

    private static int setLobby(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx)
            throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayerOrException();
        WarfareData.Spot spot = WarfareData.Spot.of(player);
        WarfareData.get(source.getServer()).setLobby(spot);
        source.sendSuccess(() -> Txt.info(Txt.c("Lobby place ici : " + spot.describe(), ChatFormatting.GREEN)), true);
        return 1;
    }

    private static int addSpawn(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx)
            throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayerOrException();
        WarfareData.Spot spot = WarfareData.Spot.of(player);
        int count = WarfareData.get(source.getServer()).addSpawn(spot);
        source.sendSuccess(() -> Txt.info(Txt.c("Point d'apparition #" + count + " ajoute : "
                + spot.describe(), ChatFormatting.GREEN)), true);
        return 1;
    }

    private static int listSpawns(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        WarfareData data = WarfareData.get(source.getServer());
        List<WarfareData.Spot> spawns = data.getSpawns();

        source.sendSuccess(() -> Txt.info(Txt.c("Lobby : "
                + (data.getLobby() == null ? "non defini" : data.getLobby().describe()),
                data.getLobby() == null ? ChatFormatting.RED : ChatFormatting.WHITE)), false);
        if (spawns.isEmpty()) {
            source.sendSuccess(() -> Txt.info(Txt.c("Aucun point d'apparition.", ChatFormatting.RED)), false);
            return 0;
        }
        for (int i = 0; i < spawns.size(); i++) {
            final int index = i + 1;
            final WarfareData.Spot spot = spawns.get(i);
            source.sendSuccess(() -> Component.empty()
                    .append(Txt.c("#" + index + " ", ChatFormatting.GOLD))
                    .append(Txt.c(spot.describe(), ChatFormatting.GRAY)), false);
        }
        return spawns.size();
    }

    private static int removeSpawn(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        int index = IntegerArgumentType.getInteger(ctx, "index");
        if (!WarfareData.get(source.getServer()).removeSpawn(index - 1)) {
            source.sendFailure(Component.literal("Aucun point d'apparition #" + index + "."));
            return 0;
        }
        source.sendSuccess(() -> Txt.info(Txt.c("Point d'apparition #" + index + " supprime.",
                ChatFormatting.YELLOW)), true);
        return 1;
    }

    private static int reload(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        WarfareConfig.reload();
        ctx.getSource().sendSuccess(() -> Txt.info(Txt.c("Configuration rechargee ("
                + WarfareConfig.killLimit + " elim. / " + WarfareConfig.matchSeconds + " s / "
                + WarfareConfig.primaryGun + " + " + WarfareConfig.secondaryGun + ").",
                ChatFormatting.GREEN)), true);
        return 1;
    }

    private WarfareCommand() {}
}
