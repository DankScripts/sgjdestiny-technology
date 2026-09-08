package com.dankscripts.sgjdestiny_dhd.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.povstalec.sgjourney.common.blocks.ChevronBlock;

/** A steady illuminated Universe chevron that is not controlled by redstone. */
public final class LitDestinyFloorChevronBlock extends ChevronBlock {
    public LitDestinyFloorChevronBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_ORANGE)
                .strength(3.0F, 6.0F)
                .sound(SoundType.METAL)
                .noOcclusion()
                .lightLevel(state -> 15));
        registerDefaultState(defaultBlockState().setValue(LIT, true));
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos,
                                Block neighbor, BlockPos neighborPos, boolean moving) {
        // The Destiny controller owns this state. ChevronBlock's implementation
        // interprets it as redstone and schedules an automatic shutoff.
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Ignore stale shutoff ticks scheduled while this was SGJourney's block.
    }
}
