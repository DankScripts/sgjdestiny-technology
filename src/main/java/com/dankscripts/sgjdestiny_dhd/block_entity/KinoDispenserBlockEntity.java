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
    private int storedKinos;

    public KinoDispenserBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.KINO_DISPENSER.get(), pos, state);
    }

    public int getStoredKinos() {
        return storedKinos;
    }

    public boolean deposit(Player player) {
        if (storedKinos >= CAPACITY) return false;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(ModItems.KINO.get())) {
                if (!player.getAbilities().instabuild) stack.shrink(1);
                storedKinos++;
                markChanged();
                return true;
            }
        }
        return false;
    }

    public boolean pickup(Player player) {
        if (storedKinos <= 0) return false;
        ItemStack kino = new ItemStack(ModItems.KINO.get());
        if (!player.getInventory().add(kino)) return false;
        storedKinos--;
        markChanged();
        return true;
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
        storedKinos = Math.max(0, Math.min(CAPACITY, tag.getInt("StoredKinos")));
    }
}
