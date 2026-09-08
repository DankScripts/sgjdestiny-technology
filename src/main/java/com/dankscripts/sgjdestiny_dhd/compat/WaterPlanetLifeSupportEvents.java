package com.dankscripts.sgjdestiny_dhd.compat;

import com.dankscripts.sgjdestiny_dhd.registry.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/** Thin, poisonous atmosphere and Ancient suit life support for Water Planet. */
@Mod.EventBusSubscriber(modid = "sgjdestiny_dhd", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WaterPlanetLifeSupportEvents {
    private static final ResourceLocation WATER_PLANET = new ResourceLocation("sgjourney", "water_planet");
    private static final java.util.Map<java.util.UUID, Long> ENTRY_SAFETY_UNTIL = new java.util.HashMap<>();
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
    public static void protectExplorers(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        if (!player.level().dimension().location().equals(WATER_PLANET)) return;
        Long safetyUntil = ENTRY_SAFETY_UNTIL.get(player.getUUID());
        if (safetyUntil != null) {
            if (player.serverLevel().getGameTime() > safetyUntil) {
                ENTRY_SAFETY_UNTIL.remove(player.getUUID());
            } else if (player.isInWall()) {
                moveToSafeSurface(player);
            }
        }
        if (player.isCreative() || player.isSpectator()) return;

        ItemStack tank = findUsableTank(player);
        if (wearingCompleteSuit(player) && !tank.isEmpty()) {
            player.setTicksFrozen(0);
            if (player.tickCount % 10 == 0) tank.setDamageValue(tank.getDamageValue() + 1);
            return;
        }
        if (wearingAdAstraSuit(player) && adAstraReportsOxygen(player)) {
            player.setTicksFrozen(0);
            return;
        }

        // Give the player a few seconds to react after stepping through the gate.
        if (player.tickCount % 40 == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0, false, false, true));
            player.setAirSupply(Math.max(-20, player.getAirSupply() - 80));
        }
    }

    private static boolean wearingCompleteSuit(ServerPlayer player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.DESTINY_ENVIRONMENTAL_HELMET.get())
                && player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.DESTINY_ENVIRONMENTAL_CHESTPLATE.get())
                && player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.DESTINY_ENVIRONMENTAL_LEGGINGS.get())
                && player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.DESTINY_ENVIRONMENTAL_BOOTS.get());
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
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
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
