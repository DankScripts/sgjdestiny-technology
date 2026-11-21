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
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
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
    private static final TicketType<BlockPos> HANDHELD_DIAL_TICKET =
            TicketType.create("destiny_handheld_dial", Comparator.comparingLong(BlockPos::asLong), 1200);
    private static final Map<UUID, ChunkLoader> KINO_LOADERS = new ConcurrentHashMap<>();

    private DestinyDialerNetwork() {}

    public static void register() {}

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1.21.1-dialer1");
        registrar.playToClient(OpenDatabase.TYPE, OpenDatabase.CODEC,
                (packet, context) -> context.enqueueWork(packet::handleClient));
        registrar.playToServer(DialAddress.TYPE, DialAddress.CODEC,
                (packet, context) -> context.enqueueWork(() -> packet.handleServer(context.player())));
        registrar.playToServer(Disconnect.TYPE, Disconnect.CODEC,
                (packet, context) -> context.enqueueWork(() -> packet.handleServer(context.player())));
        registrar.playToServer(KinoCommand.TYPE, KinoCommand.CODEC,
                (packet, context) -> context.enqueueWork(() -> packet.handleServer(context.player())));
        registrar.playToClient(KinoStatus.TYPE, KinoStatus.CODEC,
                (packet, context) -> context.enqueueWork(packet::handleClient));
        registrar.playToClient(KinoVideoFrame.TYPE, KinoVideoFrame.CODEC,
                (packet, context) -> context.enqueueWork(packet::handleClient));
        registrar.playToServer(KinoLook.TYPE, KinoLook.CODEC,
                (packet, context) -> context.enqueueWork(() -> packet.handleServer(context.player())));
    }

    public static void openSeedShipDatabase(ServerPlayer player, UniverseDHDEntity dhd,
                                            List<SeedShipAddressDatabase.Entry> entries) {
        var gateEntity = DestinyDHDApi.connectedStargateEntity(dhd);
        var gate = gateEntity == null ? null : gateEntity.getStargate();
        int state = gate != null && gate.isConnected() ? 2
                : gateEntity != null && (gateEntity.isDialingOut() || gateEntity.getChevronsEngaged() > 0
                || !gateEntity.getEncodedSymbols().isEmpty()) ? 1 : 0;
        PacketDistributor.sendToPlayer(player,
                new OpenDatabase(dhd.getBlockPos(), false, false, entries, state, false, false));
    }

    public static void openSeedShipDatabase(ServerPlayer player, UniverseStargateEntity gate,
                                            List<SeedShipAddressDatabase.Entry> entries) {
        int state = gate.isConnected() ? 2
                : gate.isDialingOut() || gate.dialFromBuffer() || gate.isRotating()
                || gate.getChevronsEngaged() > 0 ? 1 : 0;
        PacketDistributor.sendToPlayer(player,
                new OpenDatabase(gate.getBlockPos(), true, false, entries, state, false, false));
    }

    public static void openHandheldDatabase(ServerPlayer player, UniverseDHDEntity dhd,
                                            List<SeedShipAddressDatabase.Entry> entries) {
        var gate = DestinyDHDApi.connectedStargateEntity(dhd);
        int state = gate != null && gate.isConnected() ? 2
                : gate != null && (gate.isDialingOut() || gate.getChevronsEngaged() > 0
                || !gate.getEncodedSymbols().isEmpty()) ? 1 : 0;
        PacketDistributor.sendToPlayer(player,
                new OpenDatabase(dhd.getBlockPos(), false, true, entries, state, false,
                        KinoRemoteItem.hasDeployedKino(player)));
    }

    public static void openHandheldDatabase(ServerPlayer player, UniverseStargateEntity gate,
                                            List<SeedShipAddressDatabase.Entry> entries) {
        int state = gate.isConnected() ? 2
                : gate.isDialingOut() || gate.dialFromBuffer() || gate.isRotating()
                || gate.getChevronsEngaged() > 0 ? 1 : 0;
        PacketDistributor.sendToPlayer(player,
                new OpenDatabase(gate.getBlockPos(), true, true, entries, state, false,
                        KinoRemoteItem.hasDeployedKino(player)));
    }

    public static void openKinoControl(ServerPlayer player) {
        validateKinoLink(player);
        PacketDistributor.sendToPlayer(player,
                new OpenDatabase(player.blockPosition(), false, true, List.of(), 0, true,
                        KinoRemoteItem.hasDeployedKino(player)));
    }

    public static void dial(BlockPos target, boolean directGate, boolean handheld, Address address) {
        PacketDistributor.sendToServer(new DialAddress(target, directGate, handheld, address.toArray()));
    }

    public static void disconnect(BlockPos target, boolean directGate, boolean handheld) {
        PacketDistributor.sendToServer(new Disconnect(target, directGate, handheld));
    }

    public static void kinoCommand(int command) { PacketDistributor.sendToServer(new KinoCommand(command)); }
    public static void kinoLook(float yawDelta, float pitchDelta, boolean reset) {
        PacketDistributor.sendToServer(new KinoLook(yawDelta, pitchDelta, reset));
    }

    public static void suspendKinoLink(ServerPlayer player) {
        removeKinoLoader(player);
        PacketDistributor.sendToPlayer(player, new KinoStatus(false));
    }

    public static void restoreKinoLink(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new KinoStatus(true));
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

    public static final class OpenDatabase implements CustomPacketPayload {
        static final Type<OpenDatabase> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(DestinyDHD.MOD_ID, "dialer_open"));
        static final StreamCodec<RegistryFriendlyByteBuf, OpenDatabase> CODEC = StreamCodec.of(
                (buf, packet) -> packet.encode(buf), OpenDatabase::new);
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
            for (int i = 0; i < count; i++) read.add(new ClientEntry(ComponentSerialization.STREAM_CODEC.decode((RegistryFriendlyByteBuf) buffer),
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
                ComponentSerialization.STREAM_CODEC.encode((RegistryFriendlyByteBuf) buffer, entry.name());
                buffer.writeVarIntArray(entry.symbols());
                buffer.writeBoolean(entry.earth());
            }
            buffer.writeByte(gateState);
            buffer.writeBoolean(kinoInitially);
            buffer.writeBoolean(kinoDeployed);
        }
        void handleClient() {
            DestinyDialerClientPackets.openDatabase(console, directGate, handheld, entries,
                    gateState, kinoInitially, kinoDeployed);
        }
        @Override public Type<OpenDatabase> type() { return TYPE; }
    }

    public static final class DialAddress implements CustomPacketPayload {
        static final Type<DialAddress> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(DestinyDHD.MOD_ID, "dialer_dial"));
        static final StreamCodec<RegistryFriendlyByteBuf, DialAddress> CODEC = StreamCodec.of(
                (buf, packet) -> packet.encode(buf), DialAddress::new);
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
        void handleServer(net.minecraft.world.entity.player.Player rawPlayer) {
            if (rawPlayer instanceof ServerPlayer player
                    && authorizedControlDistance(player, console, handheld, directGate)) {
                try {
                    Address.Immutable address = new Address.Immutable(symbols);
                    UniverseStargateEntity gate = directGate
                            ? resolveDirectGate(player, console, handheld) : null;
                    if (gate != null
                            && SeedShipAddressDatabase.isAuthorizedSelection(player.server, gate, address)
                            && queueDirectGateDial(player, gate, symbols, handheld)) {
                    } else if (!directGate
                            && player.level().getBlockEntity(console) instanceof UniverseDHDEntity dhd
                            && SeedShipAddressDatabase.isAuthorizedSelection(player.server, dhd, address)
                            && DestinyDialDelay.queueProgrammedDial(dhd, symbols, handheld)) {
                    } else {
                        player.displayClientMessage(Component.translatable("message.sgjdestiny_dhd.dialer_busy"), true);
                    }
                } catch (IllegalArgumentException ignored) {
                    player.displayClientMessage(Component.translatable("message.sgjdestiny_dhd.invalid_destination"), true);
                }
            }
        }
        @Override public Type<DialAddress> type() { return TYPE; }

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
                BlockPos pos = gate.getBlockPos();
                player.serverLevel().getChunkSource().addRegionTicket(HANDHELD_DIAL_TICKET, new ChunkPos(pos), 2, pos);
            }
            return feedback != null;
        }
    }

    public static final class Disconnect implements CustomPacketPayload {
        static final Type<Disconnect> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(DestinyDHD.MOD_ID, "dialer_disconnect"));
        static final StreamCodec<RegistryFriendlyByteBuf, Disconnect> CODEC = StreamCodec.of(
                (buf, packet) -> packet.encode(buf), Disconnect::new);
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
        void handleServer(net.minecraft.world.entity.player.Player rawPlayer) {
            if (rawPlayer instanceof ServerPlayer player
                    && authorizedControlDistance(player, console, handheld, directGate)) {
                UniverseStargateEntity gate = directGate
                        ? resolveDirectGate(player, console, handheld) : null;
                if (gate != null) {
                    gate.engageStargate();
                } else if (directGate && handheld) {
                    player.displayClientMessage(Component.translatable("message.sgjdestiny_dhd.remote_no_gate"), true);
                } else if (!directGate
                    && player.level().getBlockEntity(console) instanceof UniverseDHDEntity dhd
                    && DestinyDHDApi.connectedStargate(dhd) != null) {
                    DestinyDialDelay.cancel(dhd);
                    dhd.engageStargate();
                }
            }
        }
        @Override public Type<Disconnect> type() { return TYPE; }
    }

    private static UniverseStargateEntity resolveDirectGate(ServerPlayer player, BlockPos target, boolean handheld) {
        if (handheld) return HandheldGateLookup.at(player.serverLevel(), target);
        return player.level().getBlockEntity(target) instanceof UniverseStargateEntity gate ? gate : null;
    }

    private static boolean authorizedControlDistance(ServerPlayer player, BlockPos target, boolean handheld,
                                                      boolean directGate) {
        if (handheld) {
            boolean holdingRemote = player.getMainHandItem().is(
                    com.dankscripts.sgjdestiny_dhd.registry.ModItems.KINO_REMOTE.get())
                    || player.getOffhandItem().is(
                    com.dankscripts.sgjdestiny_dhd.registry.ModItems.KINO_REMOTE.get());
            if (!holdingRemote) return false;
            if (directGate) return true;
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

    public static final class KinoCommand implements CustomPacketPayload {
        static final Type<KinoCommand> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(DestinyDHD.MOD_ID, "kino_command"));
        static final StreamCodec<RegistryFriendlyByteBuf, KinoCommand> CODEC = StreamCodec.of(
                (buf, packet) -> packet.encode(buf), KinoCommand::new);
        private final int command;
        KinoCommand(int command) { this.command = command; }
        KinoCommand(FriendlyByteBuf buffer) { command = buffer.readVarInt(); }
        void encode(FriendlyByteBuf buffer) { buffer.writeVarInt(command); }
        void handleServer(net.minecraft.world.entity.player.Player rawPlayer) {
            if (rawPlayer instanceof ServerPlayer player && command >= 0 && command <= 12) {
                if (command == 10) {
                    ArmorStand kino = validateKinoLink(player);
                    if (kino != null) {
                        sendKinoVideoFrame(player, kino);
                    }
                } else if (command == 11) {
                    removeKinoLoader(player);
                } else {
                    validateKinoLink(player);
                    KinoRemoteItem.handleKinoCommand(player, command);
                    PacketDistributor.sendToPlayer(player, new KinoStatus(KinoRemoteItem.hasDeployedKino(player)));
                }
            }
        }
        @Override public Type<KinoCommand> type() { return TYPE; }
    }

    private static void sendKinoVideoFrame(ServerPlayer player, ArmorStand kino) {
        updateKinoLoader(player, kino);
        PacketDistributor.sendToPlayer(player, new KinoVideoFrame(
                kino.level().dimension().location().toString(), kino.getX(), kino.getY() + 1.45,
                kino.getZ(), KinoRemoteItem.getCameraYaw(kino), KinoRemoteItem.getCameraPitch(kino)));
    }

    private static void updateKinoLoader(ServerPlayer player, ArmorStand kino) {
        DimensionalChunkPos center = new DimensionalChunkPos(kino.level().dimension(), new ChunkPos(kino.blockPosition()));
        ChunkLoader old = KINO_LOADERS.get(player.getUUID());
        if (old != null && old.getCenter().equals(center)) return;
        if (old != null) PortalAPI.removeChunkLoaderForPlayer(player, old);
        ChunkLoader loader = new ChunkLoader(center, 6);
        PortalAPI.addChunkLoaderForPlayer(player, loader);
        KINO_LOADERS.put(player.getUUID(), loader);
    }

    private static void removeKinoLoader(ServerPlayer player) {
        ChunkLoader old = KINO_LOADERS.remove(player.getUUID());
        if (old != null) PortalAPI.removeChunkLoaderForPlayer(player, old);
    }

    public static final class KinoStatus implements CustomPacketPayload {
        static final Type<KinoStatus> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(DestinyDHD.MOD_ID, "kino_status"));
        static final StreamCodec<RegistryFriendlyByteBuf, KinoStatus> CODEC = StreamCodec.of(
                (buf, packet) -> packet.encode(buf), KinoStatus::new);
        private final boolean deployed;
        KinoStatus(boolean deployed) { this.deployed = deployed; }
        KinoStatus(FriendlyByteBuf buffer) { deployed = buffer.readBoolean(); }
        void encode(FriendlyByteBuf buffer) { buffer.writeBoolean(deployed); }
        void handleClient() { DestinyDialerClientPackets.updateKinoStatus(deployed); }
        @Override public Type<KinoStatus> type() { return TYPE; }
    }

    public static final class KinoVideoFrame implements CustomPacketPayload {
        static final Type<KinoVideoFrame> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(DestinyDHD.MOD_ID, "kino_video"));
        static final StreamCodec<RegistryFriendlyByteBuf, KinoVideoFrame> CODEC = StreamCodec.of(
                (buf, packet) -> packet.encode(buf), KinoVideoFrame::new);
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
        void handleClient() { DestinyDialerClientPackets.updateKinoVideo(dimension, x, y, z, yaw, pitch); }
        @Override public Type<KinoVideoFrame> type() { return TYPE; }
    }

    public static final class KinoLook implements CustomPacketPayload {
        static final Type<KinoLook> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(DestinyDHD.MOD_ID, "kino_look"));
        static final StreamCodec<RegistryFriendlyByteBuf, KinoLook> CODEC = StreamCodec.of(
                (buf, packet) -> packet.encode(buf), KinoLook::new);
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
        void handleServer(net.minecraft.world.entity.player.Player rawPlayer) {
            if (rawPlayer instanceof ServerPlayer player && Float.isFinite(yawDelta) && Float.isFinite(pitchDelta)) {
                if (validateKinoLink(player) != null) {
                    KinoRemoteItem.lookKino(player, yawDelta, pitchDelta, reset);
                }
            }
        }
        @Override public Type<KinoLook> type() { return TYPE; }
    }
}
