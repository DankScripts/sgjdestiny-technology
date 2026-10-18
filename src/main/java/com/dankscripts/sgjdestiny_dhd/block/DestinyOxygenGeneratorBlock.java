package com.dankscripts.sgjdestiny_dhd.block;

import com.dankscripts.sgjdestiny_dhd.block_entity.DestinyOxygenGeneratorBlockEntity;
import com.dankscripts.sgjdestiny_dhd.menu.DestinyAirScrubberMenu;
import com.dankscripts.sgjdestiny_dhd.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;

/** A one-wide, two-high wall recess containing one air-scrubbing tank. */
public final class DestinyOxygenGeneratorBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    // Keep the old six values loadable so existing three-high scrubbers convert safely.
    // New scrubbers use only parts 0 and 2 in the left-hand column.
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 5);
    private static final ThreadLocal<Boolean> REMOVING = ThreadLocal.withInitial(() -> false);

    public DestinyOxygenGeneratorBlock() {
        super(BlockBehaviour.Properties.of().strength(5.0F, 12.0F).sound(SoundType.METAL)
                .noOcclusion().lightLevel(state -> state.getValue(PART) == 0 ? 3 : 0));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, 0));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    private static BlockPos part(BlockPos root, Direction facing, int x, int y) {
        return root.relative(facing.getClockWise(), x).above(y);
    }

    public static BlockPos root(BlockPos pos, BlockState state) {
        int part = state.getValue(PART);
        return part(pos, state.getValue(FACING), -(part % 2), -(part / 2));
    }

    /** Free the extra column and top row of scrubbers from previous versions. */
    public static void removeLegacyParts(Level level, BlockPos root, BlockState state) {
        if (level.isClientSide() || state.getValue(PART) != 0 || REMOVING.get()) return;
        Direction facing = state.getValue(FACING);
        REMOVING.set(true);
        try {
            for (int y = 0; y < 3; y++) for (int x = 0; x < 2; x++) {
                if (x == 0 && y < 2) continue;
                BlockPos target = part(root, facing, x, y);
                if (!level.hasChunkAt(target)) continue;
                BlockState oldPart = level.getBlockState(target);
                if (oldPart.is(state.getBlock()) && oldPart.getValue(FACING) == facing
                        && oldPart.getValue(PART) == y * 2 + x)
                    level.removeBlock(target, false);
            }
        } finally { REMOVING.set(false); }
    }

    @Nullable @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos root = context.getClickedPos();
        for (int y = 0; y < 2; y++) {
            BlockPos target = part(root, facing, 0, y);
            if (!context.getLevel().getWorldBorder().isWithinBounds(target)
                    || !context.getLevel().getBlockState(target).canBeReplaced(context)) return null;
        }
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                                      LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide()) return;
        Direction facing = state.getValue(FACING);
        for (int y = 0; y < 2; y++) {
            int index = y * 2;
            if (index != 0) level.setBlock(part(pos, facing, 0, y),
                    defaultBlockState().setValue(FACING, facing).setValue(PART, index), Block.UPDATE_ALL);
        }
    }

    @Override public void onRemove(BlockState state, Level level, BlockPos pos,
                                   BlockState replacement, boolean moving) {
        if (state.getValue(PART) == 0 && !state.is(replacement.getBlock())
                && level.getBlockEntity(pos) instanceof DestinyOxygenGeneratorBlockEntity bay)
            bay.dropInventory();
        if (!state.is(replacement.getBlock()) && !REMOVING.get()) {
            REMOVING.set(true);
            try {
                BlockPos root = root(pos, state);
                Direction facing = state.getValue(FACING);
                for (int y = 0; y < 3; y++) for (int x = 0; x < 2; x++) {
                    BlockPos target = part(root, facing, x, y);
                    BlockState targetState = level.getBlockState(target);
                    if (!target.equals(pos) && targetState.is(this)
                            && targetState.getValue(FACING) == facing
                            && targetState.getValue(PART) == y * 2 + x)
                        level.removeBlock(target, false);
                }
                if (!root.equals(pos) && level.getBlockState(root).is(this)) level.removeBlock(root, false);
            } finally { REMOVING.set(false); }
        }
        super.onRemove(state, level, pos, replacement, moving);
    }

    @Override public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && state.getValue(PART) != 0) {
            BlockPos root = root(pos, state);
            if (level.getBlockState(root).is(this)) level.destroyBlock(root, !player.isCreative());
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == 0 ? new DestinyOxygenGeneratorBlockEntity(pos, state) : null;
    }

    @Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() || state.getValue(PART) != 0 ? null
                : createTickerHelper(type, ModBlockEntities.DESTINY_OXYGEN_GENERATOR.get(),
                        DestinyOxygenGeneratorBlockEntity::tick);
    }

    @Override public RenderShape getRenderShape(BlockState state) {
        return state.getValue(PART) == 0 ? RenderShape.MODEL : RenderShape.INVISIBLE;
    }

    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                           InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        BlockPos root = root(pos, state);
        if (!(level.getBlockEntity(root) instanceof DestinyOxygenGeneratorBlockEntity bay))
            return InteractionResult.FAIL;
        if (player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, new MenuProvider() {
                @Override public Component getDisplayName() { return Component.literal("Air Scrubber"); }
                @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player menuPlayer) {
                    return new DestinyAirScrubberMenu(id, inventory, root, bay, bay.menuData());
                }
            }, root);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.FAIL;
    }
}
