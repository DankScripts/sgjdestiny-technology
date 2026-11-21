package com.dankscripts.sgjdestiny_dhd.network;

import com.dankscripts.sgjdestiny_dhd.DestinyDHD;
import com.dankscripts.sgjdestiny_dhd.client.ShuttleTravelClient;
import com.dankscripts.sgjdestiny_dhd.compat.ShuttleTravelEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class ShuttleTravelNetwork {
    private ShuttleTravelNetwork() {}
    public static void register() {}
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1.21.1-shuttle1");
        registrar.playToClient(Open.TYPE, Open.CODEC,
                (packet, context) -> context.enqueueWork(() -> Client.open(packet)));
        registrar.playToServer(Launch.TYPE, Launch.CODEC,
                (packet, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) ShuttleTravelEvents.launch(player, packet.anchor());
                }));
        registrar.playToClient(Status.TYPE, Status.CODEC,
                (packet, context) -> context.enqueueWork(() -> Client.status(packet)));
    }
    public static void open(ServerPlayer player, BlockPos anchor) {
        String destination = ShuttleTravelEvents.destinationName(player);
        if (destination != null) PacketDistributor.sendToPlayer(player, new Open(anchor, destination));
    }
    public static void launch(BlockPos anchor) { PacketDistributor.sendToServer(new Launch(anchor)); }
    public static void status(ServerPlayer player, boolean launched, String message) {
        PacketDistributor.sendToPlayer(player, new Status(launched, message));
        if (!launched) player.displayClientMessage(Component.literal(message), false);
    }
    private static final class Client {
        static void open(Open packet) { ShuttleTravelClient.open(packet.anchor(), packet.destination()); }
        static void status(Status packet) { ShuttleTravelClient.status(packet.launched(), packet.message()); }
    }
    public record Open(BlockPos anchor, String destination) implements CustomPacketPayload {
        static final Type<Open> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(DestinyDHD.MOD_ID, "shuttle_open"));
        static final StreamCodec<RegistryFriendlyByteBuf, Open> CODEC = StreamCodec.of(
                (buf, packet) -> { buf.writeBlockPos(packet.anchor()); buf.writeUtf(packet.destination(), 24); },
                buf -> new Open(buf.readBlockPos(), buf.readUtf(24)));
        @Override public Type<Open> type() { return TYPE; }
    }
    public record Launch(BlockPos anchor) implements CustomPacketPayload {
        static final Type<Launch> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(DestinyDHD.MOD_ID, "shuttle_launch"));
        static final StreamCodec<RegistryFriendlyByteBuf, Launch> CODEC = StreamCodec.of(
                (buf, packet) -> buf.writeBlockPos(packet.anchor()), buf -> new Launch(buf.readBlockPos()));
        @Override public Type<Launch> type() { return TYPE; }
    }
    public record Status(boolean launched, String message) implements CustomPacketPayload {
        static final Type<Status> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(DestinyDHD.MOD_ID, "shuttle_status"));
        static final StreamCodec<RegistryFriendlyByteBuf, Status> CODEC = StreamCodec.of(
                (buf, packet) -> { buf.writeBoolean(packet.launched()); buf.writeUtf(packet.message(), 256); },
                buf -> new Status(buf.readBoolean(), buf.readUtf(256)));
        @Override public Type<Status> type() { return TYPE; }
    }
}
