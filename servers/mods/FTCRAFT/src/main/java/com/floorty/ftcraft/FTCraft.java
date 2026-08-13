package com.floorty.ftcraft;

import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.slf4j.Logger;

@Mod(FTCraft.MOD_ID)
public final class FTCraft {
    public static final String MOD_ID = "ftcraft";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FTCraft(IEventBus modEventBus, ModContainer modContainer) {
        NeoForge.EVENT_BUS.addListener(FTCraft::blockPortalCreation);
        NeoForge.EVENT_BUS.addListener(FTCraft::blockDimensionTravel);
        LOGGER.info("FTCRAFT initialized");
    }

    private static void blockPortalCreation(BlockEvent.PortalSpawnEvent event) {
        event.setCanceled(true);
    }

    private static void blockDimensionTravel(EntityTravelToDimensionEvent event) {
        event.setCanceled(true);
        if (event.getEntity() instanceof ServerPlayer player) {
            player.displayClientMessage(
                    Component.literal("Порталы и переходы между измерениями запрещены на FTCRAFT."),
                    true
            );
        }
    }
}
