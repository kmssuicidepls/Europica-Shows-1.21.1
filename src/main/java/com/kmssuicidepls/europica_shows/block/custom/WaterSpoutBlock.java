package com.kmssuicidepls.europica_shows.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class WaterSpoutBlock extends Block {
    public static final MapCodec<WaterSpoutBlock> CODEC = simpleCodec(WaterSpoutBlock::new);
    public static final IntegerProperty POWER = BlockStateProperties.POWER;

    public WaterSpoutBlock(BlockBehaviour.Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(POWER, 0));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWER);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos,
                                   Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if (level.isClientSide) return;

        int signal = level.getBestNeighborSignal(pos);   // 0-15
        int oldSignal = state.getValue(POWER);

        if (signal != oldSignal) {
            level.setBlock(pos, state.setValue(POWER, signal), Block.UPDATE_CLIENTS);
            if (oldSignal == 0) {
                level.scheduleTick(pos, this, 1);        // start the tick loop
            }
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int power = state.getValue(POWER);
        if (power == 0) return;                          // stop when unpowered

        // One particle per half-block, from the top of the block up to `power` blocks high
        for (double h = 0; h < power; h += 0.5) {
            level.sendParticles(
                    ParticleTypes.FALLING_WATER,
                    pos.getX() + 0.5, pos.getY() + 1.0 + h, pos.getZ() + 0.5,
                    1,
                    0.1, 0.0, 0.1,
                    0.0
            );
        }

        level.scheduleTick(pos, this, 1);                // run again next tick
    }
}