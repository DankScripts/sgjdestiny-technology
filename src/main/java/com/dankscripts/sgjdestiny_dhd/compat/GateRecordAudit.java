package com.dankscripts.sgjdestiny_dhd.compat;

final class GateRecordAudit {
    enum RecordState { LIVE, STALE, UNRESOLVED }
    enum Action { USE_EXISTING, GENERATE, HOLD }

    private GateRecordAudit() {}

    static RecordState classify(Object record, Object addressOwner, boolean controllerPresent,
                                boolean controllerMatches) {
        if (controllerMatches && addressOwner == record) return RecordState.LIVE;
        if (!controllerPresent && (addressOwner == null || addressOwner == record)) return RecordState.STALE;
        return RecordState.UNRESOLVED;
    }

    static Action decide(int liveCount, boolean unresolved, Object savedPrimary, Object liveAddress) {
        if (unresolved || liveCount > 1) return Action.HOLD;
        if (liveCount == 0) return savedPrimary == null ? Action.GENERATE : Action.HOLD;
        return savedPrimary == null || savedPrimary.equals(liveAddress) ? Action.USE_EXISTING : Action.HOLD;
    }
}
