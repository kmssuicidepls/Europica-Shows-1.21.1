package com.kmssuicidepls.europica_shows.menu;

import com.kmssuicidepls.europica_shows.EuropicaShows;
import com.kmssuicidepls.europica_shows.menu.BeamProjectorMenu;
import com.kmssuicidepls.europica_shows.menu.StrobeLightMenu;
import com.kmssuicidepls.europica_shows.menu.WaterSpoutMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, EuropicaShows.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<BeamProjectorMenu>> BEAM_PROJECTOR_MENU =
            MENUS.register("beam_projector_menu",
                    () -> IMenuTypeExtension.create(BeamProjectorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<WaterSpoutMenu>> WATER_SPOUT_MENU =
            MENUS.register("water_spout_menu",
                    () -> IMenuTypeExtension.create(WaterSpoutMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<StrobeLightMenu>> STROBE_LIGHT_MENU =
            MENUS.register("strobe_light_menu",
                    () -> IMenuTypeExtension.create(StrobeLightMenu::new));

    //public static final DeferredHolder<MenuType<?>, MenuType<ExampleMenu>> EXAMPLE_MENU = MENUS.register("example_menu",
    //        () -> IMenuTypeExtension.create((id, inv, buf) -> new ExampleMenu(id, inv, ContainerLevelAccess.NULL)));
}