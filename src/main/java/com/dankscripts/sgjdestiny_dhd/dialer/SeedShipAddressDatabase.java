package com.dankscripts.sgjdestiny_dhd.dialer;

import com.dankscripts.sgjdestiny_dhd.compat.DestinyRouteEvents;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.povstalec.sgjourney.common.block_entities.dhd.UniverseDHDEntity;
import net.povstalec.sgjourney.common.block_entities.stargate.UniverseStargateEntity;
import net.povstalec.sgjourney.common.data.StargateNetwork;
import net.povstalec.sgjourney.common.data.Universe;
import net.povstalec.sgjourney.common.init.StargateInit;
import net.povstalec.sgjourney.common.sgjourney.Address;
import net.povstalec.sgjourney.common.sgjourney.AddressRegion;
import net.povstalec.sgjourney.common.sgjourney.Galaxy;
import net.povstalec.sgjourney.common.sgjourney.stargate.Stargate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Server-authoritative view of SGJourney's registered Universe gates. */
public final class SeedShipAddressDatabase {
    private SeedShipAddressDatabase() {}

    public record Entry(Component name, Address.Immutable address, boolean earth) {}

    public static List<Entry> entriesFor(MinecraftServer server, UniverseDHDEntity dhd) {
        Stargate source = dhd.stargateCache.get() == null ? null : dhd.stargateCache.get().getStargate();
        return entriesFor(server, source);
    }

    public static List<Entry> entriesFor(MinecraftServer server, UniverseStargateEntity gate) {
        return entriesFor(server, gate.getStargate());
    }

    private static List<Entry> entriesFor(MinecraftServer server, Stargate source) {
        DestinyRouteEvents.ensureRoutes(server);
        List<Entry> result = new ArrayList<>();
        EarthGateData.get(server).resolve(server).ifPresent(earth -> result.add(
                new Entry(Component.translatable("destination.sgjdestiny_dhd.earth"),
                        earth.get9ChevronAddress(), true)));
        if (source == null || source.getAddressRegion() == null) return List.copyOf(result);

        AddressRegion sourceRegion = source.getAddressRegion();
        Map<Address.Immutable, Entry> entries = new LinkedHashMap<>();
        // Destiny's seed-ship database is preprogrammed. Read its known route
        // directly from SGJourney's Universe data so unloaded destination gate
        // chunks do not make records vanish from the console.
        Universe universe = Universe.get(server);
        ResourceKey<Galaxy> kaliem = ResourceKey.create(Galaxy.REGISTRY_KEY,
                new ResourceLocation("sgjourney", "kaliem"));
        // Destiny also has an ordinary galactic address. Keep it in the
        // handheld's seeded route table so expedition teams can return to the
        // ship with seven chevrons even while its gate dimension is unloaded.
        addKnownRegion(universe, kaliem, "destiny_route", entries);
        addKnownRegion(universe, kaliem, "calcite_planet", entries);
        addKnownRegion(universe, kaliem, "water_planet", entries);
        addKnownRegion(universe, kaliem, "jungle_planet", entries);
        StargateNetwork network = StargateNetwork.get(server);
        for (var level : server.getAllLevels()) {
            for (Stargate destination : network.getStargatesInDimension(level.dimension())) {
                // Seven symbols alone cannot distinguish the three gate families.
                if (destination == source || destination.getStargateType() != StargateInit.UNIVERSE.get()) continue;
                Address.Immutable address = destination.getConnectionAddress(sourceRegion, Address.Type.ADDRESS_7_CHEVRON);
                if (address == null || address.getType() != Address.Type.ADDRESS_7_CHEVRON || !address.canBeDialed()) continue;
                AddressRegion region = destination.getAddressRegion();
                Component name = region == null ? Component.literal("SEED DESTINATION") : region.getTranslatedName();
                entries.putIfAbsent(address, new Entry(name, address, false));
            }
        }
        List<Entry> universeEntries = new ArrayList<>(entries.values());
        universeEntries.sort(Comparator.comparing(entry -> entry.name().getString(), String.CASE_INSENSITIVE_ORDER));
        result.addAll(universeEntries);
        return List.copyOf(result);
    }

    private static void addKnownRegion(Universe universe, ResourceKey<Galaxy> galaxy, String name,
                                       Map<Address.Immutable, Entry> entries) {
        ResourceKey<AddressRegion> key = ResourceKey.create(AddressRegion.REGISTRY_KEY,
                new ResourceLocation("sgjourney", name));
        AddressRegion region = universe.getAddressRegionFromKey(key);
        Address.Immutable address = universe.getAddressInGalaxyFromAddressRegionKey(galaxy, key);
        if (region != null && address != null && address.getType() == Address.Type.ADDRESS_7_CHEVRON) {
            entries.putIfAbsent(address, new Entry(region.getTranslatedName(), address, false));
        }
    }

    public static boolean isAuthorizedSelection(MinecraftServer server, UniverseDHDEntity dhd, Address address) {
        return entriesFor(server, dhd).stream().anyMatch(entry -> entry.address().equals(address));
    }

    public static boolean isAuthorizedSelection(MinecraftServer server, UniverseStargateEntity gate, Address address) {
        return entriesFor(server, gate).stream().anyMatch(entry -> entry.address().equals(address));
    }
}
