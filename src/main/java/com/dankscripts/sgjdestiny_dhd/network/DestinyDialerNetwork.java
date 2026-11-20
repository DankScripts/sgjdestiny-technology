package com.dankscripts.sgjdestiny_dhd.network;

import com.dankscripts.sgjdestiny_dhd.DestinyDHD;
import com.dankscripts.sgjdestiny_dhd.client.DestinyDialerClientPackets;
import com.dankscripts.sgjdestiny_dhd.dialer.SeedShipAddressDatabase;
import com.dankscripts.sgjdestiny_dhd.compat.DestinyDialDelay;
import com.dankscripts.sgjdestiny_dhd.compat.DestinyDHDApi;
import com.dankscripts.sgjdestiny_dhd.compat.KinoEvents;
import com.dankscripts.sgjdestiny_dhd.compat.HandheldAutoclose;
import com.dankscripts.sgjdestiny_dhd.compat.HandheldDialSymbols;
import com.dankscripts.sgjdestiny_dhd.compat.HandheldGateLookup;
import com.dankscripts.sgjdestiny_dhd.item.KinoRemoteItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
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
import net.povstalec.sgjourney.common.sgjourney.StargateInfo;
import net.povstalec.sgjourney.common.data.StargateNetwork;
import net.povstalec.sgjourney.common.init.StargateInit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
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
    private static final Logger LOGGER = LoggerFactory.getLogger("SGJ Destiny DHD/Dial");
    private static final String VERSION = "11";
    private static final TicketType<BlockPos> HANDHELD_DIAL_TICKET =
            TicketType.create("destiny_handheld_dial", Comparator.comparingLong(BlockPos::asLong), 1200);
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
        var gateEntity = DestinyDHDApi.connectedStargateEntity(dhd);
        var gate = gateEntity == null ? null : gateEntity.getStargate();
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
                                            List<SeedShipAddressDatabase.Entry> entries, boolean kinoInitially) {
        var gate = DestinyDHDApi.connectedStargateEntity(dhd);
        int state = gate != null && gate.isConnected() ? 2
                : gate != null && (gate.isDialingOut() || gate.getChevronsEngaged() > 0
                || !gate.getEncodedSymbols().isEmpty()) ? 1 : 0;
        sendHandheld(player, dhd.getBlockPos(), false, entries, state, kinoInitially);
    }

    public static void openHandheldDatabase(ServerPlayer player, UniverseStargateEntity gate,
                                            List<SeedShipAddressDatabase.Entry> entries, boolean kinoInitially) {
        int state = gate.isConnected() ? 2
                : gate.isDialingOut() || gate.dialFromBuffer() || gate.isRotating()
                || gate.getChevronsEngaged() > 0 ? 1 : 0;
        sendHandheld(player, gate.getBlockPos(), true, entries, state, kinoInitially);
    }

    public static void openKinoControl(ServerPlayer player, boolean kinoInitially) {
        sendHandheld(player, player.blockPosition(), false, List.of(), 0, kinoInitially);
    }

    private static void sendHandheld(ServerPlayer player, BlockPos target, boolean directGate,
                                     List<SeedShipAddressDatabase.Entry> entries, int state, boolean kinoInitially) {
        if (kinoInitially) validateKinoLink(player);
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new OpenDatabase(target, directGate, true, entries, state, kinoInitially,
                        KinoRemoteItem.hasDeployedKino(player)));
    }

    public static void dial(BlockPos target, boolean directGate, boolean handheld, Address address) {
        CHANNEL.sendToServer(new DialAddress(target, directGate, handheld, address.toArray()));
    }

    public static void disconnect(BlockPos target, boolean directGate, boolean handheld) {
        CHANNEL.sendToServer(new Disconnect(target, directGate, handheld));
    }

    public static void kinoCommand(int command) { CHANNEL.sendToServer(new KinoCommand(command)); }
    public static void kinoLook(float yawDelta, float pitchDelta, boolean reset) {
        CHANNEL.sendToServer(new KinoLook(yawDelta, pitchDelta, reset));
    }

    public static void suspendKinoLink(ServerPlayer player) {
        removeKinoLoader(player);
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new KinoStatus(false));
    }

    public static void restoreKinoLink(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new KinoStatus(true));
    }

    public static void refreshKinoVideoAfterTransit(ServerPlayer player, ArmorStand kino) {
        if (player == null || kino == null || !KINO_LOADERS.containsKey(player.getUUID())) return;
        sendKinoVideoFrame(player, kino);
    }

    private static ArmorStand validateKinoLink(ServerPlayer player) {
        ArmorStand kino = KinoRemoteItem.getDeployedKino(player);
        if (kino != null && !KinoEvents.hasOpenRoute(player, kino)) {
            KinoRemoteItem.setLinked(kino, false);
            suspendKinoLink(player);
            return null;
        }
        return kino;
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
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> DestinyDialerClientPackets.openDatabase(
                    message.console, message.directGate, message.handheld, message.entries,
                    message.gateState, message.kinoInitially, message.kinoDeployed));
            context.get().setPacketHandled(true);
        }
    }

    public static final class DialAddress {
        private final BlockPos console;
        private final boolean directGate;
        private final boolean handheld;
        private final int[] symbols;
        DialAddress(BlockPos console, boolean directGate, boolean handheld, int[] symbols) {
            this.console = console; this.directGate = directGate; this.handheld = handheld; this.symbols = symbols;
        }
        DialAddress(FriendlyByteBuf buffer) {
            console = buffer.readBlockPos(); directGate = buffer.readBoolean();
            handheld = buffer.readBoolean(); symbols = buffer.readVarIntArray();
        }
        void encode(FriendlyByteBuf buffer) {
            buffer.writeBlockPos(console); buffer.writeBoolean(directGate);
            buffer.writeBoolean(handheld); buffer.writeVarIntArray(symbols);
        }
        static void handle(DialAddress message, Supplier<NetworkEvent.Context> context) {
            ServerPlayer player = context.get().getSender();
            if (player != null && authorizedControlDistance(player, message.console, message.handheld, message.directGate)) {
                try {
                    Address.Immutable address = new Address.Immutable(message.symbols);
                    UniverseStargateEntity gate = message.directGate
                            ? resolveDirectGate(player, message.console, message.handheld) : null;
                    if (gate != null
                            && SeedShipAddressDatabase.isAuthorizedSelection(player.server, gate, address)
                            && queueDirectGateDial(player, gate, message.symbols, message.handheld)) {
                    } else if (!message.directGate
                            && player.level().getBlockEntity(message.console) instanceof UniverseDHDEntity dhd
                            && SeedShipAddressDatabase.isAuthorizedSelection(player.server, dhd, address)
                            && DestinyDialDelay.queueProgrammedDial(dhd, message.symbols, message.handheld)) {
                    } else {
                        player.displayClientMessage(Component.translatable("message.sgjdestiny_dhd.dialer_busy"), true);
                    }
                } catch (IllegalArgumentException ignored) {
                    player.displayClientMessage(Component.translatable("message.sgjdestiny_dhd.invalid_destination"), true);
                }
            }
            context.get().setPacketHandled(true);
        }

        private static boolean queueDirectGateDial(ServerPlayer player, UniverseStargateEntity gate, int[] symbols,
                                                   boolean handheld) {
            if (gate.isConnected() || gate.isDialingOut() || gate.isDialing()
                    || gate.isRotating() || gate.getChevronsEngaged() > 0) return false;
            Address.Immutable dialAddress = Address.Immutable.extendWithPointOfOrigin(new Address.Immutable(symbols));
            if (!dialAddress.canBeDialed()) {
                player.displayClientMessage(Component.translatable("message.sgjdestiny_dhd.invalid_destination"), true);
                return false;
            }
            int[] dialSymbols = dialAddress.toArray();
            StargateInfo.FeedbackMessage feedback = null;
            for (int i = 0; i < dialSymbols.length; i++) {
                int symbol = handheld
                        ? HandheldDialSymbols.physicalSymbol(gate.symbolMap, dialSymbols[i], dialSymbols)
                        : dialSymbols[i];
                boolean last = i == dialSymbols.length - 1;
                feedback = handheld && last
                        ? HandheldAutoclose.engage(gate,
                                () -> gate.indirectEngageSymbol(symbol, true, StargateInfo.ChevronSound.ENGAGE))
                        : gate.indirectEngageSymbol(symbol, last, StargateInfo.ChevronSound.ENGAGE);
                if (feedback.feedback().isError()) break;
            }
            LOGGER.info("Direct dial from {} at {} region={} address={} energy={}/{} feedback={}",
                    gate.getLevel() == null ? "unknown" : gate.getLevel().dimension().location(),
                    gate.getBlockPos(), gate.getStargate() == null ? "none" : gate.getStargate().getAddressRegionKey(),
                    dialAddress, gate.energyStorage.getTrueEnergyStored(),
                    gate.energyStorage.getTrueMaxEnergyStored(), feedback);
            if (feedback != null && feedback.feedback().isError()) {
                gate.resetStargate(feedback);
                player.displayClientMessage(feedback.getMessageComponent(), false);
                return false;
            }
            if (handheld && feedback != null) {
                // Keep the buffered symbol sequence ticking until SGJourney takes over chunk loading.
                BlockPos pos = gate.getBlockPos();
                player.serverLevel().getChunkSource().addRegionTicket(HANDHELD_DIAL_TICKET, new ChunkPos(pos), 2, pos);
            }
            return feedback != null;
        }
    }

    public static final class Disconnect {
        private final BlockPos console;
        private final boolean directGate;
        private final boolean handheld;
        Disconnect(BlockPos console, boolean directGate, boolean handheld) {
            this.console = console; this.directGate = directGate; this.handheld = handheld;
        }
        Disconnect(FriendlyByteBuf buffer) {
            console = buffer.readBlockPos(); directGate = buffer.readBoolean(); handheld = buffer.readBoolean();
        }
        void encode(FriendlyByteBuf buffer) {
            buffer.writeBlockPos(console); buffer.writeBoolean(directGate); buffer.writeBoolean(handheld);
        }
        static void handle(Disconnect message, Supplier<NetworkEvent.Context> context) {
            ServerPlayer player = context.get().getSender();
            if (player != null && authorizedControlDistance(player, message.console, message.handheld, message.directGate)) {
                UniverseStargateEntity gate = message.directGate
                        ? resolveDirectGate(player, message.console, message.handheld) : null;
                if (gate != null) {
                    gate.engageStargate();
                } else if (message.directGate && message.handheld) {
                    player.displayClientMessage(Component.translatable("message.sgjdestiny_dhd.remote_no_gate"), true);
                } else if (!message.directGate
                    && player.level().getBlockEntity(message.console) instanceof UniverseDHDEntity dhd
                    && DestinyDHDApi.connectedStargate(dhd) != null) {
                    DestinyDialDelay.cancel(dhd);
                    dhd.engageStargate();
                }
            }
            context.get().setPacketHandled(true);
        }
    }

    private static UniverseStargateEntity resolveDirectGate(ServerPlayer player, BlockPos target, boolean handheld) {
        if (handheld) return HandheldGateLookup.at(player.serverLevel(), target);
        return player.level().getBlockEntity(target) instanceof UniverseStargateEntity gate ? gate : null;
    }

    private static boolean authorizedControlDistance(ServerPlayer player, BlockPos target, boolean handheld, boolean directGate) {
        if (handheld) {
            boolean holdingRemote = player.getMainHandItem().is(
                    com.dankscripts.sgjdestiny_dhd.registry.ModItems.KINO_REMOTE.get())
                    || player.getOffhandItem().is(
                    com.dankscripts.sgjdestiny_dhd.registry.ModItems.KINO_REMOTE.get());
            if (!holdingRemote) return false;
            if (directGate) return true; // resolveDirectGate verifies the exact registered position before loading it.
            // Authenticate a remote request against the gate network before
            // looking up the target block entity, which may load its chunk.
            return StargateNetwork.get(player.server).getStargatesInDimension(
                    player.level().dimension()).stream()
                    .anyMatch(record -> record.getStargateType() == StargateInit.UNIVERSE.get()
                            && record.getPosition() != null
                            && record.getPosition().distanceToSqr(target.getCenter()) <= 32 * 32);
        }
        return player.distanceToSqr(target.getX() + .5, target.getY() + .5, target.getZ() + .5) <= 4096.0;
    }

    public static final class KinoCommand {
        private final int command;
        KinoCommand(int command) { this.command = command; }
        KinoCommand(FriendlyByteBuf buffer) { command = buffer.readVarInt(); }
        void encode(FriendlyByteBuf buffer) { buffer.writeVarInt(command); }
        static void handle(KinoCommand message, Supplier<NetworkEvent.Context> context) {
            ServerPlayer player = context.get().getSender();
            if (player != null && message.command >= 0 && message.command <= 12) {
                if (message.command == 10) {
                    ArmorStand kino = validateKinoLink(player);
                    if (kino != null) {
                        sendKinoVideoFrame(player, kino);
                    }
                } else if (message.command == 11) {
                    removeKinoLoader(player);
                } else {
                    validateKinoLink(player);
                    KinoRemoteItem.handleKinoCommand(player, message.command);
                    CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                            new KinoStatus(KinoRemoteItem.hasDeployedKino(player)));
                }
            }
            context.get().setPacketHandled(true);
        }
    }

    private static void sendKinoVideoFrame(ServerPlayer player, ArmorStand kino) {
        updateKinoLoader(player, kino);
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new KinoVideoFrame(
                kino.level().dimension().location().toString(), kino.getX(), kino.getY() + 1.45,
                kino.getZ(), KinoRemoteItem.getCameraYaw(kino), KinoRemoteItem.getCameraPitch(kino)));
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
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> DestinyDialerClientPackets.updateKinoStatus(message.deployed));
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
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> DestinyDialerClientPackets.updateKinoVideo(
                    message.dimension, message.x, message.y, message.z, message.yaw, message.pitch));
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
                if (validateKinoLink(player) != null) {
                    KinoRemoteItem.lookKino(player, message.yawDelta, message.pitchDelta, message.reset);
                }
            }
            context.get().setPacketHandled(true);
        }
    }
}
