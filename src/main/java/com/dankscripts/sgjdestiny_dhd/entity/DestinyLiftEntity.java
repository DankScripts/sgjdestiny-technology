package com.dankscripts.sgjdestiny_dhd.entity;

import com.dankscripts.sgjdestiny_dhd.elevator.DestinyLiftEvents;
import com.dankscripts.sgjdestiny_dhd.elevator.DestinyLiftStops;
import com.dankscripts.sgjdestiny_dhd.elevator.LiftMotion;
import com.dankscripts.sgjdestiny_dhd.compat.DestinyShipInstallEvents;
import com.dankscripts.sgjdestiny_dhd.network.DestinyLiftNetwork;
import com.dankscripts.sgjdestiny_dhd.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkHooks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.UUID;

/** Persistent cab with attached riders during travel and free movement at rest. */
public final class DestinyLiftEntity extends Entity {
    private static final EntityDataAccessor<Float> FROM = SynchedEntityData.defineId(DestinyLiftEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> TO = SynchedEntityData.defineId(DestinyLiftEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> START = SynchedEntityData.defineId(DestinyLiftEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> DURATION = SynchedEntityData.defineId(DestinyLiftEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DOCK = SynchedEntityData.defineId(DestinyLiftEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ELAPSED = SynchedEntityData.defineId(DestinyLiftEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> STOPPED_TICKS = SynchedEntityData.defineId(DestinyLiftEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> EMERGENCY = SynchedEntityData.defineId(DestinyLiftEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> HELD_FEET = SynchedEntityData.defineId(DestinyLiftEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> OPEN_TICKS = SynchedEntityData.defineId(DestinyLiftEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> CLOSE_FROM_OPEN = SynchedEntityData.defineId(DestinyLiftEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> BASE_X = SynchedEntityData.defineId(DestinyLiftEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> BASE_Z = SynchedEntityData.defineId(DestinyLiftEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ROTATION = SynchedEntityData.defineId(DestinyLiftEntity.class, EntityDataSerializers.INT);
    private static final TicketType<UUID> TICKET = TicketType.create("destiny_lift", Comparator.comparing(UUID::toString), 40);
    private final LinkedHashSet<Integer> calls = new LinkedHashSet<>();
    private long waitUntil;
    private boolean floorRemoved;
    private boolean releasing;
    private boolean emergencyRequested;

    public DestinyLiftEntity(EntityType<? extends DestinyLiftEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    @Override protected void defineSynchedData() {
        entityData.define(FROM, 70.0F);
        entityData.define(TO, 70);
        entityData.define(START, 0L);
        entityData.define(DURATION, 0);
        entityData.define(DOCK, 70);
        entityData.define(ELAPSED, -10);
        entityData.define(STOPPED_TICKS, LiftMotion.DOCKING_TICKS);
        entityData.define(EMERGENCY, false);
        entityData.define(HELD_FEET, 70.0F);
        entityData.define(OPEN_TICKS, 0);
        entityData.define(CLOSE_FROM_OPEN, false);
        entityData.define(BASE_X, -3);
        entityData.define(BASE_Z, -386);
        entityData.define(ROTATION, 0);
    }

    public void initialize(int floor) {
        ServerLevel server = (ServerLevel) level();
        double[] base = DestinyShipInstallEvents.shipXZ(server, -3, -386);
        entityData.set(BASE_X, (int) base[0]);
        entityData.set(BASE_Z, (int) base[1]);
        entityData.set(ROTATION, DestinyShipInstallEvents.shipQuarterTurns(server));
        entityData.set(DOCK, floor);
        entityData.set(TO, floor);
        entityData.set(FROM, (float) floor);
        setPos(base[0], floor - 1, base[1]);
        waitUntil = level().getGameTime() + 100;
    }

    public boolean travelling() { return entityData.get(DURATION) > 0; }
    public int dock() { return entityData.get(DOCK); }
    public boolean canRelease() { return true; }
    public boolean emergency() { return entityData.get(EMERGENCY); }
    public boolean landingDoorsOpen() { return doorsReady() && entityData.get(OPEN_TICKS) > 0; }
    public int rotation() { return entityData.get(ROTATION); }
    private double[] point(double x, double z) {
        double dx = x + 3, dz = z + 386;
        return switch (rotation()) {
            case 1 -> new double[] {entityData.get(BASE_X) - dz, entityData.get(BASE_Z) + dx};
            case 2 -> new double[] {entityData.get(BASE_X) - dx, entityData.get(BASE_Z) - dz};
            case 3 -> new double[] {entityData.get(BASE_X) + dz, entityData.get(BASE_Z) - dx};
            default -> new double[] {entityData.get(BASE_X) + dx, entityData.get(BASE_Z) + dz};
        };
    }
    private double[] unpoint(double x, double z) {
        double dx = x - entityData.get(BASE_X), dz = z - entityData.get(BASE_Z);
        return switch (rotation()) {
            case 1 -> new double[] {-3 + dz, -386 - dx};
            case 2 -> new double[] {-3 - dx, -386 - dz};
            case 3 -> new double[] {-3 - dz, -386 + dx};
            default -> new double[] {-3 + dx, -386 + dz};
        };
    }
    private AABB box(double x0, double y0, double z0, double x1, double y1, double z1) {
        double[] a = point(x0, z0), b = point(x1, z1);
        return new AABB(Math.min(a[0], b[0]), y0, Math.min(a[1], b[1]),
                Math.max(a[0], b[0]), y1, Math.max(a[1], b[1]));
    }
    private BlockPos block(int x, int y, int z) {
        return DestinyShipInstallEvents.shipPos((ServerLevel) level(), new BlockPos(x, y, z));
    }
    private boolean horizontal(Entity e, double x0, double x1, double z0, double z1) {
        double[] p = unpoint(e.getX(), e.getZ());
        return p[0] > x0 && p[0] < x1 && p[1] > z0 && p[1] < z1;
    }
    private void setFloorPos(double feet) { setPos(entityData.get(BASE_X), feet - 1, entityData.get(BASE_Z)); }

    private void openForBoarding() {
        entityData.set(OPEN_TICKS, LiftMotion.BOARDING_TICKS);
        if (!level().isClientSide) DestinyLiftEvents.setLandingDoors((ServerLevel) level(), dock());
    }

    private boolean doorwayOccupied() {
        double feet = getY() + 1;
        return !level().getEntitiesOfClass(LivingEntity.class,
                box(-5, feet, -387, -3.95, feet + 2.7, -385),
                e -> e.isAlive() && !e.isSpectator()).isEmpty();
    }

    public boolean doorsReady() {
        return !emergency() && LiftMotion.canOpenDoors(travelling(), entityData.get(STOPPED_TICKS), getY() + 1, dock(),
                level().isClientSide || dockSupported());
    }

    private boolean dockSupported() {
        for (int x = -4; x <= -3; x++) for (int z = -387; z <= -386; z++)
            if (!isDockFloor(level().getBlockState(block(x, dock() - 1, z)))) return false;
        return true;
    }

    public boolean aboard(Entity entity) {
        return entity.level() == level() && horizontal(entity, -4, -2, -387, -385)
                && Math.abs(entity.getY() - (getY() + 1)) < 0.4;
    }

    public float doorOpen(float partialTick) {
        if (emergency()) return 1;
        if (!travelling()) return landingDoorsOpen() ? 1.0F : 0.0F;
        return entityData.get(CLOSE_FROM_OPEN) ? LiftMotion.doorOpen(entityData.get(ELAPSED) + partialTick) : 0;
    }

    public void call(int floor, ServerPlayer player) {
        if (emergency()) { tell(player, "Emergency release active. Select a destination inside the cab to resume."); return; }
        if (DestinyLiftStops.index(floor) < 0) return;
        if (!travelling() && floor == dock()) {
            openForBoarding();
            tell(player, doorsReady() ? "Lift ready. Step inside and right-click its control panel." : "Lift docking. Please wait for the doors.");
            return;
        }
        if (!travelling() || floor != entityData.get(TO)) calls.add(floor);
        tell(player, "Lift called to " + DestinyLiftStops.names()[DestinyLiftStops.index(floor)] + ".");
    }

    public void select(int floor, ServerPlayer player) {
        if (DestinyLiftStops.index(floor) < 0 || !aboard(player) || (!doorsReady() && !emergency()) || player.isSpectator()) return;
        if (floor != dock() || emergency()) depart(floor, player);
        else openForBoarding();
    }

    private boolean depart(int floor, ServerPlayer requester) {
        ServerLevel server = (ServerLevel) level();
        double from = getY() + 1;
        if (!clearRoute(from, floor, true)) {
            tell(requester, "Lift blocked: clear the shaft before travelling.");
            return false;
        }
        var doorway = box(-5, from, -387, -3.95, from + 2.7, -385);
        if (!server.getEntitiesOfClass(LivingEntity.class, doorway, e -> e.isAlive() && !e.isSpectator()).isEmpty()) {
            tell(requester, "Please stand fully inside or outside the lift doorway.");
            return false;
        }
        var riders = server.getEntitiesOfClass(LivingEntity.class,
                box(-4, from - 0.1, -387, -2, from + 2.6, -385), e -> e.isAlive() && !e.isSpectator());
        for (var rider : riders) {
            if (!aboard(rider) || rider.isPassenger() || rider.isVehicle()) {
                tell(requester, "All riders must stand on the lift floor before departure.");
                return false;
            }
        }
        entityData.set(CLOSE_FROM_OPEN, emergency() || landingDoorsOpen());
        entityData.set(OPEN_TICKS, 0);
        entityData.set(EMERGENCY, false);
        DestinyLiftEvents.clearEmergency((ServerLevel) level());
        entityData.set(FROM, (float) from);
        entityData.set(TO, floor);
        entityData.set(START, level().getGameTime() + 10);
        entityData.set(DURATION, LiftMotion.duration(from, floor));
        entityData.set(ELAPSED, -10);
        entityData.set(STOPPED_TICKS, 0);
        // Vehicle positioning avoids competing player gravity/floor collision updates
        // on upward trips. Riders are released on arrival or emergency stop.
        for (LivingEntity rider : riders) rider.startRiding(this, true);
        floorRemoved = false;
        calls.remove(floor);
        DestinyLiftEvents.rememberStop(server, floor);
        DestinyLiftEvents.setLandingDoors(server, -1);
        keepLoaded(server);
        return true;
    }

    private boolean clearRoute(double from, double to, boolean ignoreDockFloor) {
        int min = (int) Math.floor(Math.min(from, to) - 1);
        int max = (int) Math.ceil(Math.max(from, to) + 2.7) - 1;
        for (int x = -4; x <= -3; x++) for (int z = -387; z <= -386; z++) {
            for (int y = min; y <= max; y++) {
                BlockPos pos = block(x, y, z);
                var state = level().getBlockState(pos);
                if (ignoreDockFloor && y == dock() - 1 && isDockFloor(state)) continue;
                if (!state.isAir()) return false;
            }
        }
        return true;
    }

    private void keepLoaded(ServerLevel server) {
        server.getChunkSource().addRegionTicket(TICKET, new ChunkPos(blockPosition()), 2, getUUID());
    }

    @Override public void tick() {
        super.tick();
        if (!level().isClientSide && emergencyRequested) {
            emergencyRequested = false;
            emergencyStop(null);
        }
        if (!level().isClientSide && !DestinyLiftEvents.registered(this)) {
            for (Entity rider : new ArrayList<>(getPassengers()))
                if (rider instanceof ServerPlayer player) releaseOnLogout(player);
            releasing = true;
            ejectPassengers();
            discard();
            return;
        }
        if (!level().isClientSide && tickCount == 1) DestinyLiftEvents.upgradeLandingDoors((ServerLevel) level());
        if (!level().isClientSide && !travelling() && !getPassengers().isEmpty()) releaseDockedPassengers();
        if (emergency()) {
            setFloorPos(entityData.get(HELD_FEET));
            if (!level().isClientSide) {
                if (tickCount % 20 == 0) keepLoaded((ServerLevel) level());
                if (tickCount % 10 == 0) DestinyLiftEvents.setLandingDoors((ServerLevel) level(), -1);
            }
            return;
        }
        if (travelling()) {
            // Client world clocks can differ from this dimension's server clock. Replicate
            // progress instead; the floor, walls, doors and riders share one entity transform.
            if (!level().isClientSide) entityData.set(ELAPSED, LiftMotion.elapsed(
                    level().getGameTime(), entityData.get(START), entityData.get(DURATION)));
            double feet = LiftMotion.height(entityData.get(FROM), entityData.get(TO),
                    entityData.get(ELAPSED), entityData.get(DURATION));
            if (!level().isClientSide) {
                ServerLevel server = (ServerLevel) level();
                if (tickCount % 20 == 0) keepLoaded(server);
                if (tickCount % 10 == 0) DestinyLiftEvents.setLandingDoors(server, -1);
                if (level().getGameTime() >= entityData.get(START) && !floorRemoved) {
                    for (int x = -4; x <= -3; x++) for (int z = -387; z <= -386; z++) {
                        BlockPos p = block(x, dock() - 1, z);
                        if (isDockFloor(level().getBlockState(p))) level().setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                    }
                    floorRemoved = true;
                }
                if (!clearRoute(getY() + 1, feet, !floorRemoved)) {
                    // Hold the current floor for rescue without snapping riders or
                    // creating a new landing at an arbitrary shaft height.
                    emergencyStop(null);
                    for (ServerPlayer player : server.players()) if (player.distanceToSqr(this) < 64)
                        tell(player, "Lift stopped: an obstruction appeared in the shaft.");
                    return;
                }
            }
            moveFloor(feet);
            if (!level().isClientSide && level().getGameTime() >= entityData.get(START) + entityData.get(DURATION)) {
                arrive(entityData.get(TO));
            }
        } else {
            setFloorPos(dock());
            if (!level().isClientSide) {
                // Upgrade dev168's visible dock blocks in place, including a saved cab.
                if (tickCount == 1) restoreDockSupport(dock());
                boolean wasReady = doorsReady();
                int stopped = entityData.get(STOPPED_TICKS);
                entityData.set(STOPPED_TICKS, dockSupported() ? Math.min(LiftMotion.DOCKING_TICKS, stopped + 1) : 0);
                boolean ready = doorsReady();
                int open = entityData.get(OPEN_TICKS);
                if (ready && open > 0) entityData.set(OPEN_TICKS,
                        LiftMotion.nextBoardingTicks(open, doorwayOccupied()));
                if (ready != wasReady || (open > 0 && entityData.get(OPEN_TICKS) == 0) || tickCount % 10 == 0)
                    DestinyLiftEvents.setLandingDoors((ServerLevel) level(), landingDoorsOpen() ? dock() : -1);
                if (ready && !getPassengers().isEmpty()) releaseDockedPassengers();
                if (ready && entityData.get(OPEN_TICKS) == 0 && level().getGameTime() >= waitUntil && !calls.isEmpty()) {
                    int next = calls.iterator().next();
                    if (next == dock()) calls.remove(next);
                    else if (!depart(next, null)) waitUntil = level().getGameTime() + 40;
                }
            }
        }
    }

    private void arrive(int floor) {
        setFloorPos(floor);
        entityData.set(DOCK, floor);
        entityData.set(DURATION, 0);
        entityData.set(STOPPED_TICKS, 0);
        restoreDockSupport(floor);
        releaseDockedPassengers();
        entityData.set(OPEN_TICKS, LiftMotion.BOARDING_TICKS);
        floorRemoved = false;
        // Send the stopped cab state before opening either set of doors.
        calls.remove(floor);
        waitUntil = level().getGameTime() + LiftMotion.DOCKING_TICKS;
        DestinyLiftEvents.rememberStop((ServerLevel) level(), floor);
        DestinyLiftEvents.setLandingDoors((ServerLevel) level(), -1);
        ((ServerLevel) level()).getChunkSource().removeRegionTicket(TICKET, new ChunkPos(blockPosition()), 2, getUUID());
    }

    private void releaseDockedPassengers() {
        releasing = true;
        // Preserve each rider's standing location when the real floor is restored.
        for (Entity rider : new ArrayList<>(getPassengers())) {
            positionRider(rider);
            Vec3 p = rider.position();
            rider.stopRiding();
            if (rider instanceof ServerPlayer player) player.teleportTo(p.x, getY() + 1, p.z);
            else rider.setPos(p.x, getY() + 1, p.z);
            rider.fallDistance = 0;
        }
        releasing = false;
    }

    private void moveFloor(double feet) {
        double previous = getY() + 1, dy = feet - previous;
        var standing = level().getEntitiesOfClass(LivingEntity.class,
                box(-4, previous - .2, -387, -2, previous + .25, -385),
                e -> !e.isSpectator() && !e.isPassenger() && horizontal(e, -4, -2, -387, -385)
                        && Math.abs(e.getY() - previous) < .2
                        && e.getDeltaMovement().y <= .01);
        // Carry only the floor's vertical displacement. Keep horizontal input, jumping,
        // camera control and crouching under the player's normal movement handling.
        if (dy > 0) for (LivingEntity rider : standing) carry(rider, dy);
        setFloorPos(feet);
        if (dy < 0) for (LivingEntity rider : standing) carry(rider, dy);
    }

    private void carry(LivingEntity rider, double dy) {
        if (level().isClientSide && !(rider instanceof Player player && player.isLocalPlayer())) return;
        rider.move(MoverType.PISTON, new Vec3(0, dy, 0));
        rider.fallDistance = 0;
        rider.setOnGround(true);
    }

    public boolean insideCab(Entity entity) {
        return entity.level() == level() && horizontal(entity, -4, -2, -387, -385) && entity.getY() >= getY() + .8
                && entity.getY() < getY() + 3.6;
    }

    public void emergencyStop(ServerPlayer player) {
        if (player != null && player.isSpectator()) return;
        entityData.set(HELD_FEET, (float) (getY() + 1));
        entityData.set(EMERGENCY, true);
        entityData.set(DURATION, 0);
        calls.clear();
        releaseDockedPassengers();
        int landing = -1;
        for (int floor : DestinyLiftStops.HEIGHTS) if (Math.abs(getY() + 1 - floor) < .01) { landing = floor; break; }
        DestinyLiftEvents.releaseEmergencyLanding((ServerLevel) level(), landing);
        DestinyLiftEvents.setLandingDoors((ServerLevel) level(), -1);
        keepLoaded((ServerLevel) level());
        tell(player, "Emergency stop: cab doors released. Select a floor inside to resume when the shaft is clear.");
    }

    private static boolean isDockFloor(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(Blocks.SEA_LANTERN) || state.is(ModBlocks.DESTINY_LIFT_SUPPORT.get());
    }

    private void restoreDockSupport(int floor) {
        for (int x = -4; x <= -3; x++) for (int z = -387; z <= -386; z++) {
            BlockPos p = block(x, floor - 1, z);
            var state = level().getBlockState(p);
            if (state.isAir() || state.is(Blocks.SEA_LANTERN))
                level().setBlock(p, ModBlocks.DESTINY_LIFT_SUPPORT.get().defaultBlockState(), 3);
        }
    }

    public void releaseOnLogout(ServerPlayer player) {
        releasing = true;
        player.stopRiding();
        double[] outside = point(-5.8, -386.5);
        player.teleportTo(outside[0], dock(), outside[1]);
        player.fallDistance = 0;
        releasing = false;
    }

    @Override protected void positionRider(Entity rider, MoveFunction move) {
        if (!hasPassenger(rider)) return;
        double[] captured = unpoint(rider.getX(), rider.getZ());
        double[] safe = point(Math.max(-3.7, Math.min(-2.3, captured[0])),
                Math.max(-386.7, Math.min(-385.3, captured[1])));
        move.accept(rider, safe[0], getY() + 1, safe[1]);
    }
    @Override protected boolean canAddPassenger(Entity passenger) { return travelling() && !emergency(); }
    public boolean releasingPassengers() { return releasing; }
    public void requestEmergencyStop() { emergencyRequested = true; }
    @Override public boolean canBeCollidedWith() { return !isRemoved(); }
    @Override public AABB getBoundingBoxForCulling() { return getBoundingBox().expandTowards(0, 2.65, 0); }
    @Override public boolean shouldRiderSit() { return false; }
    @Override public Vec3 getDismountLocationForPassenger(LivingEntity passenger) { return passenger.position(); }
    @Override public boolean isPickable() { return true; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source) { return true; }
    @Override public InteractionResult interact(Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) {
            if (insideCab(player)) DestinyLiftNetwork.open(serverPlayer, this);
            else tell(serverPlayer, "Step inside the lift to select a destination.");
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }
    @Override public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps, boolean teleport) {
        // Use replicated progress for the entire cab; a second positional lerp would add lag.
    }
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (level().isClientSide && (key.equals(DOCK) || key.equals(DURATION) || key.equals(STOPPED_TICKS)
                || key.equals(EMERGENCY) || key.equals(HELD_FEET)) && !travelling()) {
            // Apply the dock pose in the packet handler, before a later door-block
            // update can render. Do not interpolate a stale travelling frame at rest.
            setFloorPos(emergency() ? entityData.get(HELD_FEET) : dock());
            xo = xOld = getX(); yo = yOld = getY(); zo = zOld = getZ();
        }
    }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }

    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Dock", dock()); tag.putFloat("From", entityData.get(FROM)); tag.putInt("To", entityData.get(TO));
        tag.putLong("Start", entityData.get(START)); tag.putInt("Duration", entityData.get(DURATION));
        tag.putBoolean("FloorRemoved", floorRemoved); tag.putLong("WaitUntil", waitUntil);
        tag.putInt("StoppedTicks", entityData.get(STOPPED_TICKS));
        tag.putInt("OpenTicks", entityData.get(OPEN_TICKS));
        tag.putBoolean("CloseFromOpen", entityData.get(CLOSE_FROM_OPEN));
        tag.putBoolean("Emergency", emergency()); tag.putFloat("HeldFeet", entityData.get(HELD_FEET));
        tag.putInt("BaseX", entityData.get(BASE_X)); tag.putInt("BaseZ", entityData.get(BASE_Z));
        tag.putInt("Rotation", rotation());
        tag.putIntArray("Calls", calls.stream().mapToInt(Integer::intValue).toArray());
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("BaseX")) entityData.set(BASE_X, tag.getInt("BaseX"));
        if (tag.contains("BaseZ")) entityData.set(BASE_Z, tag.getInt("BaseZ"));
        entityData.set(ROTATION, Math.floorMod(tag.getInt("Rotation"), 4));
        entityData.set(DOCK, Math.max(70, Math.min(111, tag.getInt("Dock"))));
        entityData.set(FROM, tag.getFloat("From")); entityData.set(TO, tag.getInt("To"));
        entityData.set(START, tag.getLong("Start")); entityData.set(DURATION, tag.getInt("Duration"));
        entityData.set(ELAPSED, LiftMotion.elapsed(level().getGameTime(), tag.getLong("Start"), tag.getInt("Duration")));
        floorRemoved = tag.getBoolean("FloorRemoved"); waitUntil = tag.getLong("WaitUntil");
        entityData.set(OPEN_TICKS, Math.max(0, Math.min(LiftMotion.BOARDING_TICKS, tag.getInt("OpenTicks"))));
        entityData.set(CLOSE_FROM_OPEN, tag.getBoolean("CloseFromOpen"));
        entityData.set(HELD_FEET, tag.contains("HeldFeet") ? tag.getFloat("HeldFeet") : (float) dock());
        entityData.set(EMERGENCY, tag.getBoolean("Emergency"));
        entityData.set(STOPPED_TICKS, tag.contains("StoppedTicks")
                ? Math.max(0, Math.min(LiftMotion.DOCKING_TICKS, tag.getInt("StoppedTicks")))
                : (travelling() ? 0 : LiftMotion.DOCKING_TICKS));
        calls.clear();
        for (int floor : tag.getIntArray("Calls")) if (DestinyLiftStops.index(floor) >= 0) calls.add(floor);
    }
    private static void tell(ServerPlayer player, String message) {
        if (player != null) player.displayClientMessage(Component.literal(message), true);
    }
}
