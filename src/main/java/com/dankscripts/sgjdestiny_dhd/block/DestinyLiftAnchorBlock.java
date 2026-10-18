package com.dankscripts.sgjdestiny_dhd.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Hidden pit marker survives structure export, which deliberately omits entities. */
public final class DestinyLiftAnchorBlock extends Block {
    public static final IntegerProperty STOP = IntegerProperty.create("stop", 0, 7);
    public DestinyLiftAnchorBlock() {
        super(Properties.copy(Blocks.OBSIDIAN));
        registerDefaultState(stateDefinition.any().setValue(STOP, 0));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(STOP); }
}
