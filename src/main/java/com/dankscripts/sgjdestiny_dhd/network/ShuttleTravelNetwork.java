package com.dankscripts.sgjdestiny_dhd.network;

import com.dankscripts.sgjdestiny_dhd.DestinyDHD;
import com.dankscripts.sgjdestiny_dhd.client.ShuttleTravelClient;
import com.dankscripts.sgjdestiny_dhd.compat.ShuttleTravelEvents;
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

import java.util.function.Supplier;

public final class ShuttleTravelNetwork {
    private static final String VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(DestinyDHD.MOD_ID, "shuttle_travel"),
            () -> VERSION, VERSION::equals, VERSION::equals);

    private ShuttleTravelNetwork() {}

    public static void register() {
        CHANNEL.messageBuilder(Open.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(Open::encode).decoder(Open::new).consumerMainThread(Open::handle).add();
        CHANNEL.messageBuilder(Launch.class, 1, NetworkDirection.PLAY_TO_SERVER)
                .encoder(Launch::encode).decoder(Launch::new).consumerMainThread(Launch::handle).add();
        CHANNEL.messageBuilder(Status.class, 2, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(Status::encode).decoder(Status::new).consumerMainThread(Status::handle).add();
    }

    public static void open(ServerPlayer player, BlockPos anchor) {
        String destination = ShuttleTravelEvents.destinationName(player);
        if (destination == null) return;
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Open(anchor, destination));
    }

    public static void launch(BlockPos anchor) {
        CHANNEL.sendToServer(new Launch(anchor));
    }

    public static void status(ServerPlayer player, boolean launched, String message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Status(launched, message));
        if (!launched) player.displayClientMessage(Component.literal(message), false);
    }

    private record Open(BlockPos anchor, String destination) {
        Open(FriendlyByteBuf buf) { this(buf.readBlockPos(), buf.readUtf(24)); }
        void encode(FriendlyByteBuf buf) { buf.writeBlockPos(anchor); buf.writeUtf(destination); }
        static void handle(Open msg, Supplier<NetworkEvent.Context> ctx) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ShuttleTravelClient.open(msg.anchor, msg.destination));
            ctx.get().setPacketHandled(true);
        }
    }

    private record Launch(BlockPos anchor) {
        Launch(FriendlyByteBuf buf) { this(buf.readBlockPos()); }
        void encode(FriendlyByteBuf buf) { buf.writeBlockPos(anchor); }
        static void handle(Launch msg, Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) ShuttleTravelEvents.launch(player, msg.anchor);
            ctx.get().setPacketHandled(true);
        }
    }

    private record Status(boolean launched, String message) {
        Status(FriendlyByteBuf buf) { this(buf.readBoolean(), buf.readUtf(256)); }
        void encode(FriendlyByteBuf buf) { buf.writeBoolean(launched); buf.writeUtf(message); }
        static void handle(Status msg, Supplier<NetworkEvent.Context> ctx) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ShuttleTravelClient.status(msg.launched, msg.message));
            ctx.get().setPacketHandled(true);
        }
    }
}
