package com.dankscripts.sgjdestiny_dhd.elevator;

import com.dankscripts.sgjdestiny_dhd.DestinyDHD;
import com.dankscripts.sgjdestiny_dhd.entity.DestinyLiftEntity;
import com.dankscripts.sgjdestiny_dhd.network.DestinyLiftNetwork;
import com.dankscripts.sgjdestiny_dhd.registry.ModEntities;
import com.dankscripts.sgjdestiny_dhd.registry.ModBlocks;
import com.dankscripts.sgjdestiny_dhd.block.DestinyLiftAnchorBlock;
import com.dankscripts.sgjdestiny_dhd.block.DestinyLiftDoorBlock;
import com.dankscripts.sgjdestiny_dhd.registry.ModItems;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Retrofit only the recognized, parked legacy elevator; never scan/replace arbitrary ship blocks. */
@Mod.EventBusSubscriber(modid = DestinyDHD.MOD_ID)
public final class DestinyLiftEvents {
    public static final BlockPos ANCHOR = new BlockPos(-4, 65, -387);
    private DestinyLiftEvents() {}
    public static boolean isDestiny(Level level) { return level.dimension().location().toString().equals("sgjourney:destiny"); }

    public static DestinyLiftEntity find(ServerLevel level) {
        UUID id = Data.get(level).cab;
        return id != null && level.getEntity(id) instanceof DestinyLiftEntity cab ? cab : null;
    }
    public static boolean registered(DestinyLiftEntity cab) {
        return cab.getUUID().equals(Data.get((ServerLevel) cab.level()).cab);
    }

    /** Returns a diagnostic rather than changing an unrecognized/altered moving mechanism. */
    public static String install(ServerLevel level) {
        if (!isDestiny(level)) return "This upgrade is for Destiny's existing elevator.";
        level.getChunk(-1, -25);
        if (Data.get(level).cab != null) return find(level) == null ? "Elevator is loading; try again in a moment." : "Elevator already upgraded.";
        boolean capturedCab = level.getBlockState(ANCHOR).is(ModBlocks.DESTINY_LIFT_ANCHOR.get());
        int capturedFloor = capturedCab ? DestinyLiftStops.HEIGHTS[level.getBlockState(ANCHOR).getValue(DestinyLiftAnchorBlock.STOP)] : -1;
        for (int floor : DestinyLiftStops.HEIGHTS) {
            Map<BlockPos, BlockState> motor = capturedCab ? new LinkedHashMap<>() : legacyMotor(level, floor);
            if (capturedCab && floor != capturedFloor) continue;
            if (motor == null) continue;
            if (!capturedCab && !level.getBlockState(ANCHOR).is(Blocks.OBSIDIAN)) return "The original elevator pit is altered; upgrade canceled.";
            for (int x = -4; x <= -3; x++) for (int z = -387; z <= -386; z++) for (int y = 69; y <= 113; y++) {
                BlockPos p = new BlockPos(x, y, z);
                // Captures can contain a docked floor, or no floor if saved during travel.
                if (capturedCab && DestinyLiftStops.index(y + 1) >= 0
                        && (level.getBlockState(p).is(Blocks.SEA_LANTERN) || level.getBlockState(p).is(ModBlocks.DESTINY_LIFT_SUPPORT.get())))
                    motor.put(p, level.getBlockState(p));
                if (!motor.containsKey(p) && !level.getBlockState(p).isAir()) return "Clear the elevator shaft before upgrading it.";
            }
            for (int landing : DestinyLiftStops.HEIGHTS) {
                BlockPos button = new BlockPos(-6, landing + 1, -388);
                if (!level.getBlockState(button).isAir() && !(capturedCab && level.getBlockState(button).is(Blocks.STONE_BUTTON)))
                    return "The outside call-button position is occupied at " + button.toShortString();
                for (int z = -387; z <= -386; z++) for (int y = landing; y <= landing + 1; y++) {
                    BlockPos p = new BlockPos(-5, y, z);
                    if (!isLandingDoor(level.getBlockState(p)) && !level.getBlockState(p).isAir())
                        return "The landing doorway is obstructed at " + p.toShortString();
                }
            }
            DestinyLiftEntity cab = ModEntities.DESTINY_LIFT.get().create(level);
            if (cab == null) return "Unable to create elevator cab.";
            cab.initialize(floor);
            if (!level.addFreshEntity(cab)) return "Elevator spawn was rejected; existing machinery was preserved.";
            Data data = Data.get(level);
            data.original = new ListTag();
            remember(data, ANCHOR, level.getBlockState(ANCHOR));
            motor.forEach((p, s) -> remember(data, p, s));
            for (BlockPos p : motor.keySet()) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
            for (int x = -4; x <= -3; x++) for (int z = -387; z <= -386; z++)
                level.setBlock(new BlockPos(x, floor - 1, z), ModBlocks.DESTINY_LIFT_SUPPORT.get().defaultBlockState(), 3);
            for (int landing : DestinyLiftStops.HEIGHTS) {
                BlockPos button = new BlockPos(-6, landing + 1, -388);
                remember(data, button, level.getBlockState(button));
                level.setBlock(button, Blocks.STONE_BUTTON.defaultBlockState()
                        .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.WALL)
                        .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST), 3);
                for (int z = -387; z <= -386; z++) for (int y = landing; y <= landing + 1; y++) {
                    BlockPos p = new BlockPos(-5, y, z);
                    remember(data, p, level.getBlockState(p));
                    level.setBlock(p, landingDoor(landing, y, z), 18);
                }
            }
            data.cab = cab.getUUID();
            data.setDirty();
            rememberStop(level, floor);
            setLandingDoors(level, floor);
            return "Smooth elevator installed. Use a landing button to call it; right-click inside for destinations.";
        }
        return "The original elevator must be intact and parked at a landing before upgrading.";
    }

    public static void rememberStop(ServerLevel level, int floor) {
        int index = DestinyLiftStops.index(floor);
        if (index >= 0) level.setBlock(ANCHOR, ModBlocks.DESTINY_LIFT_ANCHOR.get().defaultBlockState()
                .setValue(DestinyLiftAnchorBlock.STOP, index), 2);
    }

    private static Map<BlockPos, BlockState> legacyMotor(ServerLevel level, int floor) {
        int b = floor - 4;
        Map<BlockPos, BlockState> motor = new LinkedHashMap<>();
        int[][] cells = {{-4,0,-387,0},{-4,0,-386,1},{-3,0,-386,2},
                {-4,1,-387,1},{-3,1,-387,3},{-4,1,-386,4},{-3,1,-386,2},
                {-4,2,-387,1},{-3,2,-387,1},{-4,2,-386,1},{-3,2,-386,1},
                {-4,3,-387,5},{-3,3,-387,5},{-4,3,-386,5},{-3,3,-386,5}};
        for (int[] c : cells) {
            BlockPos p = new BlockPos(c[0], b + c[1], c[2]);
            BlockState s = level.getBlockState(p);
            boolean valid = switch (c[3]) {
                case 0, 4 -> s.is(Blocks.STICKY_PISTON) && !s.getValue(BlockStateProperties.EXTENDED)
                        && s.getValue(BlockStateProperties.FACING) == (c[3] == 0 ? Direction.UP : Direction.DOWN);
                case 1 -> s.is(Blocks.SLIME_BLOCK);
                case 2, 3 -> s.is(Blocks.OBSERVER) && s.getValue(BlockStateProperties.FACING) == (c[3] == 2 ? Direction.EAST : Direction.WEST);
                default -> s.is(Blocks.SEA_LANTERN);
            };
            if (!valid) return null;
            motor.put(p, s);
        }
        return motor;
    }

    private static void remember(Data data, BlockPos p, BlockState state) {
        CompoundTag entry = new CompoundTag(); entry.putLong("Pos", p.asLong());
        entry.put("State", NbtUtils.writeBlockState(state)); data.original.add(entry);
    }

    public static void setLandingDoors(ServerLevel level, int openFloor) {
        DestinyLiftEntity rescueCab = find(level);
        int released = Data.get(level).rescueFloor;
        boolean rescue = released >= 0 && (rescueCab == null || rescueCab.emergency());
        if (rescue) openFloor = released;
        if (openFloor >= 0 && !rescue) {
            DestinyLiftEntity cab = find(level);
            if (cab == null || cab.dock() != openFloor || !cab.landingDoorsOpen()) openFloor = -1;
        }
        for (int floor : DestinyLiftStops.HEIGHTS) for (int z = -387; z <= -386; z++) {
            for (int y = floor; y <= floor + 1; y++) {
                BlockPos p = new BlockPos(-5, y, z);
                BlockState s = level.getBlockState(p);
                if (s.is(ModBlocks.DESTINY_LIFT_DOOR.get()) && s.getValue(DestinyLiftDoorBlock.OPEN) != (floor == openFloor))
                    level.setBlock(p, s.setValue(DestinyLiftDoorBlock.OPEN, floor == openFloor), 2);
            }
        }
    }

    private static boolean isLandingDoor(BlockState state) {
        return state.is(Blocks.SPRUCE_DOOR) || state.is(ModBlocks.DESTINY_LIFT_DOOR.get());
    }

    public static void clearEmergency(ServerLevel level) {
        Data.get(level).rescueFloor = -1;
        Data.get(level).setDirty();
    }

    public static void releaseEmergencyLanding(ServerLevel level, int floor) {
        Data.get(level).rescueFloor = floor;
        Data.get(level).setDirty();
    }

    public static void rescue(ServerPlayer player, BlockPos clicked) {
        ServerLevel level = player.serverLevel();
        if (!isDestiny(level) || clicked.getX() < -7 || clicked.getX() > -2 || clicked.getZ() < -389
                || clicked.getZ() > -384 || clicked.getY() < 65 || clicked.getY() > 114) {
            player.displayClientMessage(Component.literal("Use emergency release on an elevator landing or shaft wall."), true);
            return;
        }
        DestinyLiftEntity cab = find(level);
        if (cab != null) cab.emergencyStop(player);
        int landing = -1;
        for (int floor : DestinyLiftStops.HEIGHTS)
            if (clicked.getY() >= floor - 1 && clicked.getY() <= floor + 2) { landing = floor; break; }
        Data.get(level).rescueFloor = landing;
        Data.get(level).setDirty();
        setLandingDoors(level, -1);
        player.displayClientMessage(Component.literal(landing >= 0 ? "Emergency release: landing Y " + landing
                + " and cab doors unlocked. Lift stopped. Kit kept." : "Lift stopped and cab doors released. Kit kept."), true);
    }

    private static BlockState landingDoor(int floor, int y, int z) {
        return ModBlocks.DESTINY_LIFT_DOOR.get().defaultBlockState()
                .setValue(DestinyLiftDoorBlock.PART, (y - floor) * 2 + z + 387);
    }

    /** Upgrade intact wooden cells only. Missing parts are restored by the repair kit. */
    public static void upgradeLandingDoors(ServerLevel level) {
        for (int floor : DestinyLiftStops.HEIGHTS) for (int z = -387; z <= -386; z++)
            for (int y = floor; y <= floor + 1; y++) {
                BlockPos pos = new BlockPos(-5, y, z);
                // Suppress vanilla two-half neighbor teardown until both old halves
                // are replaced; otherwise replacing the lower half deletes the upper.
                if (level.getBlockState(pos).is(Blocks.SPRUCE_DOOR)) level.setBlock(pos, landingDoor(floor, y, z), 18);
            }
        DestinyLiftEntity cab = find(level);
        setLandingDoors(level, cab != null && cab.doorsReady() ? cab.dock() : -1);
    }

    /** Repair the registered ship lift in place. Never replace unrelated blocks or spawn a second cab. */
    public static boolean repair(ServerPlayer player, BlockPos clicked) {
        ServerLevel level = player.serverLevel();
        if (!isDestiny(level) || clicked.getX() < -7 || clicked.getX() > -2
                || clicked.getZ() < -389 || clicked.getZ() > -384 || clicked.getY() < 65 || clicked.getY() > 114) {
            player.displayClientMessage(Component.literal("Use the kit on Destiny's elevator doorway, button or shaft wall."), true);
            return false;
        }
        DestinyLiftEntity cab = find(level);
        if (cab == null || cab.travelling()) {
            player.displayClientMessage(Component.literal(cab == null ? "Elevator not loaded or upgraded. Try its call button first."
                    : "Crouch-use the kit for an emergency stop and door release, then repair."), true);
            return false;
        }
        Map<BlockPos, BlockState> plan = new LinkedHashMap<>();
        int stop = DestinyLiftStops.index(cab.dock());
        if (stop < 0) {
            player.displayClientMessage(Component.literal("Clear the obstruction and call the lift to a landing before repairing."), true);
            return false;
        }
        plan.put(ANCHOR, ModBlocks.DESTINY_LIFT_ANCHOR.get().defaultBlockState().setValue(DestinyLiftAnchorBlock.STOP, stop));
        for (int floor : DestinyLiftStops.HEIGHTS) {
            plan.put(new BlockPos(-5, floor + 1, -388), Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState());
            plan.put(new BlockPos(-6, floor + 1, -388), Blocks.STONE_BUTTON.defaultBlockState()
                    .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.WALL)
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST));
            for (int z = -387; z <= -386; z++) for (int y = floor; y <= floor + 1; y++)
                plan.put(new BlockPos(-5, y, z), landingDoor(floor, y, z)
                        .setValue(DestinyLiftDoorBlock.OPEN, cab.doorsReady() && cab.dock() == floor));
        }
        if (Math.abs(cab.getY() + 1 - cab.dock()) < .00001)
            for (int x = -4; x <= -3; x++) for (int z = -387; z <= -386; z++)
                plan.put(new BlockPos(x, cab.dock() - 1, z), ModBlocks.DESTINY_LIFT_SUPPORT.get().defaultBlockState());
        int repaired = 0, blocked = 0;
        for (var entry : plan.entrySet()) {
            BlockPos pos = entry.getKey(); BlockState desired = entry.getValue(); BlockState old = level.getBlockState(pos);
            // OPEN is runtime state, not damage. A healthy kit use must never consume materials.
            if (old.is(desired.getBlock())) continue;
            boolean replace = old.isAir() || (desired.is(ModBlocks.DESTINY_LIFT_DOOR.get()) && old.is(Blocks.SPRUCE_DOOR))
                    || (pos.equals(ANCHOR) && old.is(Blocks.OBSIDIAN))
                    || (desired.is(ModBlocks.DESTINY_LIFT_SUPPORT.get()) && old.is(Blocks.SEA_LANTERN));
            boolean occupied = !level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                    new net.minecraft.world.phys.AABB(pos), e -> e.isAlive() && !e.isSpectator()).isEmpty();
            if (!replace || occupied) { blocked++; continue; }
            if (desired.is(Blocks.STONE_BUTTON) && !desired.canSurvive(level, pos)) { blocked++; continue; }
            if (level.setBlock(pos, desired, desired.is(ModBlocks.DESTINY_LIFT_DOOR.get()) ? 18 : 2)) repaired++;
        }
        setLandingDoors(level, cab.doorsReady() ? cab.dock() : -1);
        player.displayClientMessage(Component.literal(repaired > 0 ? "Repaired " + repaired + " elevator parts."
                + (blocked > 0 ? " " + blocked + " positions are obstructed; clear them and use another kit." : "")
                : blocked > 0 ? "Repair blocked. Clear replaced blocks or move away from the doorway. Kit kept."
                : "Elevator parts are intact. Kit kept."), true);
        return repaired > 0;
    }

    private static int controlFloor(BlockPos p) {
        int i = DestinyLiftStops.index(p.getY() - 1);
        return p.getZ() == -388 && (p.getX() == -6 || p.getX() == -4 || p.getX() == -3) && i >= 0 ? p.getY() - 1 : -1;
    }
    private static int doorFloor(BlockPos p) {
        if (p.getX() != -5 || (p.getZ() != -387 && p.getZ() != -386)) return -1;
        for (int floor : DestinyLiftStops.HEIGHTS) if (p.getY() == floor || p.getY() == floor + 1) return floor;
        return -1;
    }

    @SubscribeEvent public static void interact(PlayerInteractEvent.RightClickBlock event) {
        if (!isDestiny(event.getLevel())) return;
        // Let the kit's first-use handler run before buttons and doors consume the click.
        if (event.getItemStack().is(ModItems.DESTINY_LIFT_REPAIR_KIT.get())) return;
        int floor = controlFloor(event.getPos());
        if (floor >= 0 && !event.getLevel().getBlockState(event.getPos()).is(Blocks.STONE_BUTTON)) floor = -1;
        if (floor < 0 && isLandingDoor(event.getLevel().getBlockState(event.getPos()))) floor = doorFloor(event.getPos());
        if (floor < 0) return;
        event.setCanceled(true); event.setCancellationResult(InteractionResult.SUCCESS);
        // Offhand interactions must not fall through to vanilla door toggling.
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ServerLevel level = player.serverLevel();
        if (Data.get(level).cab == null) {
            String result = install(level);
            if (Data.get(level).cab == null) { player.displayClientMessage(Component.literal(result), true); return; }
        }
        DestinyLiftEntity cab = find(level);
        if (cab == null) { player.displayClientMessage(Component.literal("Elevator is loading; try again in a moment."), true); return; }
        if (cab.doorsReady() && cab.aboard(player)) DestinyLiftNetwork.open(player, cab);
        else cab.call(floor, player);
    }

    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && isDestiny(player.level())) {
            DestinyLiftEntity cab = find(player.serverLevel());
            if (cab != null && (cab.insideCab(player) || player.getVehicle() == cab)) cab.releaseOnLogout(player);
        }
    }

    @SubscribeEvent public static void dismount(net.minecraftforge.event.entity.EntityMountEvent event) {
        if (!event.isDismounting() || event.getEntityMounting().level().isClientSide) return;
        if (event.getEntityBeingMounted() instanceof DestinyLiftEntity cab
                && cab.travelling() && !cab.releasingPassengers()) {
            // Sneaking remains an escape route: stop and release instead of trapping
            // a player in a moving cab or letting its floor leave them behind.
            cab.requestEmergencyStop();
        }
    }

    public static void beforeShipReset(ServerLevel level) {
        DestinyLiftEntity cab = find(level);
        if (cab != null) {
            for (ServerPlayer player : level.players())
                if (cab.insideCab(player) || player.getVehicle() == cab) cab.releaseOnLogout(player);
            cab.discard();
        }
        Data.get(level).cab = null;
        Data.get(level).rescueFloor = -1;
        Data.get(level).setDirty();
    }

    @SubscribeEvent public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("sgjdestiny").requires(s -> s.hasPermission(2))
                .then(Commands.literal("ship").then(Commands.literal("retrofitElevator").executes(ctx -> {
                    ServerLevel level = ctx.getSource().getLevel();
                    String result = install(level);
                    ctx.getSource().sendSuccess(() -> Component.literal(result), false);
                    return find(level) != null ? 1 : 0;
                }))));
    }

    private static final class Data extends SavedData {
        private UUID cab;
        private int rescueFloor = -1;
        private ListTag original = new ListTag();
        static Data get(ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(Data::load, Data::new, "sgjdestiny_elevator");
        }
        static Data load(CompoundTag tag) {
            Data data = new Data();
            if (tag.hasUUID("Cab")) data.cab = tag.getUUID("Cab");
            data.rescueFloor = tag.contains("RescueFloor") ? tag.getInt("RescueFloor") : -1;
            data.original = tag.getList("OriginalBlocks", 10);
            return data;
        }
        @Override public CompoundTag save(CompoundTag tag) {
            if (cab != null) tag.putUUID("Cab", cab);
            tag.putInt("RescueFloor", rescueFloor);
            tag.put("OriginalBlocks", original);
            return tag;
        }
    }
}
