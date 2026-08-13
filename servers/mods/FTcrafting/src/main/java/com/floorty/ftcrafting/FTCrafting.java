package com.floorty.ftcrafting;

import com.mojang.logging.LogUtils;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

@Mod(FTCrafting.MOD_ID)
public final class FTCrafting {
    public static final String MOD_ID = "ftcrafting";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);

    public static final DeferredItem<Item> STICK_T1 = item("stick_t1");
    public static final DeferredItem<Item> SUGAR_CANE_T1 = item("sugar_cane_t1");
    public static final DeferredItem<Item> CLAY_T1 = item("clay_t1");
    public static final DeferredItem<Item> FEATHER_T1 = item("feather_t1");
    public static final DeferredItem<Item> LEATHER_T1 = item("leather_t1");
    public static final DeferredItem<Item> GUNPOWDER_T1 = item("gunpowder_t1");
    public static final DeferredItem<Item> SUGAR_T1 = item("sugar_t1");
    public static final DeferredItem<Item> STRING_T1 = item("string_t1");
    public static final DeferredItem<Item> STONE_PLATE_T1 = item("stone_plate_t1");
    public static final DeferredItem<Item> WOODEN_PLATE_T1 = item("wooden_plate_t1");
    public static final DeferredItem<Item> REFINED_SLIME_T1 = item("refined_slime_t1");

    private static DeferredItem<Item> item(String name) {
        return ITEMS.registerSimpleItem(name, new Item.Properties());
    }

    public FTCrafting(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        LOGGER.info("FTcrafting initialized");
    }
}
