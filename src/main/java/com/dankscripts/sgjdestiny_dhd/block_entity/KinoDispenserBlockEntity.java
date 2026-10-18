package com.dankscripts.sgjdestiny_dhd.block_entity;

import com.dankscripts.sgjdestiny_dhd.registry.ModBlockEntities;
import com.dankscripts.sgjdestiny_dhd.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import com.dankscripts.sgjdestiny_dhd.block.KinoDispenserBlock;

public final class KinoDispenserBlockEntity extends BlockEntity {
    public static final int CAPACITY = 64;
    private static final int INITIAL_KINO_KITS = 2;
    private int storedKinos = INITIAL_KINO_KITS;

    public KinoDispenserBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.KINO_DISPENSER.get(), pos, state);
    }

    public int getStoredKinos() {
        return storedKinos;
    }

    public boolean deposit(Player player) {
        if (storedKinos >= CAPACITY) return false;
        int kinoSlot = findSlot(player, ModItems.KINO.get());
        int remoteSlot = findSlot(player, ModItems.KINO_REMOTE.get());
        if (kinoSlot < 0 || remoteSlot < 0) return false;

        if (!player.getAbilities().instabuild) {
            player.getInventory().getItem(kinoSlot).shrink(1);
            player.getInventory().getItem(remoteSlot).shrink(1);
        }
        storedKinos++;
        markChanged();
        return true;
    }

    public boolean pickup(Player player) {
        if (storedKinos <= 0) return false;
        if (!player.getAbilities().instabuild && emptyInventorySlots(player) < 2) return false;

        if (!player.getInventory().add(new ItemStack(ModItems.KINO.get()))) return false;
        if (!player.getInventory().add(new ItemStack(ModItems.KINO_REMOTE.get()))) return false;
        storedKinos--;
        markChanged();
        return true;
    }

    private static int findSlot(Player player, net.minecraft.world.item.Item item) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (player.getInventory().getItem(slot).is(item)) return slot;
        }
        return -1;
    }

    private static int emptyInventorySlots(Player player) {
        int empty = 0;
        for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
            if (player.getInventory().getItem(slot).isEmpty()) empty++;
        }
        return empty;
    }

    public int takeAll() {
        int result = storedKinos;
        storedKinos = 0;
        markChanged();
        return result;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, KinoDispenserBlockEntity dispenser) {
        if (!level.isClientSide() && level.getGameTime() % 20L == 0L) {
            KinoDispenserBlock.ensureInteractionTargets(level, pos);
        }
    }

    private void markChanged() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("StoredKinos", storedKinos);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        storedKinos = tag.contains("StoredKinos")
                ? Math.max(0, Math.min(CAPACITY, tag.getInt("StoredKinos")))
                : INITIAL_KINO_KITS;
    }
}
