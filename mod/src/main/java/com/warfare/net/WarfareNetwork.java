package com.warfare.net;

import com.warfare.Warfare;
import com.warfare.client.ClientClassData;
import com.warfare.loadout.ClassLoadout;
import com.warfare.loadout.ClassStore;
import com.warfare.loadout.GunCatalog;
import com.warfare.loadout.PlayerClasses;
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
 * Le client n'envoie que des intentions (enregistrer une classe, en equiper une) ;
 * le serveur valide tout dans ClassStore.sanitize avant d'ecrire quoi que ce soit,
 * puis renvoie l'etat reel au client.
 */
public final class WarfareNetwork {
    private static final String PROTOCOL = "2";

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(Warfare.MOD_ID, "main"))
            .clientAcceptedVersions(PROTOCOL::equals)
            .serverAcceptedVersions(PROTOCOL::equals)
            .networkProtocolVersion(() -> PROTOCOL)
            .simpleChannel();

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, MatchState.class, MatchState::encode, MatchState::decode,
                MatchState::handle);
        CHANNEL.registerMessage(id++, RequestStart.class, RequestStart::encode, RequestStart::decode,
                RequestStart::handle);
        CHANNEL.registerMessage(id++, SyncClasses.class, SyncClasses::encode, SyncClasses::decode,
                SyncClasses::handle);
        CHANNEL.registerMessage(id++, SaveClass.class, SaveClass::encode, SaveClass::decode,
                SaveClass::handle);
        CHANNEL.registerMessage(id, SelectClass.class, SelectClass::encode, SelectClass::decode,
                SelectClass::handle);
    }

    /** Envoie au joueur ses classes, le catalogue autorise et le droit d'ouvrir le menu. */
    public static void sync(ServerPlayer player, boolean menuAllowed) {
        PlayerClasses classes = ClassStore.get(player.server, player.getUUID());
        List<String> allowedGuns = new ArrayList<>();
        for (GunCatalog.Entry entry : GunCatalog.all()) allowedGuns.add(entry.gunId());
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SyncClasses(classes, allowedGuns, menuAllowed));
    }

    /** Etat de la partie, pousse a tous les joueurs une fois par seconde. */
    public record MatchState(String state, int secondsLeft, int maxPlayers, boolean canStart,
                             String mapName, List<Entry> players) {

        public record Entry(String name, int kills) {}

        public static void encode(MatchState msg, FriendlyByteBuf buf) {
            buf.writeUtf(msg.state, 16);
            buf.writeVarInt(msg.secondsLeft);
            buf.writeVarInt(msg.maxPlayers);
            buf.writeBoolean(msg.canStart);
            buf.writeUtf(msg.mapName, 64);
            buf.writeVarInt(msg.players.size());
            for (Entry entry : msg.players) {
                buf.writeUtf(entry.name(), 32);
                buf.writeVarInt(entry.kills());
            }
        }

        public static MatchState decode(FriendlyByteBuf buf) {
            String state = buf.readUtf(16);
            int seconds = buf.readVarInt();
            int max = buf.readVarInt();
            boolean canStart = buf.readBoolean();
            String map = buf.readUtf(64);
            int size = buf.readVarInt();
            List<Entry> players = new ArrayList<>(size);
            for (int i = 0; i < size; i++) players.add(new Entry(buf.readUtf(32), buf.readVarInt()));
            return new MatchState(state, seconds, max, canStart, map, players);
        }

        public static void handle(MatchState msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> com.warfare.client.ClientMatchState.accept(msg)));
            ctx.get().setPacketHandled(true);
        }
    }

    /** Bouton LANCER LA PARTIE : le serveur revalide la permission. */
    public record RequestStart() {
        public static void encode(RequestStart msg, FriendlyByteBuf buf) {}

        public static RequestStart decode(FriendlyByteBuf buf) {
            return new RequestStart();
        }

        public static void handle(RequestStart msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null) return;
                if (!player.hasPermissions(2)) {
                    player.sendSystemMessage(Txt.info(Txt.c("Reserve aux operateurs.", ChatFormatting.RED)));
                    return;
                }
                if (!com.warfare.game.GameManager.get().startCountdown(player.server)) {
                    player.sendSystemMessage(Txt.info(Txt.c(
                            "Impossible : partie en cours ou arene incomplete.", ChatFormatting.RED)));
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    // ------------------------------------------------------------ serveur -> client

    public record SyncClasses(PlayerClasses classes, List<String> allowedGuns, boolean menuAllowed) {
        public static void encode(SyncClasses msg, FriendlyByteBuf buf) {
            msg.classes.write(buf);
            buf.writeVarInt(msg.allowedGuns.size());
            for (String gun : msg.allowedGuns) buf.writeUtf(gun, 128);
            buf.writeBoolean(msg.menuAllowed);
        }

        public static SyncClasses decode(FriendlyByteBuf buf) {
            PlayerClasses classes = PlayerClasses.read(buf);
            int size = buf.readVarInt();
            List<String> guns = new ArrayList<>(size);
            for (int i = 0; i < size; i++) guns.add(buf.readUtf(128));
            return new SyncClasses(classes, guns, buf.readBoolean());
        }

        public static void handle(SyncClasses msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ClientClassData.accept(msg)));
            ctx.get().setPacketHandled(true);
        }
    }

    // ------------------------------------------------------------ client -> serveur

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
                ClassLoadout safe = ClassStore.sanitize(msg.loadout, msg.slot);
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

    private WarfareNetwork() {}
}
