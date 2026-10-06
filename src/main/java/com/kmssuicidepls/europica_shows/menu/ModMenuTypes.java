package com.kmssuicidepls.europica_shows.menu;

import com.kmssuicidepls.europica_shows.EuropicaShows;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, EuropicaShows.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<BeamProjectorMenu>> BEAM_PROJECTOR_MENU =
            MENUS.register("beam_projector_menu",
                    () -> IMenuTypeExtension.create(BeamProjectorMenu::new));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}