package com.dankscripts.sgjdestiny_dhd.compat;

import com.dankscripts.sgjdestiny_dhd.item.KinoRemoteItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;
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
import net.povstalec.sgjourney.common.data.StargateNetwork;
import net.povstalec.sgjourney.common.sgjourney.StargateConnection;
import net.povstalec.sgjourney.common.sgjourney.stargate.Stargate;

@Mod.EventBusSubscriber(modid = "sgjdestiny_dhd", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class KinoEvents {
    private static final String GATE_COOLDOWN = "sgjdestiny_kino_gate_cooldown";
    private static final Set<UUID> HELD_CONNECTIONS = new HashSet<>();
    private static final Map<UUID, Set<AbstractStargateEntity<?>>> CONNECTION_GATES = new HashMap<>();
    private KinoEvents() {}

    @SubscribeEvent
    public static void floatWithOwner(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide() || event.level.getGameTime() % 2 != 0) return;
        for (net.minecraft.world.entity.Entity entity : ((net.minecraft.server.level.ServerLevel) event.level).getAllEntities()) {
            if (!(entity instanceof ArmorStand kino) || !kino.getTags().contains("sgjdestiny_kino")) continue;
            int cooldown = kino.getPersistentData().getInt(GATE_COOLDOWN);
            if (cooldown > 0) kino.getPersistentData().putInt(GATE_COOLDOWN, cooldown - 2);
            String now = event.level.dimension().location().toString();
            String before = kino.getPersistentData().getString(KinoRemoteItem.lastDimensionKey());
            AbstractStargateEntity<?> gate = nearbyConnectedGate(kino);
            if (gate != null && gate.getConnectionID() != null) {
                CONNECTION_GATES.computeIfAbsent(gate.getConnectionID(), id -> new HashSet<>()).add(gate);
                if (!before.isEmpty() && !before.equals(now)) HELD_CONNECTIONS.add(gate.getConnectionID());
            }
            kino.getPersistentData().putString(KinoRemoteItem.lastDimensionKey(), now);
            if (cooldown <= 0 && gate != null && crossGate(kino, gate)) continue;
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
        if (event.getEntity() instanceof ServerPlayer player) {
            ArmorStand kino = KinoRemoteItem.getDeployedKino(player);
            if (kino != null && kino.getPersistentData().getBoolean(KinoRemoteItem.followKey())
                    && kino.level() != player.level()) {
                Vec3 look = player.getLookAngle();
                transferKino(kino, player.serverLevel(), new Vec3(player.getX() - look.x * 1.8,
                        player.getEyeY() - 1.45, player.getZ() - look.z * 1.8), player.getYRot());
            }
        }
        AbstractStargateEntity<?> gate = nearbyConnectedGate(event.getEntity());
        if (gate != null && gate.getConnectionID() != null) HELD_CONNECTIONS.remove(gate.getConnectionID());
    }

    private static boolean crossGate(ArmorStand kino, AbstractStargateEntity<?> gate) {
        Vec3 center = gate.getCenter();
        Vec3 forward = Vec3.atLowerCornerOf(gate.getDirection().getNormal()).normalize();
        Vec3 delta = kino.position().add(0, 1.45, 0).subtract(center);
        double plane = delta.dot(forward);
        double radial = delta.subtract(forward.scale(plane)).length();
        if (Math.abs(plane) > 1.15 || radial > Math.max(2.1, gate.getStargate().getInnerRadius())) return false;

        StargateNetwork network = StargateNetwork.get(kino.level());
        StargateConnection connection = network.getConnection(gate.getConnectionID());
        if (connection == null) return false;
        Stargate source = gate.getStargate();
        Stargate destination = sameGate(source, connection.getDialingStargate())
                ? connection.getDialedStargate() : connection.getDialingStargate();
        if (destination == null || destination.getLevel() == null) return false;
        ServerLevel targetLevel = destination.getLevel();
        Vec3 targetForward = destination.getForward().normalize();
        Vec3 target = destination.getPosition().add(targetForward.scale(2.4)).subtract(0, 1.45, 0);
        ArmorStand moved = transferKino(kino, targetLevel, target,
                (float) Math.toDegrees(Math.atan2(-targetForward.x, targetForward.z)));
        moved.getPersistentData().putInt(GATE_COOLDOWN, 40);
        HELD_CONNECTIONS.add(gate.getConnectionID());
        connection.setTimeSinceLastTraveler(0);
        return true;
    }

    private static boolean sameGate(Stargate a, Stargate b) {
        return a != null && b != null && a.getDimension().equals(b.getDimension())
                && a.getPosition().distanceToSqr(b.getPosition()) < 1.0;
    }

    private static ArmorStand transferKino(ArmorStand old, ServerLevel destination, Vec3 position, float cameraYaw) {
        ArmorStand replacement = new ArmorStand(destination, position.x, position.y, position.z);
        replacement.setInvisible(true);
        replacement.setInvulnerable(true);
        replacement.setNoGravity(true);
        replacement.setItemSlot(EquipmentSlot.HEAD, old.getItemBySlot(EquipmentSlot.HEAD).copy());
        replacement.getPersistentData().merge(old.getPersistentData().copy());
        replacement.getPersistentData().putString(KinoRemoteItem.lastDimensionKey(), destination.dimension().location().toString());
        replacement.addTag("sgjdestiny_kino");
        KinoRemoteItem.setCameraYaw(replacement, cameraYaw);
        destination.addFreshEntity(replacement);
        old.discard();
        return replacement;
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
