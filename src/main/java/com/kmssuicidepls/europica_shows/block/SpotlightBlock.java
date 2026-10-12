package com.kmssuicidepls.europica_shows.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class SpotlightBlock extends BeamProjectorBlock {
    public static final MapCodec<SpotlightBlock> CODEC = simpleCodec(SpotlightBlock::new);

    public SpotlightBlock(BlockBehaviour.Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
}