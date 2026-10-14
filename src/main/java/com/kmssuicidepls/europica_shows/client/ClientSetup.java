package com.kmssuicidepls.europica_shows.client;

import com.kmssuicidepls.europica_shows.EuropicaShows;
import com.kmssuicidepls.europica_shows.block.entity.ModBlockEntities;
import com.kmssuicidepls.europica_shows.registry.ModMenuTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = EuropicaShows.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.BEAM_PROJECTOR_MENU.get(), BeamProjectorScreen::new);
        event.register(ModMenuTypes.WATER_SPOUT_MENU.get(), WaterSpoutScreen::new);
        event.register(ModMenuTypes.STROBE_LIGHT_MENU.get(), StrobeLightScreen::new);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.BEAM_PROJECTOR.get(), BeamProjectorRenderer::new);
    }
}