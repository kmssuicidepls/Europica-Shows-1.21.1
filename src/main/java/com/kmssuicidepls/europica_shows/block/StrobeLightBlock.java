package com.kmssuicidepls.europica_shows.block;

import com.kmssuicidepls.europica_shows.block.entity.ModBlockEntities;
import com.kmssuicidepls.europica_shows.block.entity.StrobeLightBlockEntity;
import com.kmssuicidepls.europica_shows.menu.StrobeLightMenu;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

public class StrobeLightBlock extends BaseEntityBlock {
    public static final MapCodec<StrobeLightBlock> CODEC = simpleCodec(StrobeLightBlock::new);

    /** The way the light points, away from the block it is stuck to (UP = sitting on a floor). */
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    /** true during a flash: swaps the model to strobe_light_on and makes the block emit light */
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    /** true while a redstone signal is reaching the block (used to detect a new pulse) */
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    // What you point at / click / break: a little larger than the model so it is easy to hit
    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);
    // What you collide with: matches the model itself
    private static final Map<Direction, VoxelShape> COLLISION_SHAPES = new EnumMap<>(Direction.class);

    static {
        for (Direction d : Direction.values()) {
            SHAPES.put(d, shapeFor(d, 4, 12, 4));
            COLLISION_SHAPES.put(d, shapeFor(d, 5, 11, 2));
        }
    }

    /** A flat box sitting against the supporting face (opposite of `facing`), `thick` pixels deep. */
    private static VoxelShape shapeFor(Direction facing, double lo, double hi, double thick) {
        return switch (facing) {
            case UP -> Block.box(lo, 0, lo, hi, thick, hi);
            case DOWN -> Block.box(lo, 16 - thick, lo, hi, 16, hi);
            case NORTH -> Block.box(lo, lo, 16 - thick, hi, hi, 16);
            case SOUTH -> Block.box(lo, lo, 0, hi, hi, thick);
            case EAST -> Block.box(0, lo, lo, thick, hi, hi);
            case WEST -> Block.box(16 - thick, lo, lo, 16, hi, hi);
        };
    }

    public StrobeLightBlock(BlockBehaviour.Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.UP)
                .setValue(LIT, false)
                .setValue(POWERED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT, POWERED);
    }

    // Stick to the face that was clicked; if that can't hold it, try the other faces around the position
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        boolean powered = level.hasNeighborSignal(pos);

        BlockState state = defaultBlockState()
                .setValue(FACING, context.getClickedFace())
                .setValue(POWERED, powered);
        if (state.canSurvive(level, pos)) return state;

        for (Direction look : context.getNearestLookingDirections()) {
            state = defaultBlockState()
                    .setValue(FACING, look.getOpposite())
                    .setValue(POWERED, powered);
            if (state.canSurvive(level, pos)) return state;
        }
        return null;   // nothing to attach to
    }

    // Needs a solid face to be stuck to, on the side opposite to where it points
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos supportPos = pos.relative(facing.getOpposite());
        return level.getBlockState(supportPos).isFaceSturdy(level, supportPos, facing);
    }

    // Pops off (drops as an item) when the block it is stuck to is removed
    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StrobeLightBlockEntity(pos, state);
    }

    // BaseEntityBlock defaults to INVISIBLE, so this is required to show the block model
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return COLLISION_SHAPES.get(state.getValue(FACING));
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        // The strobe logic only runs on the server; clients just see the LIT block state change
        return level.isClientSide ? null
                : createTickerHelper(type, ModBlockEntities.STROBE_LIGHT.get(), StrobeLightBlockEntity::serverTick);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos,
                                   Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if (level.isClientSide) return;

        boolean powered = level.hasNeighborSignal(pos);
        if (powered != state.getValue(POWERED)) {
            level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
            // A rising edge is a "pulse"
            if (powered && level.getBlockEntity(pos) instanceof StrobeLightBlockEntity be) {
                be.onPulse();
            }
        }
    }

    // Sneak + right click (empty hands) opens the settings menu. A plain right click does nothing.
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!player.isShiftKeyDown()) return InteractionResult.PASS;

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(
                    new SimpleMenuProvider(
                            (id, inventory, p) -> new StrobeLightMenu(id, inventory, pos),
                            getName()),
                    buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}