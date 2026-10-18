package com.dankscripts.sgjdestiny_dhd.menu;

import com.dankscripts.sgjdestiny_dhd.block_entity.DestinyControlHubBlockEntity;
import com.dankscripts.sgjdestiny_dhd.compat.DestinyAtmosphere;
import com.dankscripts.sgjdestiny_dhd.registry.ModBlocks;
import com.dankscripts.sgjdestiny_dhd.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

/** Ship readouts; water comes from loaded tanks and CO2 from Destiny's atmosphere. */
public final class DestinyControlHubMenu extends AbstractContainerMenu {
    private final BlockPos pos;
    private final DestinyControlHubBlockEntity hub;
    private final ContainerData readings;

    public static DestinyControlHubMenu fromNetwork(int id, Inventory inventory, FriendlyByteBuf data) {
        return new DestinyControlHubMenu(id, inventory, data.readBlockPos(), null, new SimpleContainerData(9));
    }

    public DestinyControlHubMenu(int id, Inventory inventory, BlockPos pos,
                                 DestinyControlHubBlockEntity hub, ContainerData readings) {
        super(ModMenus.DESTINY_CONTROL_HUB.get(), id);
        this.pos = pos;
        this.hub = hub;
        this.readings = readings;
        checkContainerDataCount(readings, 9);
        addDataSlots(readings);
    }

    public int reading(int index) { return readings.get(index); }
    public boolean liveAtmosphere() { return readings.get(3) != 0; }
    public int scrubberStatus() { return readings.get(4); }
    public int scrubberCount() { return readings.get(8); }
    public boolean waterTankPresent() { return readings.get(5) != 0; }
    public int waterBuckets() { return readings.get(6); }
    public int waterCapacityBuckets() { return readings.get(7); }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (hub == null || !stillValid(player)) return false;
        if (id == 6) {
            if (!(player.level() instanceof ServerLevel server) || !DestinyAtmosphere.isDestiny(server))
                return false;
            DestinyAtmosphere.resetScrubbers(server);
            return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockState(pos).is(ModBlocks.DESTINY_CONTROL_HUB.get())
                && player.distanceToSqr(pos.getX() + .5, pos.getY() + 1.5, pos.getZ() + .5) <= 100.0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
}
