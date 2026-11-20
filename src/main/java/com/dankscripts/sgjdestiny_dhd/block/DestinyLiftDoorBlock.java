package com.dankscripts.sgjdestiny_dhd.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;

/** Four cells form a 2.5-block doorway; the upper cells extend into the slab space. */
public final class DestinyLiftDoorBlock extends Block {
    public static final BooleanProperty OPEN = BooleanProperty.create("open");
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 3);
    public static final IntegerProperty TURN = IntegerProperty.create("turn", 0, 3);
    public DestinyLiftDoorBlock() {
        super(Properties.of().strength(5, 12).sound(SoundType.METAL).noOcclusion().noLootTable()
                .pushReaction(PushReaction.BLOCK));
        registerDefaultState(stateDefinition.any().setValue(OPEN, false).setValue(PART, 0).setValue(TURN, 0));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OPEN, PART, TURN);
    }
    @Override public BlockState rotate(BlockState state, Rotation rotation) {
        int turns = switch (rotation) {
            case CLOCKWISE_90 -> 1;
            case CLOCKWISE_180 -> 2;
            case COUNTERCLOCKWISE_90 -> 3;
            default -> 0;
        };
        return state.setValue(TURN, (state.getValue(TURN) + turns) % 4);
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        boolean upper = state.getValue(PART) >= 2;
        double height = upper ? 24 : 16;
        VoxelShape shape;
        if (!state.getValue(OPEN)) shape = box(12, 0, 0, 16, height, 16);
        else {
        VoxelShape side = (state.getValue(PART) & 1) == 0
                ? box(12, 0, 0, 16, height, 1.5) : box(12, 0, 14.5, 16, height, 16);
        shape = upper ? Shapes.or(side, box(12, 23, 0, 16, 24, 16)) : side;
        }
        for (int turn = 0; turn < state.getValue(TURN); turn++) {
            VoxelShape rotated = Shapes.empty();
            for (var part : shape.toAabbs())
                rotated = Shapes.or(rotated, box((1 - part.maxZ) * 16, part.minY * 16, part.minX * 16,
                        (1 - part.minZ) * 16, part.maxY * 16, part.maxX * 16));
            shape = rotated.optimize();
        }
        return shape;
    }
}
