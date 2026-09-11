package com.dankscripts.sgjdestiny_dhd.dialer;

import com.dankscripts.sgjdestiny_dhd.compat.DestinyRouteEvents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger LOGGER = LoggerFactory.getLogger("SGJ Destiny DHD/Address Database");

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
        Map<Address.Immutable, Entry> entries = new LinkedHashMap<>();
        // Destiny's seed-ship database is preprogrammed. Read its known route
        // directly from SGJourney's Universe data so unloaded destination gate
        // chunks do not make records vanish from the console.
        Universe universe = Universe.get(server);
        ResourceKey<Galaxy> mediora = ResourceKey.create(Galaxy.REGISTRY_KEY,
                new ResourceLocation("sgjdestiny_dhd", "mediora"));
        // Destiny also has an ordinary galactic address. Keep it in the
        // handheld's seeded route table so expedition teams can return to the
        // ship with seven chevrons even while its gate dimension is unloaded.
        addKnownRegion(server, universe, mediora, "destiny_route", entries);
        addKnownRegion(server, universe, mediora, "calcite_planet", entries);
        addKnownRegion(server, universe, mediora, "water_planet", entries);
        addKnownRegion(server, universe, mediora, "jungle_planet", entries);
        addKnownRegion(server, universe, mediora, "justice_planet", entries);

        // The seeded route belongs to the Destiny database and must remain
        // visible while its physical gate or DHD cache is still loading.
        // Network discovery supplements that route only when a source region
        // is available.
        if (source != null && source.getAddressRegion() != null) {
            AddressRegion sourceRegion = source.getAddressRegion();
            StargateNetwork network = StargateNetwork.get(server);
            for (var level : server.getAllLevels()) {
                for (Stargate destination : network.getStargatesInDimension(level.dimension())) {
                    // Seven symbols alone cannot distinguish the three gate families.
                    if (destination == source || destination.getStargateType() != StargateInit.UNIVERSE.get()) continue;
                    Address.Immutable address;
                    try {
                        address = destination.getConnectionAddress(sourceRegion, Address.Type.ADDRESS_7_CHEVRON);
                    } catch (RuntimeException exception) {
                        // Large modpacks can retain legacy SGJourney records whose
                        // internal galaxy map contains a null key. One broken gate
                        // must not prevent the seeded Destiny database from opening.
                        LOGGER.warn("Skipped an unreadable Universe Stargate record in {} while building the Destiny address list",
                                level.dimension().location());
                        continue;
                    }
                    if (address == null || address.getType() != Address.Type.ADDRESS_7_CHEVRON || !address.canBeDialed()) continue;
                    AddressRegion region = destination.getAddressRegion();
                    Component name = region == null ? Component.literal("SEED DESTINATION") : region.getTranslatedName();
                    entries.putIfAbsent(address, new Entry(name, address, false));
                }
            }
        }
        List<Entry> universeEntries = new ArrayList<>(entries.values());
        universeEntries.sort(Comparator.comparing(entry -> entry.name().getString(), String.CASE_INSENSITIVE_ORDER));
        result.addAll(universeEntries);
        return List.copyOf(result);
    }

    private static void addKnownRegion(MinecraftServer server, Universe universe,
                                       ResourceKey<Galaxy> galaxy, String name,
                                       Map<Address.Immutable, Entry> entries) {
        ResourceKey<AddressRegion> key = ResourceKey.create(AddressRegion.REGISTRY_KEY,
                new ResourceLocation("sgjourney", name));
        AddressRegion region = universe.getAddressRegionFromKey(key);
        Address.Immutable address = universe.getAddressInGalaxyFromAddressRegionKey(galaxy, key);
        // SGJourney's saved reverse index may not expose a newly introduced
        // custom galaxy in an existing world. The datapack definition remains
        // authoritative and contains the fixed six destination symbols.
        if (address == null) {
            AddressRegion definition = server.registryAccess().registryOrThrow(AddressRegion.REGISTRY_KEY).get(key);
            if (definition != null) {
                if (region == null) region = definition;
                Address.Immutable definedAddress = definition.getAddressInGalaxy(galaxy);
                if (definedAddress != null) address = Address.Immutable.extendWithPointOfOrigin(definedAddress);
            }
        }
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
