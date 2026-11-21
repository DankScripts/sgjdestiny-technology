package com.dankscripts.sgjdestiny_dhd.block;

import com.dankscripts.sgjdestiny_dhd.entity.DestinyPilotSeatEntity;
import com.dankscripts.sgjdestiny_dhd.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Invisible interaction and collision aligned with the pilot chair in the OBJ. */
public final class DestinyPilotSeatBlock extends Block {
    public static final DirectionProperty FACING = net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty LAYER = IntegerProperty.create("layer", 0, 1);
    public static final int FORWARD = 20;
    // The modeled cushion runs from z=-20.62 to -19.65 relative to the hatch.
    // Its forward edge is outside the seat block at z=-20, so the click target
    // must project toward the nose rather than stopping at the block boundary.
    private static final VoxelShape NORTH_LOWER_TARGET = Block.box(0.04 * 16, 0.36 * 16, -0.66 * 16,
            0.96 * 16, 0.84 * 16, 0.56 * 16);
    private static final VoxelShape SOUTH_LOWER_TARGET = Block.box(0.04 * 16, 0.36 * 16, 0.44 * 16,
            0.96 * 16, 0.84 * 16, 1.66 * 16);
    private static final VoxelShape EAST_LOWER_TARGET = Block.box(0.44 * 16, 0.36 * 16, 0.04 * 16,
            1.66 * 16, 0.84 * 16, 0.96 * 16);
    private static final VoxelShape WEST_LOWER_TARGET = Block.box(-0.66 * 16, 0.36 * 16, 0.04 * 16,
            0.56 * 16, 0.84 * 16, 0.96 * 16);
    private static final VoxelShape UPPER_TARGET = Block.box(2, 0, 2, 14, 12, 14);
    private static final VoxelShape CUSHION = Block.box(2.5, 5.5, 3.5, 13.5, 13, 12.5);
    private static final VoxelShape BACK = Block.box(2.5, 0, 7, 13.5, 10.5, 12.5);

    public DestinyPilotSeatBlock() {
        super(BlockBehaviour.Properties.of().strength(-1.0F, 3_600_000.0F)
                .sound(SoundType.METAL).noOcclusion().noLootTable());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LAYER, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LAYER);
    }

    public static BlockPos lowerPos(BlockPos anchor, Direction facing) {
        return anchor.relative(facing, FORWARD).above();
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(LAYER) != 0) return UPPER_TARGET;
        return switch (state.getValue(FACING)) {
            case SOUTH -> SOUTH_LOWER_TARGET;
            case EAST -> EAST_LOWER_TARGET;
            case WEST -> WEST_LOWER_TARGET;
            default -> NORTH_LOWER_TARGET;
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(LAYER) == 0 ? CUSHION : BACK;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        InteractionHand hand = InteractionHand.MAIN_HAND;
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        Direction facing = state.getValue(FACING);
        BlockPos lower = pos.below(state.getValue(LAYER));
        BlockPos anchor = lower.below().relative(facing.getOpposite(), FORWARD);
        BlockState airlock = level.getBlockState(anchor);
        if (!(airlock.getBlock() instanceof DestinyShuttleAirlockBlock)
                || airlock.getValue(DestinyShuttleAirlockBlock.FACING) != facing
                || airlock.getValue(DestinyShuttleAirlockBlock.COLUMN) != 0
                || airlock.getValue(DestinyShuttleAirlockBlock.ROW) != 0) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        AABB nearby = new AABB(lower).inflate(0.5, 1.5, 0.5);
        if (!level.getEntitiesOfClass(DestinyPilotSeatEntity.class, nearby).isEmpty()) return InteractionResult.CONSUME;
        DestinyPilotSeatEntity seat = ModEntities.PILOT_SEAT.get().create(level);
        if (seat == null) return InteractionResult.FAIL;
        seat.setAnchor(anchor, facing);
        seat.setPos(lower.getX() + 0.5 + facing.getStepX() * 0.2,
                lower.getY() + 0.84, lower.getZ() + 0.5 + facing.getStepZ() * 0.2);
        seat.setYRot(facing.toYRot());
        if (!level.addFreshEntity(seat) || !player.startRiding(seat, true)) {
            seat.discard();
            return InteractionResult.FAIL;
        }
        player.setYRot(facing.toYRot());
        player.setYHeadRot(facing.toYRot());
        return InteractionResult.CONSUME;
    }
}
