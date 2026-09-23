package com.kmssuicidepls.europica_shows.items;

import com.kmssuicidepls.europica_shows.EuropicaShows;
import net.minecraft.world.food.FoodProperties;
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

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
