package com.dankscripts.sgjdestiny_dhd.compat;

import net.povstalec.sgjourney.common.block_entities.stargate.UniverseStargateEntity;
import net.povstalec.sgjourney.common.items.crystals.ControlCrystalItem;
import net.povstalec.sgjourney.common.sgjourney.StargateConnection;
import net.povstalec.sgjourney.common.sgjourney.StargateInfo;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.Supplier;

public final class HandheldAutoclose {
    private static final String CONNECTION = "sgjdestiny_handheld_connection";
    private static final Set<UniverseStargateEntity> PENDING = Collections.newSetFromMap(new WeakHashMap<>());

    private HandheldAutoclose() {}

    public static StargateInfo.FeedbackMessage engage(UniverseStargateEntity gate,
                                                      Supplier<StargateInfo.FeedbackMessage> finalSymbol) {
        if (gate.isConnected()) return finalSymbol.get();
        PENDING.add(gate);
        boolean accepted = false;
        try {
            var feedback = finalSymbol.get();
            accepted = !feedback.feedback().isError();
            return feedback;
        } finally {
            if (!accepted) PENDING.remove(gate);
        }
    }

    public static void onConnect(UniverseStargateEntity gate, UUID id, StargateConnection.State state) {
        if (PENDING.remove(gate) && state == StargateConnection.State.OUTGOING_CONNECTION)
            gate.getPersistentData().putUUID(CONNECTION, id);
    }

    public static void onReset(UniverseStargateEntity gate) {
        PENDING.remove(gate);
        if (gate.getPersistentData().contains(CONNECTION)) {
            gate.getPersistentData().remove(CONNECTION);
            gate.setChanged();
        }
    }

    public static boolean shouldClose(UniverseStargateEntity gate, StargateConnection connection) {
        var data = gate.getPersistentData();
        return data.hasUUID(CONNECTION) && HandheldAutoclosePolicy.shouldClose(
                data.getUUID(CONNECTION), connection.getID(), connection.used(), KinoEvents.shouldHoldOpen(gate),
                connection.getTimeSinceLastTraveler(), ControlCrystalItem.AUTOCLOSE_TICKS);
    }
}
