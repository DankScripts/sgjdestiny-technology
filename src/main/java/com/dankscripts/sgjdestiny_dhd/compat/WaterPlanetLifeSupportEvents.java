package com.dankscripts.sgjdestiny_dhd.compat;

import com.dankscripts.sgjdestiny_dhd.registry.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;

/** Water Planet hazards and suit life support for oxygen-free Destiny exterior. */
@EventBusSubscriber(modid = "sgjdestiny_dhd", bus = EventBusSubscriber.Bus.GAME)
public final class WaterPlanetLifeSupportEvents {
    private static final ResourceLocation WATER_PLANET = ResourceLocation.fromNamespaceAndPath("sgjourney", "water_planet");
    private static final java.util.Map<java.util.UUID, Long> ENTRY_SAFETY_UNTIL = new java.util.HashMap<>();
    private static final java.util.Map<java.util.UUID, Integer> DESTINY_EXPOSURE = new java.util.HashMap<>();
    private WaterPlanetLifeSupportEvents() {}

    @SubscribeEvent
    public static void safeLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !player.level().dimension().location().equals(WATER_PLANET)) return;
        moveToSafeSurface(player);
        grantEntryProtection(player);
    }

    @SubscribeEvent
    public static void safeArrival(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && event.getTo().location().equals(WATER_PLANET)) {
            moveToSafeSurface(player);
            ENTRY_SAFETY_UNTIL.put(player.getUUID(), player.serverLevel().getGameTime() + 240L);
            grantEntryProtection(player);
        }
    }

    private static void moveToSafeSurface(ServerPlayer player) {
        int x = player.getBlockX();
        int z = player.getBlockZ();
        int y = player.serverLevel().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        player.teleportTo(x + 0.5, y + 1.0, z + 0.5);
        player.fallDistance = 0.0F;
    }

    private static void grantEntryProtection(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 240, 0, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 240, 4, false, false, true));
    }

    @SubscribeEvent
    public static void protectExplorers(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        boolean destiny = DestinyAtmosphere.isDestiny(player.serverLevel());
        if (!destiny && !player.level().dimension().location().equals(WATER_PLANET)) {
            DESTINY_EXPOSURE.remove(player.getUUID());
            return;
        }
        if (destiny && DestinyAtmosphere.breathable(player.serverLevel(), player.blockPosition())) {
            DESTINY_EXPOSURE.remove(player.getUUID());
            player.setAirSupply(player.getMaxAirSupply());
            if (!player.isCreative() && !player.isSpectator() && player.tickCount % 40 == 0) {
                int co2 = DestinyAtmosphere.co2Percent(player.serverLevel());
                if (co2 >= 60)
                    player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, false, false, true));
                if (co2 >= 80) {
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 0, false, false, true));
                    player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0, false, false, true));
                }
                if (co2 >= 95) player.hurt(player.damageSources().drown(), 2.0F);
            }
            return;
        }
        Long safetyUntil = ENTRY_SAFETY_UNTIL.get(player.getUUID());
        if (!destiny && safetyUntil != null) {
            if (player.serverLevel().getGameTime() > safetyUntil) {
                ENTRY_SAFETY_UNTIL.remove(player.getUUID());
            } else if (player.isInWall()) {
                moveToSafeSurface(player);
            }
        }
        if (player.isCreative() || player.isSpectator()) return;

        ItemStack tank = findUsableTank(player);
        if (wearingCompleteSuit(player) && !tank.isEmpty()) {
            if (destiny) DESTINY_EXPOSURE.remove(player.getUUID());
            player.setTicksFrozen(0);
            if (player.tickCount % 10 == 0) tank.setDamageValue(tank.getDamageValue() + 1);
            return;
        }
        if (wearingAdAstraSuit(player) && adAstraReportsOxygen(player)) {
            if (destiny) DESTINY_EXPOSURE.remove(player.getUUID());
            player.setTicksFrozen(0);
            return;
        }

        // The gloves are built into the chestplate. Any missing suit piece breaks
        // the seal: oxygen escapes from a carried tank without protecting the wearer.
        if (!tank.isEmpty() && wearingAnySuitPiece(player) && player.tickCount % 2 == 0)
            tank.setDamageValue(Math.min(tank.getMaxDamage(), tank.getDamageValue() + 1));

        if (destiny) {
            int exposure = DESTINY_EXPOSURE.merge(player.getUUID(), 1, Integer::sum);
            if (exposure >= 100 && exposure % 40 == 0)
                player.hurt(player.damageSources().drown(), 2.0F);
            return;
        }

        // Give the player a few seconds to react after stepping into a bad atmosphere.
        if (player.tickCount % 40 == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0, false, false, true));
            player.setAirSupply(Math.max(0, player.getAirSupply() - 80));
        }
    }

    private static boolean wearingCompleteSuit(ServerPlayer player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.DESTINY_ENVIRONMENTAL_HELMET.get())
                && player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.DESTINY_ENVIRONMENTAL_CHESTPLATE.get())
                && player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.DESTINY_ENVIRONMENTAL_LEGGINGS.get())
                && player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.DESTINY_ENVIRONMENTAL_BOOTS.get());
    }

    private static boolean wearingAnySuitPiece(ServerPlayer player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.DESTINY_ENVIRONMENTAL_HELMET.get())
                || player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.DESTINY_ENVIRONMENTAL_CHESTPLATE.get())
                || player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.DESTINY_ENVIRONMENTAL_LEGGINGS.get())
                || player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.DESTINY_ENVIRONMENTAL_BOOTS.get());
    }

    private static ItemStack findUsableTank(ServerPlayer player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModItems.DESTINY_OXYGEN_TANK.get()) && stack.getDamageValue() < stack.getMaxDamage()) return stack;
        }
        return ItemStack.EMPTY;
    }

    private static boolean wearingAdAstraSuit(ServerPlayer player) {
        return adAstraItem(player.getItemBySlot(EquipmentSlot.HEAD), "space_helmet")
                && adAstraItem(player.getItemBySlot(EquipmentSlot.CHEST), "space_suit")
                && adAstraItem(player.getItemBySlot(EquipmentSlot.LEGS), "space_pants")
                && adAstraItem(player.getItemBySlot(EquipmentSlot.FEET), "space_boots");
    }

    private static boolean adAstraItem(ItemStack stack, String path) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id != null && id.getNamespace().equals("ad_astra") && id.getPath().equals(path);
    }

    private static boolean adAstraReportsOxygen(ServerPlayer player) {
        try {
            Class<?> api = Class.forName("earth.terrarium.adastra.api.systems.OxygenApi");
            Object instance = api.getField("API").get(null);
            return (boolean) api.getMethod("hasOxygen", net.minecraft.world.entity.LivingEntity.class)
                    .invoke(instance, player);
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }
}
