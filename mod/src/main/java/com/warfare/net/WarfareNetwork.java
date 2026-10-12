package com.warfare.net;

import com.warfare.Warfare;
import com.warfare.WarfareConfig;
import com.warfare.client.ClientClassData;
import com.warfare.game.GameManager;
import com.warfare.game.GameMode;
import com.warfare.game.WarfareData;
import com.warfare.loadout.ClassLoadout;
import com.warfare.loadout.ClassStore;
import com.warfare.loadout.GunCatalog;
import com.warfare.loadout.PlayerClasses;
import com.warfare.progression.PlayerStats;
import com.warfare.progression.Rank;
import com.warfare.progression.StatsStore;
import com.warfare.progression.Unlocks;
import com.warfare.util.Txt;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Canal reseau du mod.
 *
 * Le client n'envoie que des intentions (enregistrer une classe, lancer la partie,
 * changer de mode) ; le serveur valide tout avant d'ecrire quoi que ce soit,
 * puis renvoie l'etat reel au client.
 */
public final class WarfareNetwork {
    /**
     * A INCREMENTER a chaque ajout, suppression ou changement d'ordre de paquet.
     * Sans ca, un client d'une ancienne version passe la poignee de main puis decode
     * les paquets avec les mauvais identifiants et se deconnecte sans message clair.
     */
    private static final String PROTOCOL = "5";

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(Warfare.MOD_ID, "main"))
            .clientAcceptedVersions(PROTOCOL::equals)
            .serverAcceptedVersions(PROTOCOL::equals)
            .networkProtocolVersion(() -> PROTOCOL)
            .simpleChannel();

    public static void register() {
        // Les identifiants doivent rester stables : tout nouveau paquet s'ajoute A LA FIN.
        int id = 0;
        CHANNEL.registerMessage(id++, SyncClasses.class, SyncClasses::encode, SyncClasses::decode,
                SyncClasses::handle);
        CHANNEL.registerMessage(id++, SaveClass.class, SaveClass::encode, SaveClass::decode,
                SaveClass::handle);
        CHANNEL.registerMessage(id++, SelectClass.class, SelectClass::encode, SelectClass::decode,
                SelectClass::handle);
        CHANNEL.registerMessage(id++, MatchState.class, MatchState::encode, MatchState::decode,
                MatchState::handle);
        CHANNEL.registerMessage(id++, RequestStart.class, RequestStart::encode, RequestStart::decode,
                RequestStart::handle);
        CHANNEL.registerMessage(id++, RequestMode.class, RequestMode::encode, RequestMode::decode,
                RequestMode::handle);
        CHANNEL.registerMessage(id++, RequestArena.class, RequestArena::encode, RequestArena::decode,
                RequestArena::handle);
        CHANNEL.registerMessage(id++, ScoreEvent.class, ScoreEvent::encode, ScoreEvent::decode,
                ScoreEvent::handle);
        CHANNEL.registerMessage(id++, Leaderboard.class, Leaderboard::encode, Leaderboard::decode,
                Leaderboard::handle);
        CHANNEL.registerMessage(id++, RequestLeaderboard.class, RequestLeaderboard::encode,
                RequestLeaderboard::decode, RequestLeaderboard::handle);
        CHANNEL.registerMessage(id, RequestPrestige.class, RequestPrestige::encode,
                RequestPrestige::decode, RequestPrestige::handle);
    }

    /** Envoie au joueur ses classes, le catalogue autorise et son niveau de deblocage. */
    public static void sync(ServerPlayer player, boolean menuAllowed) {
        PlayerClasses classes = ClassStore.get(player.server, player.getUUID());
        List<String> allowedGuns = new ArrayList<>();
        for (GunCatalog.Entry entry : GunCatalog.all()) allowedGuns.add(entry.gunId());
        PlayerStats stats = StatsStore.of(player);
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SyncClasses(classes, allowedGuns, menuAllowed, Unlocks.effectiveLevel(stats),
                        stats.level, stats.prestige));
    }

    /** Points qui montent a l'ecran ("+100 ELIMINATION"). */
    public static void score(ServerPlayer player, int points, String label) {
        if (!WarfareConfig.scorePopups) return;
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ScoreEvent(points, label));
    }

    public static void leaderboard(ServerPlayer player) {
        List<Leaderboard.Row> rows = new ArrayList<>();
        for (StatsStore.Row row : StatsStore.leaderboard(player.server, 20)) {
            rows.add(new Leaderboard.Row(row.name(), row.level(), row.prestige(), row.kills(),
                    row.deaths(), row.wins(), row.score()));
        }
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Leaderboard(rows));
    }

    // ------------------------------------------------------------ serveur -> client

    /** Etat complet pour les menus et l'ATH, pousse a chaque joueur plusieurs fois par seconde. */
    public record MatchState(String state, String mode, String arena, List<String> arenas,
                             int secondsLeft, int maxPlayers, boolean canStart,
                             int scoreLimit, int matchSeconds, int alphaScore, int bravoScore,
                             String myTeam, int myScore, int myStreak, int nextStreakAt, int myLives,
                             int ladderStep, int ladderSize,
                             int level, int prestige, long xpIntoLevel, long xpToNext,
                             List<Entry> players) {

        public record Entry(String name, int score, String team) {}

        public static void encode(MatchState msg, FriendlyByteBuf buf) {
            buf.writeUtf(msg.state, 16);
            buf.writeUtf(msg.mode, 32);
            buf.writeUtf(msg.arena, 64);
            buf.writeVarInt(msg.arenas.size());
            for (String arena : msg.arenas) buf.writeUtf(arena, 64);
            buf.writeVarInt(msg.secondsLeft);
            buf.writeVarInt(msg.maxPlayers);
            buf.writeBoolean(msg.canStart);
            buf.writeVarInt(msg.scoreLimit);
            buf.writeVarInt(msg.matchSeconds);
            buf.writeVarInt(msg.alphaScore);
            buf.writeVarInt(msg.bravoScore);
            buf.writeUtf(msg.myTeam, 16);
            buf.writeVarInt(msg.myScore);
            buf.writeVarInt(msg.myStreak);
            buf.writeVarInt(msg.nextStreakAt);
            buf.writeVarInt(msg.myLives);
            buf.writeVarInt(msg.ladderStep);
            buf.writeVarInt(msg.ladderSize);
            buf.writeVarInt(msg.level);
            buf.writeVarInt(msg.prestige);
            buf.writeVarLong(msg.xpIntoLevel);
            buf.writeVarLong(msg.xpToNext);
            buf.writeVarInt(msg.players.size());
            for (Entry entry : msg.players) {
                buf.writeUtf(entry.name(), 32);
                buf.writeVarInt(entry.score());
                buf.writeUtf(entry.team(), 16);
            }
        }

        public static MatchState decode(FriendlyByteBuf buf) {
            String state = buf.readUtf(16);
            String mode = buf.readUtf(32);
            String arena = buf.readUtf(64);
            int arenaCount = buf.readVarInt();
            List<String> arenas = new ArrayList<>(arenaCount);
            for (int i = 0; i < arenaCount; i++) arenas.add(buf.readUtf(64));
            int seconds = buf.readVarInt();
            int max = buf.readVarInt();
            boolean canStart = buf.readBoolean();
            int scoreLimit = buf.readVarInt();
            int matchSeconds = buf.readVarInt();
            int alpha = buf.readVarInt();
            int bravo = buf.readVarInt();
            String myTeam = buf.readUtf(16);
            int myScore = buf.readVarInt();
            int myStreak = buf.readVarInt();
            int nextStreakAt = buf.readVarInt();
            int myLives = buf.readVarInt();
            int ladderStep = buf.readVarInt();
            int ladderSize = buf.readVarInt();
            int level = buf.readVarInt();
            int prestige = buf.readVarInt();
            long xpInto = buf.readVarLong();
            long xpNext = buf.readVarLong();
            int size = buf.readVarInt();
            List<Entry> players = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                players.add(new Entry(buf.readUtf(32), buf.readVarInt(), buf.readUtf(16)));
            }
            return new MatchState(state, mode, arena, arenas, seconds, max, canStart, scoreLimit,
                    matchSeconds, alpha, bravo, myTeam, myScore, myStreak, nextStreakAt, myLives,
                    ladderStep, ladderSize, level, prestige, xpInto, xpNext, players);
        }

        public static void handle(MatchState msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> com.warfare.client.ClientMatchState.accept(msg)));
            ctx.get().setPacketHandled(true);
        }
    }

    public record ScoreEvent(int points, String label) {
        public static void encode(ScoreEvent msg, FriendlyByteBuf buf) {
            buf.writeVarInt(msg.points);
            buf.writeUtf(msg.label, 32);
        }

        public static ScoreEvent decode(FriendlyByteBuf buf) {
            return new ScoreEvent(buf.readVarInt(), buf.readUtf(32));
        }

        public static void handle(ScoreEvent msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> com.warfare.client.ScorePopups.add(msg.points, msg.label)));
            ctx.get().setPacketHandled(true);
        }
    }

    /** Classement du serveur, envoye a la demande et a la fin de chaque partie. */
    public record Leaderboard(List<Row> rows) {
        public record Row(String name, int level, int prestige, int kills, int deaths, int wins, long score) {}

        public static void encode(Leaderboard msg, FriendlyByteBuf buf) {
            buf.writeVarInt(msg.rows.size());
            for (Row row : msg.rows) {
                buf.writeUtf(row.name(), 32);
                buf.writeVarInt(row.level());
                buf.writeVarInt(row.prestige());
                buf.writeVarInt(row.kills());
                buf.writeVarInt(row.deaths());
                buf.writeVarInt(row.wins());
                buf.writeVarLong(row.score());
            }
        }

        public static Leaderboard decode(FriendlyByteBuf buf) {
            int size = buf.readVarInt();
            List<Row> rows = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                rows.add(new Row(buf.readUtf(32), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                        buf.readVarInt(), buf.readVarInt(), buf.readVarLong()));
            }
            return new Leaderboard(rows);
        }

        public static void handle(Leaderboard msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> com.warfare.client.ClientMatchState.acceptLeaderboard(msg.rows())));
            ctx.get().setPacketHandled(true);
        }
    }

    public record SyncClasses(PlayerClasses classes, List<String> allowedGuns, boolean menuAllowed,
                              int unlockLevel, int level, int prestige) {
        public static void encode(SyncClasses msg, FriendlyByteBuf buf) {
            msg.classes.write(buf);
            buf.writeVarInt(msg.allowedGuns.size());
            for (String gun : msg.allowedGuns) buf.writeUtf(gun, 128);
            buf.writeBoolean(msg.menuAllowed);
            buf.writeVarInt(msg.unlockLevel);
            buf.writeVarInt(msg.level);
            buf.writeVarInt(msg.prestige);
        }

        public static SyncClasses decode(FriendlyByteBuf buf) {
            PlayerClasses classes = PlayerClasses.read(buf);
            int size = buf.readVarInt();
            List<String> guns = new ArrayList<>(size);
            for (int i = 0; i < size; i++) guns.add(buf.readUtf(128));
            return new SyncClasses(classes, guns, buf.readBoolean(), buf.readVarInt(),
                    buf.readVarInt(), buf.readVarInt());
        }

        public static void handle(SyncClasses msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ClientClassData.accept(msg)));
            ctx.get().setPacketHandled(true);
        }
    }

    // ------------------------------------------------------------ client -> serveur

    /** Bouton LANCER LA PARTIE : le serveur revalide la permission. */
    public record RequestStart() {
        public static void encode(RequestStart msg, FriendlyByteBuf buf) {}

        public static RequestStart decode(FriendlyByteBuf buf) {
            return new RequestStart();
        }

        public static void handle(RequestStart msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null || !mayControl(player)) return;
                GameManager.get().requestStart(player);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    /** Choix du mode de jeu depuis le menu. */
    public record RequestMode(String mode) {
        public static void encode(RequestMode msg, FriendlyByteBuf buf) {
            buf.writeUtf(msg.mode, 32);
        }

        public static RequestMode decode(FriendlyByteBuf buf) {
            return new RequestMode(buf.readUtf(32));
        }

        public static void handle(RequestMode msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null || !mayControl(player)) return;
                GameManager.get().setMode(player.server, player, GameMode.byId(msg.mode));
            });
            ctx.get().setPacketHandled(true);
        }
    }

    /** Choix de la carte (arene) depuis le menu. */
    public record RequestArena(String arena) {
        public static void encode(RequestArena msg, FriendlyByteBuf buf) {
            buf.writeUtf(msg.arena, 64);
        }

        public static RequestArena decode(FriendlyByteBuf buf) {
            return new RequestArena(buf.readUtf(64));
        }

        public static void handle(RequestArena msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null || !mayControl(player)) return;
                GameManager.get().setArena(player.server, player, msg.arena);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public record RequestLeaderboard() {
        public static void encode(RequestLeaderboard msg, FriendlyByteBuf buf) {}

        public static RequestLeaderboard decode(FriendlyByteBuf buf) {
            return new RequestLeaderboard();
        }

        public static void handle(RequestLeaderboard msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) leaderboard(player);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    /** Passage au prestige, uniquement au niveau maximum et hors partie. */
    public record RequestPrestige() {
        public static void encode(RequestPrestige msg, FriendlyByteBuf buf) {}

        public static RequestPrestige decode(FriendlyByteBuf buf) {
            return new RequestPrestige();
        }

        public static void handle(RequestPrestige msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null) return;
                if (GameManager.get().isRunning()) {
                    player.sendSystemMessage(Txt.info(Txt.c("Impossible en pleine partie.", ChatFormatting.RED)));
                    return;
                }
                PlayerStats stats = StatsStore.of(player);
                if (!stats.prestige()) {
                    player.sendSystemMessage(Txt.info(Txt.c("Niveau " + Rank.MAX_LEVEL
                            + " requis (prestige " + stats.prestige + "/" + Rank.MAX_PRESTIGE + ").",
                            ChatFormatting.RED)));
                    return;
                }
                StatsStore.save(player.server, player.getUUID());
                player.server.getPlayerList().broadcastSystemMessage(Txt.info(Txt.c(
                        player.getGameProfile().getName() + " passe au prestige " + stats.prestige + " !",
                        ChatFormatting.GOLD, ChatFormatting.BOLD)), false);
                sync(player, true);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public record SaveClass(int slot, ClassLoadout loadout) {
        public static void encode(SaveClass msg, FriendlyByteBuf buf) {
            buf.writeVarInt(msg.slot);
            msg.loadout.write(buf);
        }

        public static SaveClass decode(FriendlyByteBuf buf) {
            return new SaveClass(buf.readVarInt(), ClassLoadout.read(buf));
        }

        public static void handle(SaveClass msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null) return;
                if (msg.slot < 0 || msg.slot >= PlayerClasses.SLOTS) return;

                PlayerClasses classes = ClassStore.get(player.server, player.getUUID());
                int level = Unlocks.effectiveLevel(StatsStore.of(player));
                ClassLoadout safe = ClassStore.sanitize(msg.loadout, msg.slot, level);
                classes.set(msg.slot, safe);
                ClassStore.save(player.server, player.getUUID());
                sync(player, true);
                player.sendSystemMessage(Txt.info(Txt.c("Classe \"" + safe.name() + "\" enregistree.",
                        ChatFormatting.GREEN)));
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public record SelectClass(int slot) {
        public static void encode(SelectClass msg, FriendlyByteBuf buf) {
            buf.writeVarInt(msg.slot);
        }

        public static SelectClass decode(FriendlyByteBuf buf) {
            return new SelectClass(buf.readVarInt());
        }

        public static void handle(SelectClass msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null) return;
                if (msg.slot < 0 || msg.slot >= PlayerClasses.SLOTS) return;

                PlayerClasses classes = ClassStore.get(player.server, player.getUUID());
                classes.select(msg.slot);
                ClassStore.save(player.server, player.getUUID());
                sync(player, true);
                player.sendSystemMessage(Txt.info(Txt.c("Classe \"" + classes.current().name()
                        + "\" equipee a la prochaine reapparition.", ChatFormatting.YELLOW)));
            });
            ctx.get().setPacketHandled(true);
        }
    }

    /**
     * Qui peut piloter la partie : les operateurs, et tous les joueurs si la config
     * l'autorise (serveur prive entre amis).
     */
    private static boolean mayControl(ServerPlayer player) {
        if (player.hasPermissions(2) || WarfareConfig.playersCanStart) return true;
        player.sendSystemMessage(Txt.info(Txt.c("Reserve aux operateurs.", ChatFormatting.RED)));
        return false;
    }

    /** Vrai si ce joueur peut changer le mode, la carte et lancer la partie. */
    public static boolean canControl(ServerPlayer player) {
        return player.hasPermissions(2) || WarfareConfig.playersCanStart;
    }

    /** Diffuse le classement a tout le monde (fin de partie). */
    public static void leaderboardToAll(net.minecraft.server.MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) leaderboard(player);
    }

    private WarfareNetwork() {}
}
