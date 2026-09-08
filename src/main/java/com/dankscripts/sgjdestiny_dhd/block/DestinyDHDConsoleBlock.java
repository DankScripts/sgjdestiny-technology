package com.dankscripts.sgjdestiny_dhd.block;

import com.dankscripts.sgjdestiny_dhd.block_entity.DestinyUniverseDHDEntity;
import com.dankscripts.sgjdestiny_dhd.compat.DestinyBearingCompat;
import com.dankscripts.sgjdestiny_dhd.registry.ModBlocks;
import com.dankscripts.sgjdestiny_dhd.dialer.SeedShipAddressDatabase;
import com.dankscripts.sgjdestiny_dhd.network.DestinyDialerNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.povstalec.sgjourney.common.blocks.dhd.UniverseDHDBlock;
import net.povstalec.sgjourney.common.block_entities.dhd.UniverseDHDEntity;
import net.povstalec.sgjourney.common.menu.DHDCrystalMenu;
import net.povstalec.sgjourney.common.menu.UniverseDHDMenu;
import net.minecraftforge.network.NetworkHooks;

/**
 * Destiny's physical console backed by SGJourney's complete Universe DHD logic.
 *
 * Normal right-click opens SGJourney's Universe dialer from every console face.
 * Sneak-right-click opens its crystal interface for installing dialing hardware.
 */
public final class DestinyDHDConsoleBlock extends UniverseDHDBlock {
    static {
        DestinyDialerNetwork.register();
    }
    private static final VoxelShape NORTH_SHAPE = Shapes.or(
            box(-5.0, 0.0, 0.0, 21.0, 2.0, 20.0),
            box(-3.0, 2.0, 6.0, 2.0, 18.5, 10.0),
            box(14.0, 2.0, 6.0, 19.0, 18.5, 10.0),
            box(-12.0, 17.5, -2.0, 28.0, 20.5, 20.0),
            // Full monitor interaction area, including the upper screen housing.
            box(-3.0, 19.0, 1.0, 19.0, 35.0, 16.0));
    private static final VoxelShape EAST_SHAPE = rotateClockwise(NORTH_SHAPE);
    private static final VoxelShape SOUTH_SHAPE = rotateClockwise(EAST_SHAPE);
    private static final VoxelShape WEST_SHAPE = rotateClockwise(SOUTH_SHAPE);

    public DestinyDHDConsoleBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_GREEN)
                .strength(5.0F, 9.0F)
                .sound(SoundType.METAL)
                .noOcclusion());
        // UniverseDHDBlock already establishes FACING and WATERLOGGED=false.
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DestinyUniverseDHDEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        if (level.getBlockEntity(pos) instanceof UniverseDHDEntity dhd
                && player instanceof ServerPlayer serverPlayer) {
            if (player.isShiftKeyDown()) {
                if (dhd.hasPermissions(player, true)) {
                    NetworkHooks.openScreen(serverPlayer, new DestinyCrystalProvider(dhd), pos);
                }
                return InteractionResult.CONSUME;
            }
            dhd.generate();
            DestinyDialerNetwork.openSeedShipDatabase(serverPlayer, dhd,
                    SeedShipAddressDatabase.entriesFor(serverPlayer.server, dhd));
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide()) {
            placeInteractionTarget(level, pos.above());
            placeInteractionTarget(level, pos.above(2));
        }
    }

    private static void placeInteractionTarget(Level level, BlockPos target) {
        if (level.getBlockState(target).canBeReplaced()) {
            level.setBlock(target, ModBlocks.DESTINY_DHD_INTERACTION.get().defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!state.is(replacement.getBlock())) {
            for (int up = 1; up <= 2; up++) {
                BlockPos target = pos.above(up);
                if (level.getBlockState(target).is(ModBlocks.DESTINY_DHD_INTERACTION.get())) {
                    level.removeBlock(target, false);
                }
            }
        }
        super.onRemove(state, level, pos, replacement, moving);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        BlockEntityTicker<T> parentTicker = super.getTicker(level, state, type);
        if (parentTicker == null) {
            return null;
        }

        return (tickerLevel, tickerPos, tickerState, blockEntity) -> {
            parentTicker.tick(tickerLevel, tickerPos, tickerState, blockEntity);
            if (!tickerLevel.isClientSide()
                    && tickerLevel instanceof net.minecraft.server.level.ServerLevel serverLevel
                    && blockEntity instanceof UniverseDHDEntity dhd) {
                // Backfill interaction targets for existing worlds without checking every tick.
                if (serverLevel.getGameTime() % 20L == 0L) {
                    placeInteractionTarget(serverLevel, tickerPos.above());
                    placeInteractionTarget(serverLevel, tickerPos.above(2));
                }
                DestinyBearingCompat.tickFromDHD(serverLevel, dhd);
            }
        };
    }

    public static final class DestinyCrystalProvider implements MenuProvider {
        private final UniverseDHDEntity dhd;

        public DestinyCrystalProvider(UniverseDHDEntity dhd) {
            this.dhd = dhd;
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable("screen.sgjourney.dhd");
        }

        @Override
        public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
            return new DestinyUniverseDHDCrystalMenu(containerId, inventory, dhd);
        }
    }

    public static final class DestinyUniverseDHDCrystalMenu extends DHDCrystalMenu.Universe {
        public DestinyUniverseDHDCrystalMenu(int containerId, Inventory inventory, UniverseDHDEntity dhd) {
            super(containerId, inventory, dhd);
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    }

    public static final class DestinyMenuProvider implements MenuProvider {
        private final UniverseDHDEntity dhd;

        public DestinyMenuProvider(UniverseDHDEntity dhd) {
            this.dhd = dhd;
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable("screen.sgjourney.dhd");
        }

        @Override
        public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
            return new DestinyUniverseDHDMenu(containerId, inventory, dhd);
        }
    }

    /**
     * SGJourney's stock Universe menu only remains valid while its block entity
     * occupies the stock Universe DHD block. The Destiny console intentionally
     * reuses that block entity behind a different registered block, so it needs
     * an equivalent server menu without the stock-block identity restriction.
     */
    public static final class DestinyUniverseDHDMenu extends UniverseDHDMenu {
        public DestinyUniverseDHDMenu(int containerId, Inventory inventory, UniverseDHDEntity dhd) {
            super(containerId, inventory, dhd);
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos position, CollisionContext context) {
        return shapeFor(state.getValue(FACING));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos position,
                                        CollisionContext context) {
        return shapeFor(state.getValue(FACING));
    }

    private static VoxelShape shapeFor(Direction direction) {
        return switch (direction) {
            case EAST -> EAST_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            default -> NORTH_SHAPE;
        };
    }

    private static VoxelShape rotateClockwise(VoxelShape source) {
        VoxelShape rotated = Shapes.empty();
        for (AABB box : source.toAabbs()) {
            rotated = Shapes.or(rotated, Block.box(
                    (1.0 - box.maxZ) * 16.0,
                    box.minY * 16.0,
                    box.minX * 16.0,
                    (1.0 - box.minZ) * 16.0,
                    box.maxY * 16.0,
                    box.maxX * 16.0));
        }
        return rotated;
    }
}
