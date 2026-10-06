package com.kmssuicidepls.europica_shows.block.entity;

import com.kmssuicidepls.europica_shows.EuropicaShows;
import com.kmssuicidepls.europica_shows.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, EuropicaShows.MOD_ID);

    public static final Supplier<BlockEntityType<BeamProjectorBlockEntity>> BEAM_PROJECTOR =
            BLOCK_ENTITIES.register("beam_projector", () ->
                    BlockEntityType.Builder.of(BeamProjectorBlockEntity::new,
                            ModBlocks.BEAM_PROJECTOR.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}