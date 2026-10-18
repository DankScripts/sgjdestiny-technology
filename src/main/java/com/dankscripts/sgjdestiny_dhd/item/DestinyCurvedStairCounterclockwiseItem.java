package com.dankscripts.sgjdestiny_dhd.item;

import java.util.Map;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/** A second item for the mirrored state of the same stair block. */
public final class DestinyCurvedStairCounterclockwiseItem extends BlockItem {
    public DestinyCurvedStairCounterclockwiseItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public String getDescriptionId() {
        return "item.sgjdestiny_dhd.destiny_curved_stair_counterclockwise";
    }

    @Override
    public void registerBlocks(Map<Block, Item> items, Item item) {
        // Keep the original left item as the block's default item. The right
        // item is selected explicitly by its own recipe or creative entry.
    }
}
