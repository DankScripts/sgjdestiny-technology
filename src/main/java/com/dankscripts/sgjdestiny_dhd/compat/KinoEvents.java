package com.dankscripts.sgjdestiny_dhd.compat;

import com.dankscripts.sgjdestiny_dhd.item.KinoRemoteItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.povstalec.sgjourney.common.block_entities.stargate.AbstractStargateEntity;

@Mod.EventBusSubscriber(modid = "sgjdestiny_dhd", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class KinoEvents {
    private static final Set<UUID> HELD_CONNECTIONS = new HashSet<>();
    private static final Map<UUID, Set<AbstractStargateEntity<?>>> CONNECTION_GATES = new HashMap<>();
    private KinoEvents() {}

    @SubscribeEvent
    public static void floatWithOwner(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide() || event.level.getGameTime() % 2 != 0) return;
        for (net.minecraft.world.entity.Entity entity : ((net.minecraft.server.level.ServerLevel) event.level).getAllEntities()) {
            if (!(entity instanceof ArmorStand kino) || !kino.getTags().contains("sgjdestiny_kino")) continue;
            String now = event.level.dimension().location().toString();
            String before = kino.getPersistentData().getString(KinoRemoteItem.lastDimensionKey());
            AbstractStargateEntity<?> gate = nearbyConnectedGate(kino);
            if (gate != null && gate.getConnectionID() != null) {
                CONNECTION_GATES.computeIfAbsent(gate.getConnectionID(), id -> new HashSet<>()).add(gate);
                if (!before.isEmpty() && !before.equals(now)) HELD_CONNECTIONS.add(gate.getConnectionID());
            }
            kino.getPersistentData().putString(KinoRemoteItem.lastDimensionKey(), now);
        }
        for (UUID connection : Set.copyOf(HELD_CONNECTIONS)) {
            Set<AbstractStargateEntity<?>> gates = CONNECTION_GATES.get(connection);
            if (gates == null || gates.stream().noneMatch(AbstractStargateEntity::isConnected)) {
                HELD_CONNECTIONS.remove(connection);
                CONNECTION_GATES.remove(connection);
                continue;
            }
            gates.removeIf(gate -> !gate.isConnected());
            gates.forEach(gate -> gate.setTimeSinceLastTraveler(0));
        }
        for (net.minecraft.world.entity.player.Player player : event.level.players()) {
            if (!(player instanceof ServerPlayer owner)) continue;
            for (ArmorStand kino : event.level.getEntitiesOfClass(ArmorStand.class, owner.getBoundingBox().inflate(112),
                    e -> e.getTags().contains("sgjdestiny_kino") && owner.getUUID().toString().equals(
                            e.getPersistentData().getString(KinoRemoteItem.ownerKey())))) {
                if (!kino.getPersistentData().getBoolean(KinoRemoteItem.followKey())) continue;
                double tx = owner.getX() - owner.getLookAngle().x * 1.8;
                // Head equipment renders above the carrier position; compensate
                // so the visible Kino floats beside the owner's shoulder.
                double ty = owner.getEyeY() - 1.45;
                double tz = owner.getZ() - owner.getLookAngle().z * 1.8;
                kino.setPos(kino.getX() + (tx - kino.getX()) * .16,
                        kino.getY() + (ty - kino.getY()) * .16,
                        kino.getZ() + (tz - kino.getZ()) * .16);
                KinoRemoteItem.setCameraYaw(kino, owner.getYRot());
            }
        }
    }

    @SubscribeEvent
    public static void releaseAfterPlayerTransit(PlayerEvent.PlayerChangedDimensionEvent event) {
        AbstractStargateEntity<?> gate = nearbyConnectedGate(event.getEntity());
        if (gate != null && gate.getConnectionID() != null) HELD_CONNECTIONS.remove(gate.getConnectionID());
    }

    private static AbstractStargateEntity<?> nearbyConnectedGate(net.minecraft.world.entity.Entity entity) {
        return BlockPos.betweenClosedStream(entity.blockPosition().offset(-10, -10, -10),
                        entity.blockPosition().offset(10, 10, 10))
                .map(entity.level()::getBlockEntity)
                .filter(AbstractStargateEntity.class::isInstance)
                .map(blockEntity -> (AbstractStargateEntity<?>) blockEntity)
                .filter(AbstractStargateEntity::isConnected)
                .findFirst().orElse(null);
    }
}
