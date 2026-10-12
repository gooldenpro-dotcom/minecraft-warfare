package com.warfare.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.warfare.Warfare;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * Touches du mod : menu d'accueil (M) et creation de classe (C), remappables
 * dans les options de controles.
 */
@Mod.EventBusSubscriber(modid = Warfare.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class WarfareClient {

    public static final KeyMapping OPEN_CLASSES = new KeyMapping(
            "key.warfare.open_classes",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            "key.categories.warfare");

    public static final KeyMapping OPEN_MENU = new KeyMapping(
            "key.warfare.open_menu",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            "key.categories.warfare");

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_CLASSES);
        event.register(OPEN_MENU);
        MinecraftForge.EVENT_BUS.register(Handlers.class);
    }

    /** Ecouteurs sur le bus Forge (en jeu). */
    public static final class Handlers {
        /** En quittant un serveur, on oublie son etat : le menu ne doit pas suivre ailleurs. */
        @SubscribeEvent
        public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
            ClientMatchState.reset();
            ScorePopups.clear();
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Minecraft minecraft = Minecraft.getInstance();
            ScorePopups.tick();

            while (OPEN_MENU.consumeClick()) {
                if (minecraft.player == null || !ClientMatchState.synced) continue;
                if (minecraft.screen == null) {
                    ClientMatchState.menuDismissed = false;
                    minecraft.setScreen(new LobbyScreen());
                }
            }

            while (OPEN_CLASSES.consumeClick()) {
                if (minecraft.player == null || minecraft.screen != null) continue;
                if (!ClientClassData.menuAllowed) {
                    minecraft.player.displayClientMessage(
                            Component.literal("§cMenu disponible au menu d'accueil ou entre deux vies."), true);
                    continue;
                }
                minecraft.setScreen(new ClassScreen());
            }

            // Hors partie, le menu d'accueil s'ouvre tout seul, sauf si le joueur l'a ferme.
            if (ClientMatchState.synced && ClientMatchState.inLobby() && !ClientMatchState.menuDismissed
                    && minecraft.player != null && minecraft.level != null
                    && minecraft.screen == null) {
                minecraft.setScreen(new LobbyScreen());
            }
        }

        private Handlers() {}
    }

    private WarfareClient() {}
}
