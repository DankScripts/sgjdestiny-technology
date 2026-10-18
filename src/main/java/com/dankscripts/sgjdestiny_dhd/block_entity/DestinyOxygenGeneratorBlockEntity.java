package com.dankscripts.sgjdestiny_dhd.block_entity;

import com.dankscripts.sgjdestiny_dhd.block.DestinyOxygenGeneratorBlock;
import com.dankscripts.sgjdestiny_dhd.compat.DestinyAtmosphere;
import com.dankscripts.sgjdestiny_dhd.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.items.ItemStackHandler;

/** Water/calcite service inventory and CO2 scrubbing mix in one recessed tank bay. */
public final class DestinyOxygenGeneratorBlockEntity extends BlockEntity {
    public static final int MAX_WATER = 16000;
    public static final int MAX_CALCITE = 64;
    public static final int MIX_PER_BATCH = 24000;
    public static final int MAX_SCRUBBER_MIX = 48000;
    private int water;
    private int calcite;
    private int scrubberMix;
    private boolean poweredOn = true;
    private boolean dryFault;
    // Water bucket, calcite, and returned empty buckets.
    private final ItemStackHandler inventory = new ItemStackHandler(3) {
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case 0 -> stack.is(Items.WATER_BUCKET);
                case 1 -> stack.is(Blocks.CALCITE.asItem());
                default -> false;
            };
        }

        @Override protected void onContentsChanged(int slot) { setChanged(); }
    };
    private final ContainerData menuData = new ContainerData() {
        @Override public int get(int index) {
            return switch (index) {
                case 0 -> water / 1000;
                case 1 -> calcite;
                case 2 -> scrubberMix * 100 / MAX_SCRUBBER_MIX;
                case 3 -> enabled() ? 1 : 0;
                case 4 -> level instanceof ServerLevel server && DestinyAtmosphere.isDestiny(server)
                        ? DestinyAtmosphere.co2Percent(server) : -1;
                case 5 -> poweredOn ? 1 : 0;
                case 6 -> dryFault ? 1 : 0;
                default -> 0;
            };
        }

        @Override public void set(int index, int value) {}
        @Override public int getCount() { return 7; }
    };

    public DestinyOxygenGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DESTINY_OXYGEN_GENERATOR.get(), pos, state);
    }

    public int water() { return water; }
    public int calcite() { return calcite; }
    public int scrubberMix() { return scrubberMix; }
    public boolean poweredOn() { return poweredOn; }
    public boolean dryFault() { return dryFault; }
    public boolean enabled() {
        return level instanceof ServerLevel server && DestinyAtmosphere.isDestiny(server)
                && poweredOn && !dryFault;
    }
    public void setPoweredOn(boolean value) {
        if (poweredOn == value) return;
        poweredOn = value;
        setChanged();
    }

    /** A dry unit stays faulted after being refilled until reset from the control room. */
    public void latchDryFaultIfEmpty() {
        if (poweredOn && !dryFault && scrubberMix == 0 && (water < 1000 || calcite < 1)) {
            dryFault = true;
            setChanged();
        }
    }

    public boolean resetAfterRefill() {
        ingestInputs();
        if (!dryFault || scrubberMix == 0 && (water < 1000 || calcite < 1)) return false;
        dryFault = false;
        setChanged();
        return true;
    }
    public ItemStackHandler inventory() { return inventory; }
    public ContainerData menuData() { return menuData; }

    /** Move accepted slot items into the existing reservoir without losing empty buckets. */
    public void ingestInputs() {
        if (level == null || level.isClientSide()) return;
        ItemStack waterInput = inventory.getStackInSlot(0);
        ItemStack bucketOutput = inventory.getStackInSlot(2);
        if (waterInput.is(Items.WATER_BUCKET) && water <= MAX_WATER - 1000
                && (bucketOutput.isEmpty() || bucketOutput.is(Items.BUCKET)
                && bucketOutput.getCount() < bucketOutput.getMaxStackSize())) {
            inventory.extractItem(0, 1, false);
            inventory.setStackInSlot(2, bucketOutput.isEmpty()
                    ? new ItemStack(Items.BUCKET) : new ItemStack(Items.BUCKET, bucketOutput.getCount() + 1));
            water += 1000;
            setChanged();
        }
        if (calcite < MAX_CALCITE && inventory.getStackInSlot(1).is(Blocks.CALCITE.asItem())) {
            inventory.extractItem(1, 1, false);
            calcite++;
            setChanged();
        }
    }

    public static void tick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state,
                            DestinyOxygenGeneratorBlockEntity bay) {
        if (!level.isClientSide() && level.getGameTime() % 20 == 0) {
            DestinyOxygenGeneratorBlock.removeLegacyParts(level, pos, state);
            bay.ingestInputs();
        }
    }

    public void dropInventory() {
        if (level == null || level.isClientSide()) return;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty()) Block.popResource(level, worldPosition, stack.copy());
        }
    }

    public boolean addWater() {
        if (water > MAX_WATER - 1000) return false;
        water += 1000;
        setChanged();
        return true;
    }

    public boolean addCalcite() {
        if (calcite >= MAX_CALCITE) return false;
        calcite++;
        setChanged();
        return true;
    }

    /** A batch runs only when there is room; no input is silently wasted. */
    public boolean processBatch() {
        if (!enabled() || water < 1000 || calcite < 1 || scrubberMix > MAX_SCRUBBER_MIX - MIX_PER_BATCH) return false;
        water -= 1000;
        calcite--;
        scrubberMix += MIX_PER_BATCH;
        setChanged();
        return true;
    }

    public int scrub(int requested) {
        int amount = Math.min(Math.max(requested, 0), scrubberMix);
        if (amount != 0) { scrubberMix -= amount; setChanged(); }
        return amount;
    }

    @Override public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel && DestinyAtmosphere.isDestiny(serverLevel))
            DestinyAtmosphere.register(serverLevel, worldPosition);
    }

    @Override public void setRemoved() {
        if (level instanceof ServerLevel serverLevel) DestinyAtmosphere.unregister(serverLevel, worldPosition);
        super.setRemoved();
    }

    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Water", water);
        tag.putInt("Calcite", calcite);
        tag.putInt("ScrubberMix", scrubberMix);
        tag.putBoolean("PoweredOn", poweredOn);
        tag.putBoolean("DryFault", dryFault);
        tag.put("Inventory", inventory.serializeNBT());
    }

    @Override public void load(CompoundTag tag) {
        super.load(tag);
        water = Math.max(0, Math.min(MAX_WATER, tag.getInt("Water")));
        calcite = Math.max(0, Math.min(MAX_CALCITE, tag.getInt("Calcite")));
        // Preserve prepared material in existing worlds that used the old oxygen key.
        int storedMix = tag.contains("ScrubberMix") ? tag.getInt("ScrubberMix") : tag.getInt("Oxygen");
        scrubberMix = Math.max(0, Math.min(MAX_SCRUBBER_MIX, storedMix));
        poweredOn = !tag.contains("PoweredOn") || tag.getBoolean("PoweredOn");
        dryFault = tag.getBoolean("DryFault");
        if (tag.contains("Inventory")) inventory.deserializeNBT(tag.getCompound("Inventory"));
    }
}
