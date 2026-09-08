package com.dankscripts.sgjdestiny_dhd.dialer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.povstalec.sgjourney.common.data.StargateNetwork;
import net.povstalec.sgjourney.common.init.StargateInit;
import net.povstalec.sgjourney.common.sgjourney.Address;
import net.povstalec.sgjourney.common.sgjourney.stargate.Stargate;

import java.util.Optional;

/** The one server-wide Milky Way gate authorized as Destiny's Earth endpoint. */
public final class EarthGateData extends SavedData {
    private static final String FILE_NAME = "sgjdestiny_earth_gate";
    private static final String ADDRESS = "EarthGateAddress";
    private int[] symbols;

    public static EarthGateData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(EarthGateData::load,
                EarthGateData::new, FILE_NAME);
    }

    public static EarthGateData load(CompoundTag tag) {
        EarthGateData data = new EarthGateData();
        int[] saved = tag.getIntArray(ADDRESS);
        data.symbols = saved.length == 0 ? null : saved;
        return data;
    }

    public void assign(Address address) {
        symbols = address.toArray();
        setDirty();
    }

    public Optional<Stargate> resolve(MinecraftServer server) {
        if (symbols == null) return Optional.empty();
        try {
            Address.Immutable saved = new Address.Immutable(symbols);
            StargateNetwork network = StargateNetwork.get(server);
            Stargate gate = network.getStargate(saved);
            if (gate != null && gate.getStargateType() == StargateInit.MILKY_WAY.get()) {
                return Optional.of(gate);
            }
            // Some SGJourney versions index the network by the local seven-chevron
            // address. Compare the stable nine-chevron address as a fallback.
            for (var level : server.getAllLevels()) {
                for (Stargate candidate : network.getStargatesInDimension(level.dimension())) {
                    if (candidate.getStargateType() == StargateInit.MILKY_WAY.get()
                            && saved.equals(candidate.get9ChevronAddress())) return Optional.of(candidate);
                }
            }
        } catch (IllegalArgumentException ignored) {}
        return Optional.empty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        if (symbols != null) tag.putIntArray(ADDRESS, symbols);
        return tag;
    }
}
