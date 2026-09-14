package com.dankscripts.sgjdestiny_dhd.menu;

import com.dankscripts.sgjdestiny_dhd.block_entity.KinoDispenserBlockEntity;
import com.dankscripts.sgjdestiny_dhd.registry.ModBlocks;
import com.dankscripts.sgjdestiny_dhd.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

public final class KinoDispenserMenu extends AbstractContainerMenu {
    public static final int DEPOSIT = 0;
    public static final int PICKUP = 1;
    private final BlockPos pos;
    private final KinoDispenserBlockEntity dispenser;
    private final ContainerData data;

    public static KinoDispenserMenu fromNetwork(int id, Inventory inventory, FriendlyByteBuf buffer) {
        return new KinoDispenserMenu(id, inventory, buffer.readBlockPos(), null, new SimpleContainerData(1));
    }

    public KinoDispenserMenu(int id, Inventory inventory, BlockPos pos,
                             KinoDispenserBlockEntity dispenser, ContainerData data) {
        super(ModMenus.KINO_DISPENSER.get(), id);
        this.pos = pos;
        this.dispenser = dispenser;
        this.data = data;
        checkContainerDataCount(data, 1);
        addDataSlots(data);
    }

    public int getStoredKinos() {
        return data.get(0);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (dispenser == null) return false;
        if (id == DEPOSIT) return dispenser.deposit(player);
        if (id == PICKUP) return dispenser.pickup(player);
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockState(pos).is(ModBlocks.KINO_DISPENSER.get())
                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
