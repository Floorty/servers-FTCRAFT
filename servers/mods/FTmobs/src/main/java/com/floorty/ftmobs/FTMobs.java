package com.floorty.ftmobs;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Monster;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import org.slf4j.Logger;

@Mod(FTMobs.MOD_ID)
public final class FTMobs {
    public static final String MOD_ID = "ftmobs";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FTMobs(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.addListener(FTMobs::blockVanillaHostileMobs);
        LOGGER.info("FTmobs initialized: vanilla hostile mobs are disabled");
    }

    private static void blockVanillaHostileMobs(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Monster)) {
            return;
        }

        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType());
        if ("minecraft".equals(entityId.getNamespace())) {
            event.setCanceled(true);
        }
    }
}
