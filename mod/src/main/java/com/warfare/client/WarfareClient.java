package com.warfare.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.warfare.Warfare;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** Touche d'ouverture du menu "Creer une classe" (C par defaut, remappable dans les options). */
@Mod.EventBusSubscriber(modid = Warfare.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class WarfareClient {

    public static final KeyMapping OPEN_CLASSES = new KeyMapping(
            "key.warfare.open_menu",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            "key.categories.warfare");

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_CLASSES);
        MinecraftForge.EVENT_BUS.register(Handlers.class);
    }

    /** Ecouteurs sur le bus Forge (en jeu). */
    public static final class Handlers {
        /** En quittant un serveur, on oublie son etat : le menu ne doit pas suivre ailleurs. */
        @SubscribeEvent
        public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
            ClientMatchState.reset();
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Minecraft minecraft = Minecraft.getInstance();

            // Pas de lobby ou se promener : hors partie, le menu reste ouvert.
            if (ClientMatchState.synced && ClientMatchState.inLobby()
                    && minecraft.player != null && minecraft.level != null
                    && minecraft.screen == null) {
                minecraft.setScreen(new LobbyScreen());
                return;
            }

            while (OPEN_CLASSES.consumeClick()) {
                if (minecraft.player == null || minecraft.screen != null) continue;
                if (!ClientClassData.menuAllowed) {
                    minecraft.player.displayClientMessage(
                            Component.literal("§cMenu disponible au lobby ou entre deux vies."), true);
                    continue;
                }
                minecraft.setScreen(new ClassScreen());
            }
        }

        private Handlers() {}
    }

    private WarfareClient() {}
}
