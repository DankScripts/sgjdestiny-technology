package com.dankscripts.sgjdestiny_dhd.compat;

public final class DialInputBufferTest {
    public static void main(String[] args) {
        DialInputBuffer buffer = new DialInputBuffer(3, 3);
        check(buffer.addSymbol(7), "second symbol was rejected");
        check(buffer.addEngage(), "engage was rejected");
        check(buffer.addSymbol(9), "symbol after engage was rejected");
        check(!buffer.tick() && !buffer.tick() && buffer.tick(), "wrong delay");
        var actions = buffer.actions();
        check(actions.size() == 4, "input was lost");
        check(!actions.get(0).engage() && actions.get(0).symbol() == 3, "first symbol changed");
        check(!actions.get(1).engage() && actions.get(1).symbol() == 7, "second symbol reordered");
        check(actions.get(2).engage(), "engage reordered");
        check(!actions.get(3).engage() && actions.get(3).symbol() == 9, "later symbol reordered");
        for (int i = 4; i < 16; i++) check(buffer.addSymbol(i), "queue filled early");
        check(!buffer.addEngage() && buffer.actions().size() == 16, "queue did not reject overflow");
        verifyProgrammedDial(new int[] {1, 2, 3, 4, 5, 6, 0});
        verifyProgrammedDial(new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9});
        var sent = new java.util.ArrayList<Integer>();
        ProgrammedDialSequence.send(new int[] {1, 2, 3, 4, 5, 6, 0}, (symbol, last) -> {
            sent.add(symbol);
            return symbol != 3;
        });
        check(sent.equals(java.util.List.of(1, 2, 3)), "dial did not stop after rejected symbol");
        verifyHandheldAutoclose();
        System.out.println("PASS: delayed Destiny dialer input ordering and bounded overflow");
    }

    private static void verifyProgrammedDial(int[] symbols) {
        var sent = new java.util.ArrayList<Integer>();
        var autoEngage = new java.util.ArrayList<Boolean>();
        ProgrammedDialSequence.send(symbols, (symbol, last) -> {
            sent.add(symbol);
            autoEngage.add(last);
            return true;
        });
        check(sent.size() == symbols.length, "programmed dial lost symbols");
        for (int i = 0; i < symbols.length; i++) {
            check(sent.get(i) == symbols[i], "programmed dial reordered symbols");
            check(autoEngage.get(i) == (i == symbols.length - 1), "auto-engage set on wrong symbol");
        }
    }

    private static void verifyHandheldAutoclose() {
        var connection = java.util.UUID.randomUUID();
        check(HandheldAutoclosePolicy.shouldClose(connection, connection, true, false, 201, 200),
                "used handheld connections should close after the idle timeout");
        check(!HandheldAutoclosePolicy.shouldClose(connection, java.util.UUID.randomUUID(), true, false, 201, 200),
                "a different connection must not inherit handheld auto-close");
        check(!HandheldAutoclosePolicy.shouldClose(connection, connection, false, false, 201, 200),
                "untraveled connections should not auto-close");
        check(!HandheldAutoclosePolicy.shouldClose(connection, connection, true, true, 201, 200),
                "Kino holds must remain effective");
        check(!HandheldAutoclosePolicy.shouldClose(connection, connection, true, false, 200, 200),
                "the timeout boundary should match SGJourney's strict greater-than rule");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
