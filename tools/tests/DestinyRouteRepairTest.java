package com.dankscripts.sgjdestiny_dhd.compat;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.povstalec.sgjourney.common.sgjourney.*;
import java.util.Map;

/** Runs against the real SGJourney classes, including its keyless-template behavior. */
public final class DestinyRouteRepairTest {
    @SuppressWarnings("unchecked")
    private static <T> ResourceKey<T> key(ResourceKey<?> registry, String id) throws Exception {
        // Support the installed SRG runtime and the mapped development runtime.
        java.lang.reflect.Method create;
        try { create = ResourceKey.class.getMethod("create", ResourceKey.class, ResourceLocation.class); }
        catch (NoSuchMethodException e) { create = ResourceKey.class.getMethod("m_135785_", ResourceKey.class, ResourceLocation.class); }
        return (ResourceKey<T>) create.invoke(null, registry, new ResourceLocation(id));
    }
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        Class.forName("net.minecraft.SharedConstants").getMethod("m_142977_").invoke(null);
        Class.forName("net.minecraft.server.Bootstrap").getMethod("m_135870_").invoke(null);
        var mediora = key(Galaxy.REGISTRY_KEY, "sgjdestiny_dhd:mediora");
        var regionKey = key(AddressRegion.REGISTRY_KEY, "sgjourney:destiny_route");
        Address.Immutable local = new Address.Immutable(new int[]{1,8,15,21,30,35,0});
        Address.Immutable extra = new Address.Immutable(new int[]{2,9,16,23,27,32,35,0});
        AddressRegion region = new AddressRegion((ResourceKey) regionKey, false, "destiny", null, null, 26,
                new Address.Randomizable<>(extra), Map.of((ResourceKey<Galaxy>)(ResourceKey)mediora, new Address.Randomizable<>(local)));
        Galaxy template = new Galaxy(null, "mediora", new GalaxyType(6), null, 26, null, false, Map.of());
        template.addAddressRegion(local, region);
        check(region.getGalacticAddresses().containsKey(null), "Regression setup must reproduce corrupt null key");
        AddressRegion earth = new AddressRegion((ResourceKey) key(AddressRegion.REGISTRY_KEY,"sgjourney:earth"), false,
                "earth", null,null,1,new Address.Randomizable<>(extra),Map.of(
                    (ResourceKey<Galaxy>)(ResourceKey)key(Galaxy.REGISTRY_KEY,"sgjourney:milky_way"), new Address.Randomizable<>(local)));
        boolean crashed = false;
        try { region.findCommonGalaxy(null, earth); } catch (NullPointerException expected) { crashed = true; }
        check(crashed, "Old code must reproduce the reported Earth-dial exception");
        Galaxy fixed = template.copyTemplateWithKey((ResourceKey) mediora, Map.of(local, region));
        check(DestinyRouteRepair.repair(fixed,region,local), "Corruption must be repaired");
        check(!region.getGalacticAddresses().containsKey(null), "Null key removed");
        check(region.findCommonGalaxy(null,earth) == null, "Earth is correctly outside Mediora without crashing");
        check(fixed.getAddressRegion(local) == region, "Reverse address lookup restored");
        check(local.equals(region.getAddressInGalaxy((ResourceKey)mediora)), "Saved local address preserved");
        check(extra.equals(region.getExtragalacticAddress()), "Saved intergalactic address preserved");
        check(!DestinyRouteRepair.repair(fixed,region,local), "Repair is idempotent");
        region.serialize(); fixed.serialize();
        Galaxy empty = template.copyTemplateWithKey((ResourceKey)mediora,Map.of());
        check(DestinyRouteRepair.repair(empty,region,local), "Missing reverse index repaired");
        check(empty.getAddressRegion(local)==region, "Repaired reverse index resolves");
        System.out.println("PASS: reproduced Earth-dial null-key crash; keyed migration, route lookup, address preservation, repeat repair and serialization.");
    }
}
