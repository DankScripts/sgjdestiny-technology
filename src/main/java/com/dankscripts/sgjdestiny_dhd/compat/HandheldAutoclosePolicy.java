package com.dankscripts.sgjdestiny_dhd.compat;

import java.util.UUID;

final class HandheldAutoclosePolicy {
    private HandheldAutoclosePolicy() {}

    static boolean shouldClose(UUID handheldConnection, UUID currentConnection, boolean used,
                               boolean kinoHold, int idleTicks, int timeoutTicks) {
        return handheldConnection != null && handheldConnection.equals(currentConnection)
                && used && !kinoHold && idleTicks > timeoutTicks;
    }
}
