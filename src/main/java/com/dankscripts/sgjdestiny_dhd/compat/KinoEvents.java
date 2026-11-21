package com.dankscripts.sgjdestiny_dhd.compat;

import com.dankscripts.sgjdestiny_dhd.item.KinoRemoteItem;
import com.dankscripts.sgjdestiny_dhd.network.DestinyDialerNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.UUID;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.povstalec.sgjourney.common.block_entities.stargate.AbstractStargateEntity;

@EventBusSubscriber(modid = "sgjdestiny_dhd", bus = EventBusSubscriber.Bus.GAME)
public final class KinoEvents {
    private static final String FOLLOW_DX = "sgjdestiny_kino_follow_dx";
    private static final String FOLLOW_DY = "sgjdestiny_kino_follow_dy";
    private static final String FOLLOW_DZ = "sgjdestiny_kino_follow_dz";
    private static final Set<UUID> HELD_CONNECTIONS = new HashSet<>();
    private static final Set<UUID> PLAYER_RELEASED_CONNECTIONS = new HashSet<>();
    private static final java.util.Map<UUID, KinoRoute> ACTIVE_ROUTES = new java.util.HashMap<>();
    private KinoEvents() {}

    @SubscribeEvent
    public static void stopLoadingRemovedKino(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ArmorStand kino)
                || !kino.getTags().contains("sgjdestiny_kino")) return;
        Entity.RemovalReason reason = kino.getRemovalReason();
        if (reason == Entity.RemovalReason.KILLED || reason == Entity.RemovalReason.DISCARDED)
            KinoRemoteItem.releaseKinoChunk(kino);
    }

    @SubscribeEvent
    public static void floatWithOwner(LevelTickEvent.Post event) {
        if (event.getLevel().isClientSide() || event.getLevel().getGameTime() % 2 != 0) return;
        for (net.minecraft.world.entity.Entity entity : ((net.minecraft.server.level.ServerLevel) event.getLevel()).getAllEntities()) {
            if (!(entity instanceof ArmorStand kino) || !kino.getTags().contains("sgjdestiny_kino")) continue;
            // The armor stand is only a visual carrier. The handheld resolves
            // movement with the Kino-sized collision box before moving it.
            kino.noPhysics = true;
            KinoRemoteItem.keepKinoChunkLoaded(kino);
            String now = event.getLevel().dimension().location().toString();
            String before = kino.getPersistentData().getString(KinoRemoteItem.lastDimensionKey());
            AbstractStargateEntity<?> gate = nearbyConnectedGate(kino);
            if (gate != null && gate.getConnectionID() != null) {
                // Claim the live connection before SGJourney processes the Kino as
                // a traveler. Its DHD then reports autoclose disabled until a real
                // player crosses or the connection is manually terminated.
                if (!PLAYER_RELEASED_CONNECTIONS.contains(gate.getConnectionID())) {
                    HELD_CONNECTIONS.add(gate.getConnectionID());
                }
            }

            // SGJourney reconstructs non-player travelers directly on the
            // destination event-horizon plane. Move the reconstructed Kino just
            // beyond that plane once so subsequent handheld movement is not
            // swallowed by another wormhole collision check.
            if (!before.isEmpty() && !before.equals(now) && gate != null && gate.getStargate() != null) {
                KinoRemoteItem.synchronizeAfterTransit(kino);
                net.minecraft.world.phys.Vec3 forward = gate.getStargate().getForward();
                if (forward.lengthSqr() > .0001) {
                    net.minecraft.world.phys.Vec3 exit = forward.normalize().scale(1.35);
                    KinoRemoteItem.moveWithBlockCollision(kino, exit);
                    kino.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                }
                try {
                    ServerPlayer owner = ((net.minecraft.server.level.ServerLevel) event.getLevel()).getServer()
                            .getPlayerList().getPlayer(UUID.fromString(kino.getPersistentData()
                                    .getString(KinoRemoteItem.ownerKey())));
                    DestinyDialerNetwork.refreshKinoVideoAfterTransit(owner, kino);
                } catch (IllegalArgumentException ignored) {}
            }
            kino.getPersistentData().putString(KinoRemoteItem.lastDimensionKey(), now);

            // If the owner has just crossed first, keep the Kino physically flying
            // along its existing path. SGJourney then observes the Kino crossing the
            // event-horizon plane itself and performs its normal wormhole travel.
            if (kino.getPersistentData().getBoolean(KinoRemoteItem.followKey())) {
                try {
                    ServerPlayer owner = ((net.minecraft.server.level.ServerLevel) event.getLevel()).getServer()
                            .getPlayerList().getPlayer(UUID.fromString(kino.getPersistentData()
                                    .getString(KinoRemoteItem.ownerKey())));
                    if (owner != null && owner.level() != kino.level()) {
                        KinoRemoteItem.moveWithBlockCollision(kino, new Vec3(kino.getPersistentData().getDouble(FOLLOW_DX),
                                kino.getPersistentData().getDouble(FOLLOW_DY),
                                kino.getPersistentData().getDouble(FOLLOW_DZ)));
                    } else if (owner != null && !before.isEmpty() && !before.equals(now)
                            && gate != null && gate.getConnectionID() != null) {
                        HELD_CONNECTIONS.remove(gate.getConnectionID());
                    }
                } catch (IllegalArgumentException ignored) {}
            }
        }
        for (net.minecraft.world.entity.player.Player player : event.getLevel().players()) {
            if (!(player instanceof ServerPlayer owner)) continue;
            for (ArmorStand kino : event.getLevel().getEntitiesOfClass(ArmorStand.class, owner.getBoundingBox().inflate(112),
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
                KinoRemoteItem.moveWithBlockCollision(kino, new Vec3(dx, dy, dz));
                KinoRemoteItem.setCameraYaw(kino, owner.getYRot());
            }
        }
    }

    /** SGJourney treats a DHD autoclose duration of zero as a held-open connection. */
    public static boolean shouldHoldOpen(AbstractStargateEntity<?> gate) {
        return gate != null && gate.getConnectionID() != null
                && HELD_CONNECTIONS.contains(gate.getConnectionID());
    }

    @SubscribeEvent
    public static void connectionTerminated(
            net.povstalec.sgjourney.common.events.custom.StargateConnectionEvent.Terminate event) {
        var connection = event.getStargateConnection();
        HELD_CONNECTIONS.remove(connection.getID());
        PLAYER_RELEASED_CONNECTIONS.remove(connection.getID());
        ACTIVE_ROUTES.remove(connection.getID());
        var dialingDimension = connection.getDialingStargate().getDimension();
        Set<net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level>> destinationDimensions =
                connection.getDialedStargates().stream().map(
                        net.povstalec.sgjourney.api.stargate.Stargate::getDimension)
                        .collect(java.util.stream.Collectors.toSet());
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (!player.level().dimension().equals(dialingDimension)) continue;
            boolean suspended = false;
            for (ArmorStand kino : KinoRemoteItem.ownedKinos(player)) {
                if (KinoRemoteItem.isLinked(kino)
                        && destinationDimensions.contains(kino.level().dimension())) {
                    KinoRemoteItem.setLinked(kino, false);
                    suspended = true;
                }
            }
            if (suspended) DestinyDialerNetwork.suspendKinoLink(player);
        }
    }

    @SubscribeEvent
    public static void connectionEstablished(
            net.povstalec.sgjourney.common.events.custom.StargateConnectionEvent.Establish event) {
        var connection = event.getStargateConnection();
        var dialingDimension = connection.getDialingStargate().getDimension();
        Set<net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level>> destinationDimensions =
                connection.getDialedStargates().stream().map(
                        net.povstalec.sgjourney.api.stargate.Stargate::getDimension)
                        .collect(java.util.stream.Collectors.toSet());
        ACTIVE_ROUTES.put(connection.getID(), new KinoRoute(dialingDimension, destinationDimensions));
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (!player.level().dimension().equals(dialingDimension)) continue;
            // A replacement Kino owns the handheld link. Do not reclaim an older
            // abandoned unit when its planet is dialed again.
            if (KinoRemoteItem.hasDeployedKino(player)) continue;
            boolean restored = false;
            for (ArmorStand kino : KinoRemoteItem.ownedKinos(player)) {
                if (!KinoRemoteItem.isLinked(kino)
                        && destinationDimensions.contains(kino.level().dimension())
                        && KinoRemoteItem.canRestoreBinding(player, kino)) {
                    KinoRemoteItem.setLinked(kino, true);
                    restored = true;
                }
            }
            if (restored) DestinyDialerNetwork.restoreKinoLink(player);
        }
    }

    public static boolean hasOpenRoute(ServerPlayer player, ArmorStand kino) {
        if (player.level().dimension().equals(kino.level().dimension())) return true;
        for (KinoRoute route : ACTIVE_ROUTES.values()) {
            if (route.dialingDimension().equals(player.level().dimension())
                    && route.destinationDimensions().contains(kino.level().dimension())) return true;
        }
        return false;
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
                releaseForPlayer(sourceGate);
            } else if (gate != null && gate.getConnectionID() != null) {
                releaseForPlayer(gate);
            }
        }
    }

    private static void releaseForPlayer(AbstractStargateEntity<?> gate) {
        UUID connectionID = gate.getConnectionID();
        if (connectionID == null) return;
        PLAYER_RELEASED_CONNECTIONS.add(connectionID);
        HELD_CONNECTIONS.remove(connectionID);
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

    private record KinoRoute(
            net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dialingDimension,
            Set<net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level>> destinationDimensions) {}
}
