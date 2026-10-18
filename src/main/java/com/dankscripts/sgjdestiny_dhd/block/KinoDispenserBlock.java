package com.dankscripts.sgjdestiny_dhd.block;

import com.dankscripts.sgjdestiny_dhd.block_entity.KinoDispenserBlockEntity;
import com.dankscripts.sgjdestiny_dhd.menu.KinoDispenserMenu;
import com.dankscripts.sgjdestiny_dhd.registry.ModBlocks;
import com.dankscripts.sgjdestiny_dhd.registry.ModItems;
import com.dankscripts.sgjdestiny_dhd.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;

/** A tall, floor-mounted Ancient Kino storage and loading fixture. */
public final class KinoDispenserBlock extends BaseEntityBlock {
    public static final net.minecraft.world.level.block.state.properties.DirectionProperty FACING =
            net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING;
    private static final VoxelShape ROUND_BASE = Shapes.or(
            box(4, 0, 0, 12, 2.4, 16),
            box(2, 0, 1, 14, 2.4, 15),
            box(1, 0, 2, 15, 2.4, 14),
            box(0, 0, 4, 16, 2.4, 12)).optimize();

    private static final VoxelShape SHAPE = Shapes.or(
            ROUND_BASE,
            box(5, 3, 4, 11, 18, 12),
            box(0, 16, 0, 16, 48, 16)).optimize();

    public KinoDispenserBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BROWN)
                .strength(5.0F, 12.0F)
                .sound(SoundType.METAL)
                .noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new KinoDispenserBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof KinoDispenserBlockEntity dispenser) {
            NetworkHooks.openScreen(serverPlayer, menuProvider(pos, dispenser), pos);
        }
        return InteractionResult.CONSUME;
    }

    private static MenuProvider menuProvider(BlockPos pos, KinoDispenserBlockEntity dispenser) {
        return new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("screen.sgjdestiny_dhd.kino_dispenser");
            }

            @Nullable
            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
                ContainerData data = new ContainerData() {
                    @Override public int get(int index) { return dispenser.getStoredKinos(); }
                    @Override public void set(int index, int value) {}
                    @Override public int getCount() { return 1; }
                };
                return new KinoDispenserMenu(id, inventory, pos, dispenser, data);
            }
        };
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide()) {
            ensureInteractionTargets(level, pos);
        }
    }

    public static void ensureInteractionTargets(Level level, BlockPos pos) {
        for (int up = 1; up <= 2; up++) {
            BlockPos interactionPos = pos.above(up);
            if (level.getBlockState(interactionPos).canBeReplaced()) {
                level.setBlock(interactionPos,
                        ModBlocks.DESTINY_DHD_INTERACTION.get().defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.KINO_DISPENSER.get(), KinoDispenserBlockEntity::tick);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!state.is(replacement.getBlock())) {
            if (!level.isClientSide() && level.getBlockEntity(pos) instanceof KinoDispenserBlockEntity dispenser) {
                int stored = dispenser.takeAll();
                for (int i = 0; i < stored; i++) {
                    level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5,
                            pos.getZ() + 0.5, new ItemStack(ModItems.KINO.get())));
                    level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5,
                            pos.getZ() + 0.5, new ItemStack(ModItems.KINO_REMOTE.get())));
                }
            }
            for (int up = 1; up <= 2; up++) {
                BlockPos interactionPos = pos.above(up);
                if (level.getBlockState(interactionPos).is(ModBlocks.DESTINY_DHD_INTERACTION.get())) {
                    level.removeBlock(interactionPos, false);
                }
            }
        }
        super.onRemove(state, level, pos, replacement, moving);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, net.minecraft.core.BlockPos pos,
                               CollisionContext context) {
        return Shapes.block();
    }
}
