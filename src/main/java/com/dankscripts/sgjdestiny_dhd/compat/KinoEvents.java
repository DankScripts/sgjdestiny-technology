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
    private static final String FOLLOW_DX = "sgjdestiny_kino_follow_dx";
    private static final String FOLLOW_DY = "sgjdestiny_kino_follow_dy";
    private static final String FOLLOW_DZ = "sgjdestiny_kino_follow_dz";
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

            // If the owner has just crossed first, keep the Kino physically flying
            // along its existing path. SGJourney then observes the Kino crossing the
            // event-horizon plane itself and performs its normal wormhole travel.
            if (kino.getPersistentData().getBoolean(KinoRemoteItem.followKey())) {
                try {
                    ServerPlayer owner = ((net.minecraft.server.level.ServerLevel) event.level).getServer()
                            .getPlayerList().getPlayer(UUID.fromString(kino.getPersistentData()
                                    .getString(KinoRemoteItem.ownerKey())));
                    if (owner != null && owner.level() != kino.level()) {
                        kino.setPos(kino.getX() + kino.getPersistentData().getDouble(FOLLOW_DX),
                                kino.getY() + kino.getPersistentData().getDouble(FOLLOW_DY),
                                kino.getZ() + kino.getPersistentData().getDouble(FOLLOW_DZ));
                    } else if (owner != null && !before.isEmpty() && !before.equals(now)
                            && gate != null && gate.getConnectionID() != null) {
                        HELD_CONNECTIONS.remove(gate.getConnectionID());
                    }
                } catch (IllegalArgumentException ignored) {}
            }
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
                double dx = (tx - kino.getX()) * .16;
                double dy = (ty - kino.getY()) * .16;
                double dz = (tz - kino.getZ()) * .16;
                kino.getPersistentData().putDouble(FOLLOW_DX, dx);
                kino.getPersistentData().putDouble(FOLLOW_DY, dy);
                kino.getPersistentData().putDouble(FOLLOW_DZ, dz);
                kino.setPos(kino.getX() + dx, kino.getY() + dy, kino.getZ() + dz);
                KinoRemoteItem.setCameraYaw(kino, owner.getYRot());
            }
        }
    }

    @SubscribeEvent
    public static void releaseAfterPlayerTransit(PlayerEvent.PlayerChangedDimensionEvent event) {
        AbstractStargateEntity<?> gate = nearbyConnectedGate(event.getEntity());
        if (event.getEntity() instanceof ServerPlayer player) {
            ArmorStand kino = KinoRemoteItem.getDeployedKino(player);
            AbstractStargateEntity<?> sourceGate = kino == null ? null : nearbyConnectedGate(kino);
            if (kino != null && sourceGate != null && sourceGate.getConnectionID() != null
                    && kino.getPersistentData().getBoolean(KinoRemoteItem.followKey())
                    && kino.level() != player.level()) {
                // Aim at the physical center of whichever source gate was used. This
                // avoids assuming that Destiny and planetary gates face the same way.
                net.minecraft.world.phys.Vec3 visibleKino = kino.position().add(0, 1.45, 0);
                net.minecraft.world.phys.Vec3 towardGate = sourceGate.getCenter().subtract(visibleKino);
                if (towardGate.lengthSqr() > .0001) {
                    net.minecraft.world.phys.Vec3 step = towardGate.normalize().scale(.34);
                    kino.getPersistentData().putDouble(FOLLOW_DX, step.x);
                    kino.getPersistentData().putDouble(FOLLOW_DY, step.y);
                    kino.getPersistentData().putDouble(FOLLOW_DZ, step.z);
                }
                HELD_CONNECTIONS.add(sourceGate.getConnectionID());
            } else if (gate != null && gate.getConnectionID() != null) {
                HELD_CONNECTIONS.remove(gate.getConnectionID());
            }
        }
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
