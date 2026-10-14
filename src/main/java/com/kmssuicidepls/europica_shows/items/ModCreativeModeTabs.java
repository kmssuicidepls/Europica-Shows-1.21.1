package com.kmssuicidepls.europica_shows.items;

import com.kmssuicidepls.europica_shows.EuropicaShows;
import com.kmssuicidepls.europica_shows.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, EuropicaShows.MOD_ID);

    public static final Supplier<CreativeModeTab> EUROPICA_SHOWS = CREATIVE_MODE_TAB.register("europica_shows_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(Items.BEACON))
                    .title(Component.translatable("creativetab.europicashows.europica_europica_shows"))
                    .displayItems((itemDisplayParameters, output) -> {

                    output.accept(ModBlocks.WATER_SPOUT);

                    output.accept(ModItems.BEAM_PROJECTOR);
                    output.accept(ModItems.SPOTLIGHT);
                    output.accept(ModItems.STROBE_LIGHT);
                    output.accept(ModItems.STONE_CONDUIT);
                    output.accept(ModItems.RAT_SANDWICH);

                    }).build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TAB.register(eventBus);
    }
}