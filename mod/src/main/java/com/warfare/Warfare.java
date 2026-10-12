package com.warfare;

import com.mojang.logging.LogUtils;
import com.warfare.command.WarfareCommand;
import com.warfare.event.WarfareEvents;
import com.warfare.net.WarfareNetwork;
import com.warfare.perk.PerkEffects;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;

/**
 * Minecraft Warfare : mode de jeu Free For All base sur les armes TaCZ,
 * avec creation de classes et atouts.
 */
@Mod(Warfare.MOD_ID)
public class Warfare {
    public static final String MOD_ID = "warfare";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Warfare() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, WarfareConfig.SPEC, "warfare-common.toml");
        WarfareNetwork.register();
        MinecraftForge.EVENT_BUS.register(WarfareEvents.class);
        MinecraftForge.EVENT_BUS.register(WarfareCommand.class);
        MinecraftForge.EVENT_BUS.register(WarfareConfig.class);
        MinecraftForge.EVENT_BUS.register(PerkEffects.class);
    }
}
