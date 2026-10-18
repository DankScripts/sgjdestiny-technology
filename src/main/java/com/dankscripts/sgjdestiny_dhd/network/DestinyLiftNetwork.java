package com.dankscripts.sgjdestiny_dhd.network;

import com.dankscripts.sgjdestiny_dhd.DestinyDHD;
import com.dankscripts.sgjdestiny_dhd.client.DestinyLiftScreen;
import com.dankscripts.sgjdestiny_dhd.elevator.DestinyLiftEvents;
import com.dankscripts.sgjdestiny_dhd.elevator.DestinyLiftStops;
import com.dankscripts.sgjdestiny_dhd.entity.DestinyLiftEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.function.Supplier;

public final class DestinyLiftNetwork {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(DestinyDHD.MOD_ID, "elevator"), () -> "2", "2"::equals, "2"::equals);
    private DestinyLiftNetwork() {}
    public static void register() {
        CHANNEL.messageBuilder(Open.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(Open::encode).decoder(Open::new).consumerMainThread(Open::handle).add();
        CHANNEL.messageBuilder(Select.class, 1, NetworkDirection.PLAY_TO_SERVER)
                .encoder(Select::encode).decoder(Select::new).consumerMainThread(Select::handle).add();
        CHANNEL.messageBuilder(Control.class, 2, NetworkDirection.PLAY_TO_SERVER)
                .encoder(Control::encode).decoder(Control::new).consumerMainThread(Control::handle).add();
    }
    public static void open(ServerPlayer player, DestinyLiftEntity cab) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Open(cab.getId(),
                cab.emergency() ? -1 : cab.travelling() ? -2 : cab.dock(), DestinyLiftStops.names()));
    }
    public static void select(int entity, int floor) { CHANNEL.sendToServer(new Select(entity, floor)); }
    public static void control(int entity, boolean emergency) { CHANNEL.sendToServer(new Control(entity, emergency)); }

    private record Control(int entity, boolean emergency) {
        Control(FriendlyByteBuf buf) { this(buf.readVarInt(), buf.readBoolean()); }
        void encode(FriendlyByteBuf buf) { buf.writeVarInt(entity); buf.writeBoolean(emergency); }
        static void handle(Control msg, Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer player = ctx.get().getSender();
            if (player != null && !player.isSpectator() && DestinyLiftEvents.isDestiny(player.level())
                    && player.serverLevel().getEntity(msg.entity) instanceof DestinyLiftEntity cab
                    && cab == DestinyLiftEvents.find(player.serverLevel()) && cab.insideCab(player)) {
                if (msg.emergency) cab.emergencyStop(player);
                open(player, cab);
            }
            ctx.get().setPacketHandled(true);
        }
    }

    private record Open(int entity, int current, String[] names) {
        Open(FriendlyByteBuf buf) { this(buf.readVarInt(), buf.readVarInt(), readNames(buf)); }
        private static String[] readNames(FriendlyByteBuf buf) {
            String[] names = new String[8]; for (int i = 0; i < 8; i++) names[i] = buf.readUtf(48); return names;
        }
        void encode(FriendlyByteBuf buf) {
            buf.writeVarInt(entity); buf.writeVarInt(current); for (String name : names) buf.writeUtf(name, 48);
        }
        static void handle(Open msg, Supplier<NetworkEvent.Context> ctx) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> DestinyLiftScreen.open(msg.entity, msg.current, msg.names));
            ctx.get().setPacketHandled(true);
        }
    }
    private record Select(int entity, int floor) {
        Select(FriendlyByteBuf buf) { this(buf.readVarInt(), buf.readVarInt()); }
        void encode(FriendlyByteBuf buf) { buf.writeVarInt(entity); buf.writeVarInt(floor); }
        static void handle(Select msg, Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer player = ctx.get().getSender();
            if (player != null && DestinyLiftEvents.isDestiny(player.level())
                    && player.serverLevel().getEntity(msg.entity) instanceof DestinyLiftEntity cab
                    && cab == DestinyLiftEvents.find(player.serverLevel())) cab.select(msg.floor, player);
            ctx.get().setPacketHandled(true);
        }
    }
}
