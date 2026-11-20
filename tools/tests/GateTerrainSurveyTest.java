package com.dankscripts.sgjdestiny_dhd.compat;

public final class GateTerrainSurveyTest {
    public static void main(String[] args) {
        var flat = GateTerrainSurvey.measure(-1, 1, -1, 1, -64, 320, 9, (x, z) -> 70);
        check(flat != null && flat.baseY() == 70 && flat.variation() == 0 && flat.score() == 0,
                "flat ground was not selected");
        var slope = GateTerrainSurvey.measure(0, 2, 0, 0, -64, 320, 9,
                (x, z) -> new int[] {60, 65, 72}[x]);
        check(slope != null && slope.baseY() == 72 && slope.variation() == 12,
                "site must clear the highest terrain column");
        check(slope.score() > flat.score(), "rough ground should score worse than flat ground");
        check(GateTerrainSurvey.measure(0, 0, 0, 0, -64, 320, 9, (x, z) -> -64) == null,
                "void or missing terrain must not be used");
        check(GateTerrainSurvey.measure(0, 0, 0, 0, -64, 320, 9, (x, z) -> 312) == null,
                "structure must fit below the build limit");
        check(GateTerrainSurvey.isBuried(-8, 5, -4, (x, z) -> 62),
                "a gate near bedrock beneath continuous terrain should be relocated");
        check(!GateTerrainSurvey.isBuried(0, 65, 800, (x, z) -> 68),
                "an exposed gate should not be relocated");
        check(!GateTerrainSurvey.isBuried(0, 65, 800, (x, z) -> x < 0 ? 64 : 90),
                "one hillside must not classify an exposed gate as buried");
        check(GateTerrainSurvey.jungleClearing(0, 0, 0, 320, 9, (x, z) -> 70 + x / 8) != null,
                "a broad, gently sloping jungle clearing should be accepted");
        check(GateTerrainSurvey.jungleClearing(0, 0, 0, 320, 9,
                (x, z) -> Math.max(Math.abs(x), Math.abs(z)) <= 8 ? 115 : 95) == null,
                "a flat pedestal-sized ledge is not a clearing");
        check(GateTerrainSurvey.jungleClearing(0, 0, 0, 320, 9,
                (x, z) -> Math.max(Math.abs(x), Math.abs(z)) <= 24 ? 115 : 70) == null,
                "a broad but isolated mountaintop is not a landing site");
        check(GateTerrainSurvey.jungleClearing(0, 0, 0, 320, 9, (x, z) -> 1) == null,
                "a water-level shelf is not a dry clearing");
        verifyGateRecordAudit();
        System.out.println("PASS: terrain survey and stale gate record audit");
    }

    private static void verifyGateRecordAudit() {
        Object record = new Object();
        check(GateRecordAudit.classify(record, record, false, false) == GateRecordAudit.RecordState.STALE,
                "dead controller's own record can be pruned");
        check(GateRecordAudit.classify(record, new Object(), false, false) == GateRecordAudit.RecordState.UNRESOLVED,
                "pruning a dead record must not remove another record's address");
        check(GateRecordAudit.classify(record, record, true, false) == GateRecordAudit.RecordState.UNRESOLVED,
                "a present controller with unverified identity must not be pruned");
        check(GateRecordAudit.classify(record, record, true, true) == GateRecordAudit.RecordState.LIVE,
                "matching controller and owner identify the surviving gate");
        check(GateRecordAudit.decide(1, false, "original", "original") == GateRecordAudit.Action.USE_EXISTING,
                "pruned stale records must not block relocation of the surviving original");
        check(GateRecordAudit.decide(2, false, "original", null) == GateRecordAudit.Action.HOLD,
                "two live controllers must not be resolved by picking one at startup");
        check(GateRecordAudit.decide(1, false, "original", "other") == GateRecordAudit.Action.HOLD
                        && GateRecordAudit.decide(0, false, "original", null) == GateRecordAudit.Action.HOLD,
                "a missing saved primary must not be silently replaced");
        check(GateRecordAudit.decide(0, true, null, null) == GateRecordAudit.Action.HOLD
                        && GateRecordAudit.decide(0, false, null, null) == GateRecordAudit.Action.GENERATE,
                "only an empty, unambiguous destination without a saved primary may get a new address");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
