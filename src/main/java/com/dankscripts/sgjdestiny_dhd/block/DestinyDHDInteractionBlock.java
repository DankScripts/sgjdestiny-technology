package com.dankscripts.sgjdestiny_dhd.block;

import com.dankscripts.sgjdestiny_dhd.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Invisible, non-colliding screen target that forwards interaction to the console below it. */
public final class DestinyDHDInteractionBlock extends Block {
    public DestinyDHDInteractionBlock() {
        super(BlockBehaviour.Properties.of().noCollission().noOcclusion().strength(-1.0F, 3_600_000.0F));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        for (int down = 1; down <= 2; down++) {
            BlockPos controllerPos = pos.below(down);
            BlockState controllerState = level.getBlockState(controllerPos);
            if (controllerState.is(ModBlocks.DESTINY_DHD.get())) {
                return controllerState.use(level, player, hand,
                        new BlockHitResult(hit.getLocation(), hit.getDirection(), controllerPos, hit.isInside()));
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                        CollisionContext context) {
        return Shapes.empty();
    }
}
