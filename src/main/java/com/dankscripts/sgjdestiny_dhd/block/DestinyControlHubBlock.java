package com.dankscripts.sgjdestiny_dhd.block;

import com.mojang.serialization.MapCodec;

import com.dankscripts.sgjdestiny_dhd.block_entity.DestinyControlHubBlockEntity;
import com.dankscripts.sgjdestiny_dhd.menu.DestinyControlHubMenu;
import com.dankscripts.sgjdestiny_dhd.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.dankscripts.sgjdestiny_dhd.network.MenuNetworkHooks;

/** Five-block control-room apple core and its attached, working Destiny console. */
public final class DestinyControlHubBlock extends BaseEntityBlock {
    private static final MapCodec<DestinyControlHubBlock> CODEC = simpleCodec(ignored -> new DestinyControlHubBlock());
    @Override protected MapCodec<DestinyControlHubBlock> codec() { return CODEC; }
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private static final VoxelShape COLUMN = Block.box(0, 0, 0, 16, 80, 16);

    public DestinyControlHubBlock() {
        super(BlockBehaviour.Properties.of().strength(6.0F, 12.0F)
                .sound(SoundType.METAL).lightLevel(s -> 8).noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    public static BlockPos at(BlockPos anchor, Direction forward, int x, int y, int z) {
        return anchor.relative(forward.getCounterClockWise(), x).relative(forward, z).above(y);
    }

    public static boolean occupied(int x, int y, int z) {
        if (x == 0 && y == 0 && z == 0) return false;
        if (y == 0 || y == 4) return x >= -1 && x <= 2 && z >= -1 && z <= 2;
        if (y == 1) return (x == 0 && z == 0) || (z >= 1 && z <= 2 && x >= -1 && x <= 1);
        return x == 0 && z == 0 && y <= 3;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction direction = context.getHorizontalDirection();
        BlockPos origin = context.getClickedPos();
        for (int y = 0; y < 5; y++)
            for (int z = -1; z <= 2; z++)
                for (int x = -1; x <= 2; x++) {
                    if (!occupied(x, y, z)) continue;
                    BlockPos target = at(origin, direction, x, y, z);
                    if (!context.getLevel().getWorldBorder().isWithinBounds(target)
                            || !context.getLevel().getBlockState(target).canBeReplaced(context)) {
                        if (!context.getLevel().isClientSide() && context.getPlayer() != null)
                            context.getPlayer().displayClientMessage(Component.literal(
                                    "Control hub needs clear floor, conduit, and ceiling space at "
                                            + target.toShortString()), true);
                        return null;
                    }
                }
        return defaultBlockState().setValue(FACING, direction);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DestinyControlHubBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide()) return;
        Direction direction = state.getValue(FACING);
        for (int y = 0; y < 5; y++)
            for (int z = -1; z <= 2; z++)
                for (int x = -1; x <= 2; x++)
                    if (occupied(x, y, z))
                        level.setBlock(at(pos, direction, x, y, z),
                                ModBlocks.DESTINY_CONTROL_HUB_PART.get().defaultBlockState()
                                        .setValue(DestinyControlHubPartBlock.FACING, direction)
                                        .setValue(DestinyControlHubPartBlock.PART,
                                                DestinyControlHubPartBlock.encode(x, y, z)), Block.UPDATE_ALL);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!state.is(replacement.getBlock())) {
            Direction direction = state.getValue(FACING);
            for (int y = 0; y < 5; y++)
                for (int z = -1; z <= 2; z++)
                    for (int x = -1; x <= 2; x++) {
                        BlockPos target = at(pos, direction, x, y, z);
                        if (occupied(x, y, z) && level.getBlockState(target)
                                .is(ModBlocks.DESTINY_CONTROL_HUB_PART.get()))
                            level.removeBlock(target, false);
                    }
        }
        super.onRemove(state, level, pos, replacement, moving);
    }

    public static InteractionResult activate(Level level, BlockPos pos, Player player) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.CONSUME;
        BlockEntity existing = level.getBlockEntity(pos);
        if (!(existing instanceof DestinyControlHubBlockEntity)) {
            // Hubs placed before the ship-status conversion may still carry SGJourney's DHD entity.
            CompoundTag oldReadings = existing == null ? null : existing.saveWithFullMetadata(level.registryAccess());
            level.removeBlockEntity(pos);
            DestinyControlHubBlockEntity replacement = new DestinyControlHubBlockEntity(pos, level.getBlockState(pos));
            if (oldReadings != null) replacement.loadWithComponents(oldReadings, level.registryAccess());
            level.setBlockEntity(replacement);
            replacement.setChanged();
        }
        if (!(level.getBlockEntity(pos) instanceof DestinyControlHubBlockEntity hub))
            return InteractionResult.CONSUME;
        MenuNetworkHooks.openScreen(serverPlayer, new net.minecraft.world.MenuProvider() {
            @Override public Component getDisplayName() { return Component.literal("Destiny Control Interface"); }
            @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player menuPlayer) {
                return new DestinyControlHubMenu(id, inventory, pos, hub, hub.statusData());
            }
        }, pos);
        return InteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        InteractionHand hand = InteractionHand.MAIN_HAND;
        return activate(level, pos, player);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return COLUMN;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return COLUMN;
    }
}
