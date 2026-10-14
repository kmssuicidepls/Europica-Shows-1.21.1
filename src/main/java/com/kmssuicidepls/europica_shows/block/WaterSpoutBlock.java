package com.kmssuicidepls.europica_shows.block;

import com.kmssuicidepls.europica_shows.block.entity.WaterSpoutBlockEntity;
import com.kmssuicidepls.europica_shows.menu.WaterSpoutMenu;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

public class WaterSpoutBlock extends BaseEntityBlock {
    public static final MapCodec<WaterSpoutBlock> CODEC = simpleCodec(WaterSpoutBlock::new);
    public static final IntegerProperty POWER = BlockStateProperties.POWER;

    public WaterSpoutBlock(BlockBehaviour.Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(POWER, 0));
    }

    // Pick up glass that is already touching the block when it is placed
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof WaterSpoutBlockEntity be) {
            be.updateGlassColor();
        }
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWER);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WaterSpoutBlockEntity(pos, state);
    }

    // BaseEntityBlock defaults to INVISIBLE, so this is required to show the block model
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(
                    new SimpleMenuProvider(
                            (id, inventory, p) -> new WaterSpoutMenu(id, inventory, pos),
                            getName()),
                    buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos,
                                   Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if (level.isClientSide) return;

        // Colored glass placed / removed next to the block changes the particle color
        if (level.getBlockEntity(pos) instanceof WaterSpoutBlockEntity be) {
            be.updateGlassColor();
        }

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

        // Blocks placed before this update have no block entity yet: fall back to water
        ParticleOptions options = ParticleTypes.FALLING_WATER;
        int count = 1;
        double spread = 0.1;
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 1.0;
        double z = pos.getZ() + 0.5;

        if (level.getBlockEntity(pos) instanceof WaterSpoutBlockEntity be) {
            options = be.getParticleOptions();
            count = be.getCount();
            spread = be.getSpread();
            x += be.getOffsetX();
            y += be.getStartHeight() + be.getOffsetY();
            z += be.getOffsetZ();
        }

        // One spawn point per half-block, from the start point up to `power` blocks high
        for (double h = 0; h < power; h += 0.5) {
            level.sendParticles(options, x, y + h, z, count, spread, 0.0, spread, 0.0);
        }

        level.scheduleTick(pos, this, 1);                // run again next tick
    }
}