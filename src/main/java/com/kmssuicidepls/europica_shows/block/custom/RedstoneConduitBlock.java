package com.kmssuicidepls.europica_shows.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * A full block that carries redstone like dust: signal strength 0-15 that drops by 1 for each
 * conduit it travels through, in all six directions, placed like any normal block.
 */
public class RedstoneConduitBlock extends Block {
    public static final MapCodec<RedstoneConduitBlock> CODEC = simpleCodec(RedstoneConduitBlock::new);
    public static final IntegerProperty POWER = BlockStateProperties.POWER;

    public RedstoneConduitBlock(BlockBehaviour.Properties props) {
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

    // ---- Output: tell neighbors our power ----

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(POWER);
    }

    // ---- Input: work out our power from the neighbors ----

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide && !oldState.is(this)) {
            level.scheduleTick(pos, this, 1);   // settle our power right after placement
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                   BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide) {
            updatePower(state, level, pos);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        updatePower(state, level, pos);
    }

    private void updatePower(BlockState state, Level level, BlockPos pos) {
        int target = 0;
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            BlockState neighbor = level.getBlockState(neighborPos);
            if (neighbor.getBlock() instanceof RedstoneConduitBlock) {
                // Another conduit: take its power minus 1, which gives the decay with distance
                target = Math.max(target, neighbor.getValue(POWER) - 1);
            } else {
                // Levers, repeaters, torches, observers, redstone blocks, etc.
                target = Math.max(target, level.getSignal(neighborPos, dir));
            }
        }
        if (target != state.getValue(POWER)) {
            level.setBlock(pos, state.setValue(POWER, target), Block.UPDATE_ALL);
        }
    }
}