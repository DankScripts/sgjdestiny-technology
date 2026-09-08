package com.dankscripts.sgjdestiny_dhd.item;

import com.dankscripts.sgjdestiny_dhd.dialer.SeedShipAddressDatabase;
import com.dankscripts.sgjdestiny_dhd.network.DestinyDialerNetwork;
import com.dankscripts.sgjdestiny_dhd.registry.ModItems;
import com.dankscripts.sgjdestiny_dhd.config.DestinyServerConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import net.povstalec.sgjourney.common.block_entities.dhd.UniverseDHDEntity;
import net.povstalec.sgjourney.common.block_entities.stargate.UniverseStargateEntity;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Paired Destiny field controller: Universe dialing database and Kino controls. */
public final class KinoRemoteItem extends Item {
    private static final String MODE = "KinoMode";
    private static final String OWNER = "sgjdestiny_kino_owner";
    private static final String FOLLOW = "sgjdestiny_kino_follow";
    private static final String LAST_DIMENSION = "sgjdestiny_kino_last_dimension";
    private static final String CAMERA_PITCH = "sgjdestiny_kino_camera_pitch";
    private static final String CAMERA_HOME_YAW = "sgjdestiny_kino_camera_home_yaw";
    private static final String CAMERA_YAW = "sgjdestiny_kino_camera_yaw";

    public KinoRemoteItem(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResultHolder.success(stack);
        ServerPlayer serverPlayer = (ServerPlayer) player;
        CompoundTag tag = stack.getOrCreateTag();
        if (player.isShiftKeyDown()) {
            boolean kino = !tag.getBoolean(MODE);
            tag.putBoolean(MODE, kino);
            serverPlayer.displayClientMessage(Component.translatable(kino
                    ? "message.sgjdestiny_dhd.remote_kino_mode" : "message.sgjdestiny_dhd.remote_gate_mode"), true);
            return InteractionResultHolder.consume(stack);
        }
        if (tag.getBoolean(MODE)) DestinyDialerNetwork.openKinoControl(serverPlayer);
        else openGateDatabase(serverPlayer);
        player.getCooldowns().addCooldown(this, 8);
        return InteractionResultHolder.consume(stack);
    }

    private static void openGateDatabase(ServerPlayer player) {
        UniverseDHDEntity dhd = BlockPos.betweenClosedStream(player.blockPosition().offset(-16, -8, -16),
                        player.blockPosition().offset(16, 8, 16))
                .map(player.level()::getBlockEntity).filter(UniverseDHDEntity.class::isInstance)
                .map(UniverseDHDEntity.class::cast)
                .min(Comparator.comparingDouble(be -> be.getBlockPos().distSqr(player.blockPosition())))
                .orElse(null);
        if (dhd != null) {
            DestinyDialerNetwork.openHandheldDatabase(player, dhd,
                    SeedShipAddressDatabase.entriesFor(player.server, dhd));
            return;
        }
        UniverseStargateEntity gate = BlockPos.betweenClosedStream(player.blockPosition().offset(-32, -16, -32),
                        player.blockPosition().offset(32, 16, 32))
                .map(player.level()::getBlockEntity).filter(UniverseStargateEntity.class::isInstance)
                .map(UniverseStargateEntity.class::cast)
                .min(Comparator.comparingDouble(be -> be.getBlockPos().distSqr(player.blockPosition())))
                .orElse(null);
        if (gate == null) {
            player.displayClientMessage(Component.translatable("message.sgjdestiny_dhd.remote_no_gate"), true);
            return;
        }
        DestinyDialerNetwork.openHandheldDatabase(player, gate,
                SeedShipAddressDatabase.entriesFor(player.server, gate));
    }

    private static void operateKino(ServerPlayer player) {
        List<ArmorStand> kinos = ownedKinos(player);
        if (kinos.isEmpty()) deployKino(player);
        else if (player.isSprinting()) recallKino(player, kinos);
        else setFollowing(player, kinos, !kinos.get(0).getPersistentData().getBoolean(FOLLOW));
    }

    public static void deployFromItem(ServerPlayer player) {
        List<ArmorStand> kinos = ownedKinos(player);
        if (kinos.isEmpty()) deployKino(player);
        else setFollowing(player, kinos, true);
    }

    public static void handleKinoCommand(ServerPlayer player, int command) {
        boolean holdingRemote = player.getMainHandItem().is(ModItems.KINO_REMOTE.get())
                || player.getOffhandItem().is(ModItems.KINO_REMOTE.get());
        if (!holdingRemote) return;
        List<ArmorStand> kinos = ownedKinos(player);
        if (command == 0) {
            if (kinos.isEmpty()) deployKino(player);
            else setFollowing(player, kinos, true);
        } else if (command == 1 && !kinos.isEmpty()) {
            setFollowing(player, kinos, false);
        } else if (command == 2 && !kinos.isEmpty()) {
            recallKino(player, kinos);
        } else if (command >= 3 && command <= 8 && !kinos.isEmpty()) {
            moveKino(kinos, command);
        } else if (command == 9) {
            scanKino(player, kinos);
        }
    }

    private static List<ArmorStand> ownedKinos(ServerPlayer player) {
        String owner = player.getUUID().toString();
        java.util.ArrayList<ArmorStand> found = new java.util.ArrayList<>();
        for (ServerLevel level : player.server.getAllLevels()) {
            for (net.minecraft.world.entity.Entity entity : level.getAllEntities()) {
                if (entity instanceof ArmorStand stand && stand.getTags().contains("sgjdestiny_kino")
                        && owner.equals(stand.getPersistentData().getString(OWNER))) found.add(stand);
            }
        }
        return found;
    }

    public static boolean hasDeployedKino(ServerPlayer player) {
        return !ownedKinos(player).isEmpty();
    }

    public static ArmorStand getDeployedKino(ServerPlayer player) {
        List<ArmorStand> kinos = ownedKinos(player);
        return kinos.isEmpty() ? null : kinos.get(0);
    }

    public static float getCameraPitch(ArmorStand kino) {
        return kino.getPersistentData().getFloat(CAMERA_PITCH);
    }

    public static float getCameraYaw(ArmorStand kino) {
        CompoundTag data = kino.getPersistentData();
        if (!data.contains(CAMERA_YAW)) {
            data.putFloat(CAMERA_YAW, kino.getYRot());
            applyVisualYaw(kino, kino.getYRot());
        }
        return data.getFloat(CAMERA_YAW);
    }

    public static void setCameraYaw(ArmorStand kino, float cameraYaw) {
        kino.getPersistentData().putFloat(CAMERA_YAW, cameraYaw);
        applyVisualYaw(kino, cameraYaw);
    }

    private static void applyVisualYaw(ArmorStand kino, float cameraYaw) {
        float visualYaw = cameraYaw + 180;
        kino.setYRot(visualYaw);
        kino.setYBodyRot(visualYaw);
        kino.setYHeadRot(visualYaw);
    }

    public static VideoFrame captureVideo(ServerPlayer player) {
        List<ArmorStand> kinos = ownedKinos(player);
        if (kinos.isEmpty()) return null;
        ArmorStand kino = kinos.get(0);
        ServerLevel level = (ServerLevel) kino.level();
        final int width = 96, height = 54;
        int[] pixels = new int[width * height];
        Vec3 origin = kino.position().add(0, 1.45, 0);
        double yaw = Math.toRadians(getCameraYaw(kino));
        double pitch = Math.toRadians(kino.getPersistentData().getFloat(CAMERA_PITCH));
        Vec3 forward = new Vec3(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch),
                Math.cos(yaw) * Math.cos(pitch));
        Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
        Vec3 up = right.cross(forward).normalize();
        for (int py = 0; py < height; py++) {
            double v = ((py + .5) / height * 2 - 1) * .42;
            for (int px = 0; px < width; px++) {
                double u = ((px + .5) / width * 2 - 1) * .70;
                Vec3 direction = forward.add(right.scale(u)).add(up.scale(-v)).normalize();
                BlockHitResult hit = level.clip(new ClipContext(origin, origin.add(direction.scale(40)),
                        ClipContext.Block.VISUAL, ClipContext.Fluid.ANY, kino));
                int color;
                if (hit.getType() == HitResult.Type.MISS) {
                    double sky = 1.0 - (double) py / height;
                    int base = level.isDay() ? 72 : 8;
                    int r = base + (int)(45 * sky), g = base + (int)(64 * sky), b = base + (int)(92 * sky);
                    color = 0xFF000000 | Math.min(255, r) << 16 | Math.min(255, g) << 8 | Math.min(255, b);
                } else {
                    BlockPos pos = hit.getBlockPos();
                    int rgb = level.getBlockState(pos).getMapColor(level, pos).col;
                    double distance = Math.sqrt(origin.distanceToSqr(hit.getLocation()));
                    double shade = Math.max(.62, 1.12 - distance / 72.0);
                    int r = (int)(((rgb >> 16) & 255) * shade);
                    int g = (int)(((rgb >> 8) & 255) * shade);
                    int b = (int)((rgb & 255) * shade);
                    color = 0xFF000000 | r << 16 | g << 8 | b;
                }
                pixels[py * width + px] = color;
            }
        }
        return new VideoFrame(width, height, pixels, level.dimension().location().toString());
    }

    public record VideoFrame(int width, int height, int[] pixels, String dimension) {}

    private static void deployKino(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        long deployedKinos = 0;
        for (ServerLevel serverLevel : player.server.getAllLevels()) {
            for (net.minecraft.world.entity.Entity entity : serverLevel.getAllEntities()) {
                if (entity.getTags().contains("sgjdestiny_kino") && ++deployedKinos >= DestinyServerConfig.MAX_DEPLOYED_KINOS.get()) {
                    player.displayClientMessage(Component.translatable("message.sgjdestiny_dhd.kino_server_limit",
                            DestinyServerConfig.MAX_DEPLOYED_KINOS.get()), true);
                    return;
                }
            }
        }
        int slot = player.getInventory().findSlotMatchingItem(new ItemStack(ModItems.KINO.get()));
        if (slot < 0) {
            player.displayClientMessage(Component.translatable("message.sgjdestiny_dhd.kino_required"), true);
            return;
        }
        if (slot >= 0 && !player.getAbilities().instabuild) player.getInventory().removeItem(slot, 1);
        net.minecraft.world.phys.Vec3 look = player.getLookAngle();
        ArmorStand kino = new ArmorStand(level, player.getX() + look.x * 1.6,
                player.getEyeY() - 1.45, player.getZ() + look.z * 1.6);
        kino.setInvisible(true);
        kino.setInvulnerable(true);
        kino.setNoGravity(true);
        kino.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.KINO.get()));
        setCameraYaw(kino, player.getYRot());
        kino.getPersistentData().putString(OWNER, player.getUUID().toString());
        kino.getPersistentData().putBoolean(FOLLOW, false);
        kino.getPersistentData().putFloat(CAMERA_PITCH, 0);
        kino.getPersistentData().putString(LAST_DIMENSION, level.dimension().location().toString());
        kino.addTag("sgjdestiny_kino");
        level.addFreshEntity(kino);
        player.displayClientMessage(Component.translatable("message.sgjdestiny_dhd.kino_deployed"), true);
    }

    private static void recallKino(ServerPlayer player, List<ArmorStand> kinos) {
        for (ArmorStand kino : kinos) {
            kino.discard();
            player.getInventory().placeItemBackInInventory(new ItemStack(ModItems.KINO.get()));
        }
        player.displayClientMessage(Component.translatable("message.sgjdestiny_dhd.kino_recalled"), true);
    }

    private static void setFollowing(ServerPlayer player, List<ArmorStand> kinos, boolean follow) {
        for (ArmorStand kino : kinos) {
            kino.getPersistentData().putBoolean(FOLLOW, follow);
        }
        player.displayClientMessage(Component.translatable(follow
                ? "message.sgjdestiny_dhd.kino_follow" : "message.sgjdestiny_dhd.kino_hold"), true);
    }

    private static void moveKino(List<ArmorStand> kinos, int command) {
        for (ArmorStand kino : kinos) {
            kino.getPersistentData().putBoolean(FOLLOW, false);
            float cameraYaw = getCameraYaw(kino);
            double yaw = Math.toRadians(cameraYaw);
            double fx = -Math.sin(yaw), fz = Math.cos(yaw);
            double dx = 0, dy = 0, dz = 0;
            if (command == 3) { dx = fx; dz = fz; }
            if (command == 4) { dx = -fx; dz = -fz; }
            if (command == 5) { dx = fz; dz = -fx; cameraYaw -= 12; }
            if (command == 6) { dx = -fz; dz = fx; cameraYaw += 12; }
            setCameraYaw(kino, cameraYaw);
            if (command == 7) dy = 1;
            if (command == 8) dy = -1;
            double moveX = dx * .7, moveZ = dz * .7;
            kino.move(MoverType.SELF, new net.minecraft.world.phys.Vec3(moveX, dy * .55, moveZ));
            if (command >= 3 && command <= 6) maintainHoverHeight(kino);
        }
    }

    private static void maintainHoverHeight(ArmorStand kino) {
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos((int)Math.floor(kino.getX()),
                (int)Math.floor(kino.getY()) + 3, (int)Math.floor(kino.getZ()));
        ServerLevel level = (ServerLevel) kino.level();
        for (int i = 0; i < 12 && probe.getY() > level.getMinBuildHeight(); i++, probe.move(0, -1, 0)) {
            var state = level.getBlockState(probe);
            if (!state.getCollisionShape(level, probe).isEmpty()) {
                double groundTop = probe.getY() + state.getCollisionShape(level, probe).max(net.minecraft.core.Direction.Axis.Y);
                double targetY = groundTop + .15;
                kino.setPos(kino.getX(), kino.getY() + Math.max(-.7, Math.min(.7, targetY - kino.getY())), kino.getZ());
                return;
            }
        }
    }

    public static void lookKino(ServerPlayer player, float yawDelta, float pitchDelta, boolean reset) {
        for (ArmorStand kino : ownedKinos(player)) {
            CompoundTag data = kino.getPersistentData();
            if (reset) {
                if (data.contains(CAMERA_HOME_YAW)) setCameraYaw(kino, data.getFloat(CAMERA_HOME_YAW));
                data.remove(CAMERA_HOME_YAW);
                data.putFloat(CAMERA_PITCH, 0);
            } else {
                float cameraYaw = getCameraYaw(kino);
                if (!data.contains(CAMERA_HOME_YAW)) data.putFloat(CAMERA_HOME_YAW, cameraYaw);
                setCameraYaw(kino, cameraYaw + Math.max(-30, Math.min(30, yawDelta)));
                float pitch = data.getFloat(CAMERA_PITCH) + Math.max(-20, Math.min(20, pitchDelta));
                data.putFloat(CAMERA_PITCH, Math.max(-85, Math.min(85, pitch)));
            }
        }
    }

    private static void scanKino(ServerPlayer player, List<ArmorStand> kinos) {
        if (kinos.isEmpty()) {
            player.displayClientMessage(Component.literal("KINO LINK: NO SIGNAL").withStyle(ChatFormatting.RED), false);
            return;
        }
        ArmorStand kino = kinos.get(0);
        ServerLevel level = (ServerLevel) kino.level();
        var biome = level.getBiome(kino.blockPosition());
        float temperature = biome.value().getBaseTemperature();
        String dimension = level.dimension().location().toString();
        boolean waterPlanet = dimension.equals("sgjourney:water_planet");
        boolean breathable = level.dimensionType().natural() && !waterPlanet;
        String atmosphere = waterPlanet ? "THIN / POISONOUS" : breathable ? "BREATHABLE" : "HOSTILE / SUIT REQUIRED";
        player.displayClientMessage(Component.literal("KINO TELEMETRY — " + dimension).withStyle(ChatFormatting.AQUA), false);
        player.displayClientMessage(Component.literal(String.format(java.util.Locale.ROOT,
                "ATMOSPHERE: %s  TEMP INDEX: %.2f  ALTITUDE: %d", atmosphere, temperature, kino.getBlockY()))
                .withStyle(breathable ? ChatFormatting.GREEN : ChatFormatting.GOLD), false);
        player.displayClientMessage(Component.literal("BIOME: " + biome.unwrapKey()
                .map(key -> key.location().toString()).orElse("unknown")), false);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(stack.getOrCreateTag().getBoolean(MODE)
                ? "tooltip.sgjdestiny_dhd.remote_mode_kino" : "tooltip.sgjdestiny_dhd.remote_mode_gate").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.sgjdestiny_dhd.remote_controls").withStyle(ChatFormatting.GRAY));
    }

    public static String ownerKey() { return OWNER; }
    public static String followKey() { return FOLLOW; }
    public static String lastDimensionKey() { return LAST_DIMENSION; }
}
