package com.kmssuicidepls.europica_shows.registry;

import com.kmssuicidepls.europica_shows.EuropicaShows;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, EuropicaShows.MOD_ID);

    //public static final DeferredHolder<MenuType<?>, MenuType<ExampleMenu>> EXAMPLE_MENU = MENUS.register("example_menu",
    //        () -> IMenuTypeExtension.create((id, inv, buf) -> new ExampleMenu(id, inv, ContainerLevelAccess.NULL)));
}