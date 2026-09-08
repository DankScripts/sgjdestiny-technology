package com.dankscripts.sgjdestiny_dhd.network;

import com.dankscripts.sgjdestiny_dhd.DestinyDHD;
import com.dankscripts.sgjdestiny_dhd.client.SeedShipDialerScreen;
import com.dankscripts.sgjdestiny_dhd.dialer.SeedShipAddressDatabase;
import com.dankscripts.sgjdestiny_dhd.compat.DestinyDialDelay;
import com.dankscripts.sgjdestiny_dhd.item.KinoRemoteItem;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.povstalec.sgjourney.common.block_entities.dhd.UniverseDHDEntity;
import net.povstalec.sgjourney.common.block_entities.stargate.UniverseStargateEntity;
import net.povstalec.sgjourney.common.sgjourney.Address;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.entity.decoration.ArmorStand;
import qouteall.imm_ptl.core.api.PortalAPI;
import qouteall.imm_ptl.core.chunk_loading.ChunkLoader;
import qouteall.imm_ptl.core.chunk_loading.DimensionalChunkPos;

public final class DestinyDialerNetwork {
    private static final String VERSION = "10";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(DestinyDHD.MOD_ID, "seed_ship_dialer"),
            () -> VERSION, VERSION::equals, VERSION::equals);
    private static final Map<UUID, ChunkLoader> KINO_LOADERS = new ConcurrentHashMap<>();

    private DestinyDialerNetwork() {}

    public static void register() {
        CHANNEL.messageBuilder(OpenDatabase.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenDatabase::encode).decoder(OpenDatabase::new)
                .consumerMainThread(OpenDatabase::handle).add();
        CHANNEL.messageBuilder(DialAddress.class, 1, NetworkDirection.PLAY_TO_SERVER)
                .encoder(DialAddress::encode).decoder(DialAddress::new)
                .consumerMainThread(DialAddress::handle).add();
        CHANNEL.messageBuilder(Disconnect.class, 2, NetworkDirection.PLAY_TO_SERVER)
                .encoder(Disconnect::encode).decoder(Disconnect::new)
                .consumerMainThread(Disconnect::handle).add();
        CHANNEL.messageBuilder(KinoCommand.class, 3, NetworkDirection.PLAY_TO_SERVER)
                .encoder(KinoCommand::encode).decoder(KinoCommand::new)
                .consumerMainThread(KinoCommand::handle).add();
        CHANNEL.messageBuilder(KinoStatus.class, 4, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(KinoStatus::encode).decoder(KinoStatus::new)
                .consumerMainThread(KinoStatus::handle).add();
        CHANNEL.messageBuilder(KinoVideoFrame.class, 5, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(KinoVideoFrame::encode).decoder(KinoVideoFrame::new)
                .consumerMainThread(KinoVideoFrame::handle).add();
        CHANNEL.messageBuilder(KinoLook.class, 6, NetworkDirection.PLAY_TO_SERVER)
                .encoder(KinoLook::encode).decoder(KinoLook::new)
                .consumerMainThread(KinoLook::handle).add();
    }

    public static void openSeedShipDatabase(ServerPlayer player, UniverseDHDEntity dhd,
                                            List<SeedShipAddressDatabase.Entry> entries) {
        var gate = dhd.stargateCache.get() == null ? null : dhd.stargateCache.get().getStargate();
        var gateEntity = dhd.stargateCache.get();
        int state = gate != null && gate.isConnected() ? 2
                : gateEntity != null && (gateEntity.isDialingOut() || gateEntity.getChevronsEngaged() > 0
                || !gateEntity.getEncodedSymbols().isEmpty()) ? 1 : 0;
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new OpenDatabase(dhd.getBlockPos(), false, false, entries, state, false, false));
    }

    public static void openSeedShipDatabase(ServerPlayer player, UniverseStargateEntity gate,
                                            List<SeedShipAddressDatabase.Entry> entries) {
        int state = gate.isConnected() ? 2
                : gate.isDialingOut() || gate.dialFromBuffer() || gate.isRotating()
                || gate.getChevronsEngaged() > 0 ? 1 : 0;
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new OpenDatabase(gate.getBlockPos(), true, false, entries, state, false, false));
    }

    public static void openHandheldDatabase(ServerPlayer player, UniverseDHDEntity dhd,
                                            List<SeedShipAddressDatabase.Entry> entries) {
        var gate = dhd.stargateCache.get();
        int state = gate != null && gate.isConnected() ? 2
                : gate != null && (gate.isDialingOut() || gate.getChevronsEngaged() > 0
                || !gate.getEncodedSymbols().isEmpty()) ? 1 : 0;
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new OpenDatabase(dhd.getBlockPos(), false, true, entries, state, false,
                        KinoRemoteItem.hasDeployedKino(player)));
    }

    public static void openHandheldDatabase(ServerPlayer player, UniverseStargateEntity gate,
                                            List<SeedShipAddressDatabase.Entry> entries) {
        int state = gate.isConnected() ? 2
                : gate.isDialingOut() || gate.dialFromBuffer() || gate.isRotating()
                || gate.getChevronsEngaged() > 0 ? 1 : 0;
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new OpenDatabase(gate.getBlockPos(), true, true, entries, state, false,
                        KinoRemoteItem.hasDeployedKino(player)));
    }

    public static void openKinoControl(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new OpenDatabase(player.blockPosition(), false, true, List.of(), 0, true,
                        KinoRemoteItem.hasDeployedKino(player)));
    }

    public static void dial(BlockPos target, boolean directGate, Address address) {
        CHANNEL.sendToServer(new DialAddress(target, directGate, address.toArray()));
    }

    public static void disconnect(BlockPos target, boolean directGate) {
        CHANNEL.sendToServer(new Disconnect(target, directGate));
    }

    public static void kinoCommand(int command) { CHANNEL.sendToServer(new KinoCommand(command)); }
    public static void kinoLook(float yawDelta, float pitchDelta, boolean reset) {
        CHANNEL.sendToServer(new KinoLook(yawDelta, pitchDelta, reset));
    }

    public record ClientEntry(Component name, int[] symbols, boolean earth) {}

    public static final class OpenDatabase {
        private final BlockPos console;
        private final boolean directGate;
        private final boolean handheld;
        private final List<ClientEntry> entries;
        private final int gateState;
        private final boolean kinoInitially;
        private final boolean kinoDeployed;
        OpenDatabase(BlockPos console, boolean directGate, boolean handheld,
                     List<SeedShipAddressDatabase.Entry> entries, int gateState, boolean kinoInitially,
                     boolean kinoDeployed) {
            this.console = console;
            this.directGate = directGate;
            this.handheld = handheld;
            this.entries = entries.stream().map(e -> new ClientEntry(e.name(), e.address().toArray(), e.earth())).toList();
            this.gateState = gateState;
            this.kinoInitially = kinoInitially;
            this.kinoDeployed = kinoDeployed;
        }
        OpenDatabase(FriendlyByteBuf buffer) {
            console = buffer.readBlockPos();
            directGate = buffer.readBoolean();
            handheld = buffer.readBoolean();
            int count = buffer.readVarInt();
            List<ClientEntry> read = new ArrayList<>(count);
            for (int i = 0; i < count; i++) read.add(new ClientEntry(buffer.readComponent(),
                    buffer.readVarIntArray(), buffer.readBoolean()));
            entries = List.copyOf(read);
            gateState = buffer.readByte();
            kinoInitially = buffer.readBoolean();
            kinoDeployed = buffer.readBoolean();
        }
        void encode(FriendlyByteBuf buffer) {
            buffer.writeBlockPos(console);
            buffer.writeBoolean(directGate);
            buffer.writeBoolean(handheld);
            buffer.writeVarInt(entries.size());
            for (ClientEntry entry : entries) {
                buffer.writeComponent(entry.name());
                buffer.writeVarIntArray(entry.symbols());
                buffer.writeBoolean(entry.earth());
            }
            buffer.writeByte(gateState);
            buffer.writeBoolean(kinoInitially);
            buffer.writeBoolean(kinoDeployed);
        }
        static void handle(OpenDatabase message, Supplier<NetworkEvent.Context> context) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    Minecraft.getInstance().setScreen(new SeedShipDialerScreen(message.console, message.directGate,
                            message.handheld,
                            message.entries, message.gateState, message.kinoInitially, message.kinoDeployed)));
            context.get().setPacketHandled(true);
        }
    }

    public static final class DialAddress {
        private final BlockPos console;
        private final boolean directGate;
        private final int[] symbols;
        DialAddress(BlockPos console, boolean directGate, int[] symbols) {
            this.console = console; this.directGate = directGate; this.symbols = symbols;
        }
        DialAddress(FriendlyByteBuf buffer) {
            console = buffer.readBlockPos(); directGate = buffer.readBoolean(); symbols = buffer.readVarIntArray();
        }
        void encode(FriendlyByteBuf buffer) {
            buffer.writeBlockPos(console); buffer.writeBoolean(directGate); buffer.writeVarIntArray(symbols);
        }
        static void handle(DialAddress message, Supplier<NetworkEvent.Context> context) {
            ServerPlayer player = context.get().getSender();
            if (player != null && player.distanceToSqr(message.console.getX() + 0.5,
                    message.console.getY() + 0.5, message.console.getZ() + 0.5) <= 4096.0) {
                try {
                    Address.Immutable address = new Address.Immutable(message.symbols);
                    if (message.directGate
                            && player.level().getBlockEntity(message.console) instanceof UniverseStargateEntity gate
                            && SeedShipAddressDatabase.isAuthorizedSelection(player.server, gate, address)
                            && queueDirectGateDial(gate, message.symbols)) {
                    } else if (!message.directGate
                            && player.level().getBlockEntity(message.console) instanceof UniverseDHDEntity dhd
                            && SeedShipAddressDatabase.isAuthorizedSelection(player.server, dhd, address)
                            && DestinyDialDelay.queueProgrammedDial(dhd, message.symbols)) {
                    } else {
                        player.displayClientMessage(Component.translatable("message.sgjdestiny_dhd.dialer_busy"), true);
                    }
                } catch (IllegalArgumentException ignored) {
                    player.displayClientMessage(Component.translatable("message.sgjdestiny_dhd.invalid_destination"), true);
                }
            }
            context.get().setPacketHandled(true);
        }

        private static boolean queueDirectGateDial(UniverseStargateEntity gate, int[] symbols) {
            if (gate.isConnected() || gate.isDialingOut() || gate.dialFromBuffer()
                    || gate.isRotating() || gate.getChevronsEngaged() > 0) return false;
            int[] dialSymbols = symbols;
            if (symbols.length == 6) {
                dialSymbols = java.util.Arrays.copyOf(symbols, 7);
                dialSymbols[6] = 0;
            }
            for (int i = 0; i < dialSymbols.length; i++) {
                gate.indirectEngageSymbol(dialSymbols[i], i == dialSymbols.length - 1);
            }
            return true;
        }
    }

    public static final class Disconnect {
        private final BlockPos console;
        private final boolean directGate;
        Disconnect(BlockPos console, boolean directGate) { this.console = console; this.directGate = directGate; }
        Disconnect(FriendlyByteBuf buffer) { console = buffer.readBlockPos(); directGate = buffer.readBoolean(); }
        void encode(FriendlyByteBuf buffer) { buffer.writeBlockPos(console); buffer.writeBoolean(directGate); }
        static void handle(Disconnect message, Supplier<NetworkEvent.Context> context) {
            ServerPlayer player = context.get().getSender();
            if (player != null && player.distanceToSqr(message.console.getX() + .5,
                    message.console.getY() + .5, message.console.getZ() + .5) <= 4096) {
                if (message.directGate
                        && player.level().getBlockEntity(message.console) instanceof UniverseStargateEntity gate) {
                    gate.engageStargate();
                } else if (!message.directGate
                    && player.level().getBlockEntity(message.console) instanceof UniverseDHDEntity dhd
                    && dhd.stargateCache.get() != null && dhd.stargateCache.get().getStargate() != null) {
                    DestinyDialDelay.cancel(dhd);
                    dhd.stargateCache.get().dhdDisconnectStargate(dhd);
                }
            }
            context.get().setPacketHandled(true);
        }
    }

    public static final class KinoCommand {
        private final int command;
        KinoCommand(int command) { this.command = command; }
        KinoCommand(FriendlyByteBuf buffer) { command = buffer.readVarInt(); }
        void encode(FriendlyByteBuf buffer) { buffer.writeVarInt(command); }
        static void handle(KinoCommand message, Supplier<NetworkEvent.Context> context) {
            ServerPlayer player = context.get().getSender();
            if (player != null && message.command >= 0 && message.command <= 11) {
                if (message.command == 10) {
                    ArmorStand kino = KinoRemoteItem.getDeployedKino(player);
                    if (kino != null) {
                        updateKinoLoader(player, kino);
                        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new KinoVideoFrame(
                                kino.level().dimension().location().toString(), kino.getX(), kino.getY() + 1.45,
                                kino.getZ(), KinoRemoteItem.getCameraYaw(kino), KinoRemoteItem.getCameraPitch(kino)));
                    }
                } else if (message.command == 11) {
                    removeKinoLoader(player);
                } else {
                    KinoRemoteItem.handleKinoCommand(player, message.command);
                    CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                            new KinoStatus(KinoRemoteItem.hasDeployedKino(player)));
                }
            }
            context.get().setPacketHandled(true);
        }
    }

    private static void updateKinoLoader(ServerPlayer player, ArmorStand kino) {
        DimensionalChunkPos center = new DimensionalChunkPos(kino.level().dimension(), new ChunkPos(kino.blockPosition()));
        ChunkLoader old = KINO_LOADERS.get(player.getUUID());
        if (old != null && old.center.equals(center)) return;
        if (old != null) PortalAPI.removeChunkLoaderForPlayer(player, old);
        ChunkLoader loader = new ChunkLoader(center, 6);
        PortalAPI.addChunkLoaderForPlayer(player, loader);
        KINO_LOADERS.put(player.getUUID(), loader);
    }

    private static void removeKinoLoader(ServerPlayer player) {
        ChunkLoader old = KINO_LOADERS.remove(player.getUUID());
        if (old != null) PortalAPI.removeChunkLoaderForPlayer(player, old);
    }

    public static final class KinoStatus {
        private final boolean deployed;
        KinoStatus(boolean deployed) { this.deployed = deployed; }
        KinoStatus(FriendlyByteBuf buffer) { deployed = buffer.readBoolean(); }
        void encode(FriendlyByteBuf buffer) { buffer.writeBoolean(deployed); }
        static void handle(KinoStatus message, Supplier<NetworkEvent.Context> context) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                if (Minecraft.getInstance().screen instanceof SeedShipDialerScreen screen) {
                    screen.setKinoDeployed(message.deployed);
                }
            });
            context.get().setPacketHandled(true);
        }
    }

    public static final class KinoVideoFrame {
        private final String dimension;
        private final double x, y, z;
        private final float yaw, pitch;
        KinoVideoFrame(String dimension, double x, double y, double z, float yaw, float pitch) {
            this.dimension = dimension; this.x = x; this.y = y; this.z = z; this.yaw = yaw; this.pitch = pitch;
        }
        KinoVideoFrame(FriendlyByteBuf buffer) {
            dimension = buffer.readUtf(128); x = buffer.readDouble(); y = buffer.readDouble(); z = buffer.readDouble();
            yaw = buffer.readFloat(); pitch = buffer.readFloat();
        }
        void encode(FriendlyByteBuf buffer) {
            buffer.writeUtf(dimension, 128); buffer.writeDouble(x); buffer.writeDouble(y); buffer.writeDouble(z);
            buffer.writeFloat(yaw); buffer.writeFloat(pitch);
        }
        static void handle(KinoVideoFrame message, Supplier<NetworkEvent.Context> context) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                if (Minecraft.getInstance().screen instanceof SeedShipDialerScreen screen) {
                    screen.setKinoCamera(message.dimension, message.x, message.y, message.z, message.yaw, message.pitch);
                }
            });
            context.get().setPacketHandled(true);
        }
    }

    public static final class KinoLook {
        private final float yawDelta, pitchDelta;
        private final boolean reset;
        KinoLook(float yawDelta, float pitchDelta, boolean reset) {
            this.yawDelta = yawDelta; this.pitchDelta = pitchDelta; this.reset = reset;
        }
        KinoLook(FriendlyByteBuf buffer) {
            yawDelta = buffer.readFloat(); pitchDelta = buffer.readFloat(); reset = buffer.readBoolean();
        }
        void encode(FriendlyByteBuf buffer) {
            buffer.writeFloat(yawDelta); buffer.writeFloat(pitchDelta); buffer.writeBoolean(reset);
        }
        static void handle(KinoLook message, Supplier<NetworkEvent.Context> context) {
            ServerPlayer player = context.get().getSender();
            if (player != null && Float.isFinite(message.yawDelta) && Float.isFinite(message.pitchDelta)) {
                KinoRemoteItem.lookKino(player, message.yawDelta, message.pitchDelta, message.reset);
            }
            context.get().setPacketHandled(true);
        }
    }
}
