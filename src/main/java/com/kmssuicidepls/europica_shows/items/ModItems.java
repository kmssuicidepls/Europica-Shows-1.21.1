package com.kmssuicidepls.europica_shows.items;

import com.kmssuicidepls.europica_shows.EuropicaShows;
import com.kmssuicidepls.europica_shows.block.ModBlocks;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(EuropicaShows.MOD_ID);

    //public static final DeferredItem<Item> EXAMPLE_ITEM = ITEMS.register("example_item",
    //        () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> RAT_SANDWICH = ITEMS.register("rat_sandwich",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<BlockItem> WATER_SPOUT =
            ITEMS.registerSimpleBlockItem("water_spout", ModBlocks.WATER_SPOUT);

    public static final DeferredItem<BlockItem> BEAM_PROJECTOR =
            ITEMS.registerSimpleBlockItem("beam_projector", ModBlocks.BEAM_PROJECTOR);

    public static final DeferredItem<BlockItem> SPOTLIGHT =
            ITEMS.registerSimpleBlockItem("spotlight", ModBlocks.SPOTLIGHT);

    public static final DeferredItem<BlockItem> STROBE_LIGHT =
            ITEMS.registerSimpleBlockItem("strobe_light", ModBlocks.STROBE_LIGHT);

    public static final DeferredItem<BlockItem> STONE_CONDUIT =
            ITEMS.registerSimpleBlockItem("stone_conduit", ModBlocks.STONE_CONDUIT);


    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}