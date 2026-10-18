package com.dankscripts.sgjdestiny_dhd.block;

import com.dankscripts.sgjdestiny_dhd.block_entity.DestinyWaterHoldingTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraft.world.level.material.Fluids;

import javax.annotation.Nullable;

/** One placeable 3x3x3 tank, anchored at the center of its bottom layer. */
public final class DestinyWaterHoldingTankBlock extends BaseEntityBlock {
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 26);
    private static final int ROOT = 4;
    private static final ThreadLocal<Boolean> REMOVING = ThreadLocal.withInitial(() -> false);

    public DestinyWaterHoldingTankBlock() {
        super(BlockBehaviour.Properties.of().strength(5.0F, 12.0F).sound(SoundType.METAL));
        registerDefaultState(stateDefinition.any().setValue(PART, ROOT));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART);
    }

    private static int part(int x, int y, int z) { return y * 9 + (z + 1) * 3 + x + 1; }
    private static BlockPos offset(BlockPos root, int x, int y, int z) { return root.offset(x, y, z); }
    private static BlockPos root(BlockPos pos, BlockState state) {
        int part = state.getValue(PART);
        return pos.offset(-(part % 3 - 1), -(part / 9), -((part % 9) / 3 - 1));
    }

    @Nullable @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos root = context.getClickedPos();
        for (int y = 0; y < 3; y++) for (int z = -1; z <= 1; z++) for (int x = -1; x <= 1; x++) {
            BlockPos target = offset(root, x, y, z);
            if (!context.getLevel().getWorldBorder().isWithinBounds(target)
                    || !context.getLevel().getBlockState(target).canBeReplaced(context)) {
                if (!context.getLevel().isClientSide() && context.getPlayer() != null)
                    context.getPlayer().displayClientMessage(Component.literal(
                            "Destiny water tank needs a clear 3×3×3 space."), true);
                return null;
            }
        }
        return defaultBlockState();
    }

    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                                      LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide()) return;
        for (int y = 0; y < 3; y++) for (int z = -1; z <= 1; z++) for (int x = -1; x <= 1; x++) {
            int part = part(x, y, z);
            if (part != ROOT) level.setBlock(offset(pos, x, y, z),
                    defaultBlockState().setValue(PART, part), Block.UPDATE_ALL);
        }
    }

    @Override public void onRemove(BlockState state, Level level, BlockPos pos,
                                   BlockState replacement, boolean moving) {
        if (!state.is(replacement.getBlock()) && !REMOVING.get()) {
            REMOVING.set(true);
            try {
                BlockPos root = root(pos, state);
                if (!root.equals(pos) && level.getBlockState(root).is(this)
                        && level.getBlockState(root).getValue(PART) == ROOT)
                    level.destroyBlock(root, true);
                for (int y = 0; y < 3; y++) for (int z = -1; z <= 1; z++) for (int x = -1; x <= 1; x++) {
                    BlockPos target = offset(root, x, y, z);
                    BlockState found = level.getBlockState(target);
                    if (!target.equals(pos) && found.is(this) && found.getValue(PART) == part(x, y, z))
                        level.removeBlock(target, false);
                }
            } finally { REMOVING.set(false); }
        }
        super.onRemove(state, level, pos, replacement, moving);
    }

    @Override public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && state.getValue(PART) != ROOT) {
            BlockPos root = root(pos, state);
            BlockState anchor = level.getBlockState(root);
            if (anchor.is(this) && anchor.getValue(PART) == ROOT)
                level.destroyBlock(root, !player.isCreative());
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == ROOT ? new DestinyWaterHoldingTankBlockEntity(pos, state) : null;
    }

    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                           InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(root(pos, state)) instanceof DestinyWaterHoldingTankBlockEntity tank))
            return InteractionResult.FAIL;
        ItemStack held = player.getItemInHand(hand);
        boolean changed = false;
        if (held.is(Items.WATER_BUCKET)) {
            int accepted = tank.fluidHandler().fill(new FluidStack(Fluids.WATER, 1000),
                    IFluidHandler.FluidAction.SIMULATE);
            if (accepted == 1000) {
                tank.fluidHandler().fill(new FluidStack(Fluids.WATER, 1000),
                        IFluidHandler.FluidAction.EXECUTE);
                if (!player.getAbilities().instabuild) player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                changed = true;
            }
        } else if (held.is(Items.BUCKET)) {
            FluidStack drained = tank.fluidHandler().drain(1000, IFluidHandler.FluidAction.SIMULATE);
            if (drained.getAmount() == 1000 && drained.getFluid() == Fluids.WATER) {
                tank.fluidHandler().drain(1000, IFluidHandler.FluidAction.EXECUTE);
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                    ItemStack filled = new ItemStack(Items.WATER_BUCKET);
                    if (held.isEmpty()) player.setItemInHand(hand, filled);
                    else if (!player.getInventory().add(filled)) player.drop(filled, false);
                }
                changed = true;
            }
        }
        if (changed) level.updateNeighbourForOutputSignal(root(pos, state), this);
        player.displayClientMessage(Component.literal("Destiny water tank: "
                + tank.bankWaterAmount() / 1000 + " / " + tank.bankCapacity() / 1000 + " buckets"), true);
        return InteractionResult.CONSUME;
    }

    @Override public boolean hasAnalogOutputSignal(BlockState state) { return true; }

    @Override public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(root(pos, state)) instanceof DestinyWaterHoldingTankBlockEntity tank)
            return tank.bankWaterAmount() == 0 ? 0
                    : 1 + (int) (14L * tank.bankWaterAmount() / tank.bankCapacity());
        return 0;
    }
}
