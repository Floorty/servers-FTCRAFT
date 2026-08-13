package com.floorty.ftcraft;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(FTCraft.MOD_ID)
public final class FTCraft {
    public static final String MOD_ID = "ftcraft";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FTCraft(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("FTCRAFT initialized");
    }
}
