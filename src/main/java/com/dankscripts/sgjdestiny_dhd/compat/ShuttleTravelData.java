package com.dankscripts.sgjdestiny_dhd.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** The two fixed shuttle berths for this world. Eden is chosen once per save. */
public final class ShuttleTravelData extends SavedData {
    private static final String FILE_NAME = "sgjdestiny_shuttle_route";
    private BlockPos destiny;
    private BlockPos eden;
    private int edenSearchCandidate;

    public static ShuttleTravelData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                ShuttleTravelData::load, ShuttleTravelData::new, FILE_NAME);
    }

    private static ShuttleTravelData load(CompoundTag tag) {
        ShuttleTravelData data = new ShuttleTravelData();
        if (tag.contains("Destiny")) data.destiny = BlockPos.of(tag.getLong("Destiny"));
        if (tag.contains("Eden")) data.eden = BlockPos.of(tag.getLong("Eden"));
        data.edenSearchCandidate = tag.getInt("EdenSearchCandidate");
        return data;
    }

    public BlockPos destiny() { return destiny; }
    public BlockPos eden() { return eden; }
    public int edenSearchCandidate() { return edenSearchCandidate; }

    public void setDestiny(BlockPos pos) {
        if (!pos.equals(destiny)) { destiny = pos.immutable(); setDirty(); }
    }

    public void setEden(BlockPos pos) {
        if (!pos.equals(eden)) { eden = pos.immutable(); setDirty(); }
        setEdenSearchCandidate(0);
    }

    public void setEdenSearchCandidate(int candidate) {
        if (edenSearchCandidate != candidate) { edenSearchCandidate = candidate; setDirty(); }
    }

    @Override public CompoundTag save(CompoundTag tag) {
        if (destiny != null) tag.putLong("Destiny", destiny.asLong());
        if (eden != null) tag.putLong("Eden", eden.asLong());
        tag.putInt("EdenSearchCandidate", edenSearchCandidate);
        return tag;
    }
}
