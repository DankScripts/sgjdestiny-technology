package com.dankscripts.sgjdestiny_dhd.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;

/** Click the center floor tile at the doorway. */
public final class DestinyCorridorItem extends BlockItem {
    public DestinyCorridorItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public BlockPlaceContext updatePlacementContext(BlockPlaceContext context) {
        Direction forward = context.getHorizontalDirection();
        BlockPos anchor = context.getClickedPos().relative(forward.getClockWise(), 2);
        return BlockPlaceContext.at(context, anchor, context.getClickedFace());
    }
}
