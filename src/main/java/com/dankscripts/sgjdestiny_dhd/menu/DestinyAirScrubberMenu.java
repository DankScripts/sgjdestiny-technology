package com.dankscripts.sgjdestiny_dhd.menu;

import com.dankscripts.sgjdestiny_dhd.block_entity.DestinyOxygenGeneratorBlockEntity;
import com.dankscripts.sgjdestiny_dhd.registry.ModBlocks;
import com.dankscripts.sgjdestiny_dhd.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

/** Three-slot service inventory: water, calcite, and returned buckets. */
public final class DestinyAirScrubberMenu extends AbstractContainerMenu {
    private final BlockPos pos;
    private final DestinyOxygenGeneratorBlockEntity scrubber;
    private final ContainerData readings;

    public static DestinyAirScrubberMenu fromNetwork(int id, Inventory inventory, FriendlyByteBuf data) {
        return new DestinyAirScrubberMenu(id, inventory, data.readBlockPos(), null, new SimpleContainerData(7));
    }

    public DestinyAirScrubberMenu(int id, Inventory inventory, BlockPos pos,
                                  DestinyOxygenGeneratorBlockEntity scrubber, ContainerData readings) {
        super(ModMenus.DESTINY_AIR_SCRUBBER.get(), id);
        this.pos = pos;
        this.scrubber = scrubber;
        this.readings = readings;
        checkContainerDataCount(readings, 7);
        addDataSlots(readings);

        ItemStackHandler items = scrubber == null ? new ItemStackHandler(3) {
            @Override public boolean isItemValid(int slot, ItemStack stack) {
                return slot == 0 && stack.is(Items.WATER_BUCKET)
                        || slot == 1 && stack.is(Blocks.CALCITE.asItem());
            }
        } : scrubber.inventory();
        addSlot(new SlotItemHandler(items, 0, 35, 56));
        addSlot(new SlotItemHandler(items, 1, 80, 56));
        addSlot(new SlotItemHandler(items, 2, 125, 56) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 111 + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col, 8 + col * 18, 169));
    }

    public int waterBuckets() { return readings.get(0); }
    public int calcite() { return readings.get(1); }
    public int mixPercent() { return readings.get(2); }
    public boolean enabled() { return readings.get(3) != 0; }
    public int co2Percent() { return readings.get(4); }
    public boolean poweredOn() { return readings.get(5) != 0; }
    public boolean dryFault() { return readings.get(6) != 0; }

    @Override public boolean clickMenuButton(Player player, int id) {
        if (id != 0 || scrubber == null || !stillValid(player)) return false;
        scrubber.setPoweredOn(!scrubber.poweredOn());
        return true;
    }

    @Override public boolean stillValid(Player player) {
        return player.level().getBlockState(pos).is(ModBlocks.DESTINY_OXYGEN_GENERATOR.get())
                && player.distanceToSqr(pos.getX() + .5, pos.getY() + 1.5, pos.getZ() + .5) <= 64.0;
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack original = slot.getItem();
        ItemStack copy = original.copy();
        if (index < 3) {
            if (!moveItemStackTo(original, 3, 39, true)) return ItemStack.EMPTY;
        } else if (original.is(Items.WATER_BUCKET)) {
            if (!moveItemStackTo(original, 0, 1, false)) return ItemStack.EMPTY;
        } else if (original.is(Blocks.CALCITE.asItem())) {
            if (!moveItemStackTo(original, 1, 2, false)) return ItemStack.EMPTY;
        } else if (index < 30) {
            if (!moveItemStackTo(original, 30, 39, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(original, 3, 30, false)) return ItemStack.EMPTY;

        if (original.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, original);
        return copy;
    }
}
