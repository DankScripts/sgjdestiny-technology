package com.dankscripts.sgjdestiny_dhd.item;

import com.dankscripts.sgjdestiny_dhd.block.DestinySafetyRailingBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Adds another edge to an existing railing instead of replacing it. */
public final class DestinySafetyRailingItem extends BlockItem {
    public DestinySafetyRailingItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        BlockPos railingPos = level.getBlockState(clicked).is(getBlock()) ? clicked
                : context.getClickedFace() == Direction.UP ? clicked.above() : clicked;
        BlockState state = level.getBlockState(railingPos);
        if (state.getBlock() instanceof DestinySafetyRailingBlock) {
            Player player = context.getPlayer();
            if (player == null || !level.mayInteract(player, railingPos)
                    || !player.mayUseItemAt(railingPos, Direction.UP, context.getItemInHand())) {
                return InteractionResult.FAIL;
            }
            Direction edge = DestinySafetyRailingBlock.nearestEmptyEdge(state, context.getClickLocation(), railingPos);
            if (edge == null) return InteractionResult.FAIL;
            if (!level.isClientSide()) {
                level.setBlock(railingPos, state.setValue(DestinySafetyRailingBlock.property(edge), true), Block.UPDATE_ALL);
                if (!player.getAbilities().instabuild) context.getItemInHand().shrink(1);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return super.useOn(context);
    }
}
