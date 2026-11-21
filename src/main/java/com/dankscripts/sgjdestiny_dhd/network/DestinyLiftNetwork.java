package com.dankscripts.sgjdestiny_dhd.network;

import com.dankscripts.sgjdestiny_dhd.DestinyDHD;
import com.dankscripts.sgjdestiny_dhd.client.DestinyLiftScreen;
import com.dankscripts.sgjdestiny_dhd.elevator.DestinyLiftEvents;
import com.dankscripts.sgjdestiny_dhd.elevator.DestinyLiftStops;
import com.dankscripts.sgjdestiny_dhd.entity.DestinyLiftEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class DestinyLiftNetwork {
    private DestinyLiftNetwork() {}
    public static void register() {}

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1.21.1-elevator1");
        registrar.playToClient(Open.TYPE, Open.CODEC,
                (packet, context) -> context.enqueueWork(() -> Client.open(packet)));
        registrar.playToServer(Select.TYPE, Select.CODEC,
                (packet, context) -> context.enqueueWork(() -> handle(packet, context.player())));
        registrar.playToServer(Control.TYPE, Control.CODEC,
                (packet, context) -> context.enqueueWork(() -> handle(packet, context.player())));
    }

    public static void open(ServerPlayer player, DestinyLiftEntity cab) {
        PacketDistributor.sendToPlayer(player, new Open(cab.getId(),
                cab.emergency() ? -1 : cab.travelling() ? -2 : cab.dock(), DestinyLiftStops.names()));
    }
    public static void select(int entity, int floor) { PacketDistributor.sendToServer(new Select(entity, floor)); }
    public static void control(int entity, boolean emergency) { PacketDistributor.sendToServer(new Control(entity, emergency)); }

    private static void handle(Control packet, net.minecraft.world.entity.player.Player rawPlayer) {
        if (rawPlayer instanceof ServerPlayer player && !player.isSpectator()
                && DestinyLiftEvents.isDestiny(player.level())
                && player.serverLevel().getEntity(packet.entity()) instanceof DestinyLiftEntity cab
                && cab == DestinyLiftEvents.find(player.serverLevel()) && cab.insideCab(player)) {
            if (packet.emergency()) cab.emergencyStop(player);
            open(player, cab);
        }
    }
    private static void handle(Select packet, net.minecraft.world.entity.player.Player rawPlayer) {
        if (rawPlayer instanceof ServerPlayer player && DestinyLiftEvents.isDestiny(player.level())
                && player.serverLevel().getEntity(packet.entity()) instanceof DestinyLiftEntity cab
                && cab == DestinyLiftEvents.find(player.serverLevel())) cab.select(packet.floor(), player);
    }

    private static final class Client {
        static void open(Open packet) { DestinyLiftScreen.open(packet.entity(), packet.current(), packet.names()); }
    }

    public record Control(int entity, boolean emergency) implements CustomPacketPayload {
        static final Type<Control> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(DestinyDHD.MOD_ID, "lift_control"));
        static final StreamCodec<RegistryFriendlyByteBuf, Control> CODEC = StreamCodec.of(
                (buf, packet) -> { buf.writeVarInt(packet.entity()); buf.writeBoolean(packet.emergency()); },
                buf -> new Control(buf.readVarInt(), buf.readBoolean()));
        @Override public Type<Control> type() { return TYPE; }
    }
    public record Open(int entity, int current, String[] names) implements CustomPacketPayload {
        static final Type<Open> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(DestinyDHD.MOD_ID, "lift_open"));
        static final StreamCodec<RegistryFriendlyByteBuf, Open> CODEC = StreamCodec.of(
                (buf, packet) -> {
                    buf.writeVarInt(packet.entity()); buf.writeVarInt(packet.current());
                    for (String name : packet.names()) buf.writeUtf(name, 48);
                }, buf -> {
                    int entity = buf.readVarInt(); int current = buf.readVarInt(); String[] names = new String[8];
                    for (int i = 0; i < names.length; i++) names[i] = buf.readUtf(48);
                    return new Open(entity, current, names);
                });
        @Override public Type<Open> type() { return TYPE; }
    }
    public record Select(int entity, int floor) implements CustomPacketPayload {
        static final Type<Select> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(DestinyDHD.MOD_ID, "lift_select"));
        static final StreamCodec<RegistryFriendlyByteBuf, Select> CODEC = StreamCodec.of(
                (buf, packet) -> { buf.writeVarInt(packet.entity()); buf.writeVarInt(packet.floor()); },
                buf -> new Select(buf.readVarInt(), buf.readVarInt()));
        @Override public Type<Select> type() { return TYPE; }
    }
}
