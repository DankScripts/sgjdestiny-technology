package com.dankscripts.sgjdestiny_dhd.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

/** Stationary collision/light for boarding; the cab entity owns the visible floor. */
public final class DestinyLiftSupportBlock extends Block {
    public DestinyLiftSupportBlock() {
        super(Properties.copy(Blocks.SEA_LANTERN).noOcclusion().noLootTable());
    }

    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }
}
