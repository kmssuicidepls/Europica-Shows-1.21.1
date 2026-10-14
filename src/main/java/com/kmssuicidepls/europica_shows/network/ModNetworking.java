package com.kmssuicidepls.europica_shows.network;

import com.kmssuicidepls.europica_shows.EuropicaShows;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = EuropicaShows.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class ModNetworking {

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(UpdateBeamPayload.TYPE, UpdateBeamPayload.STREAM_CODEC, UpdateBeamPayload::handle);
        registrar.playToServer(UpdateSpoutPayload.TYPE, UpdateSpoutPayload.STREAM_CODEC, UpdateSpoutPayload::handle);
        registrar.playToServer(UpdateStrobePayload.TYPE, UpdateStrobePayload.STREAM_CODEC, UpdateStrobePayload::handle);
    }
}