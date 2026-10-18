package com.dankscripts.sgjdestiny_dhd.compat;

import net.povstalec.sgjourney.common.sgjourney.Address;
import net.povstalec.sgjourney.common.sgjourney.AddressRegion;
import net.povstalec.sgjourney.common.sgjourney.Galaxy;

/** Repairs only the supplied route region, preserving its valid saved address. */
public final class DestinyRouteRepair {
    private DestinyRouteRepair() {}

    public static boolean repair(Galaxy galaxy, AddressRegion region, Address.Immutable fallback) {
        var key = galaxy.getResourceKey();
        if (key == null) throw new IllegalArgumentException("Cannot register a route in a keyless galaxy template");
        Address.Immutable address = region.getAddressInGalaxy(key);
        if (address == null) address = fallback;
        if (address == null) return false;
        boolean changed = region.getGalacticAddresses().containsKey(null);
        region.removeFromGalaxy(null);
        if (galaxy.getAddressRegion(address) != region) {
            // Remove stale aliases for this same region, never another destination.
            for (var entry : galaxy.getAddressRegions(e -> region.getResourceKey().equals(e.getValue().getResourceKey())))
                galaxy.removeAddressRegion(entry.getKey());
            galaxy.addAddressRegion(address, region);
            changed = true;
        }
        return changed;
    }
}
