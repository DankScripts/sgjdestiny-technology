package com.dankscripts.sgjdestiny_dhd.compat;

import net.povstalec.sgjourney.common.sgjourney.SymbolMap;

public final class DialInputBufferTest {
    public static void main(String[] args) {
        verifyHandheldAutoclose();
        verifyHandheldSymbols(new int[] {36, 20, 1, 15, 14, 7, 0});
        int laterGlyph = new SymbolMap(36).remapToRandomSymbol(36);
        int[] nineChevron = {36, 38, laterGlyph, 0, 0, 0, 0, 0, 0};
        for (int i = 3, glyph = 1; i < nineChevron.length; i++, glyph++) {
            if (glyph == laterGlyph) glyph++;
            nineChevron[i] = glyph;
        }
        verifyHandheldSymbols(nineChevron);
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
        System.out.println("PASS: Destiny dial ordering, handheld glyph remapping and auto-close");
    }

    private static void verifyHandheldSymbols(int[] logical) {
        var map = new SymbolMap(36);
        var physicalSlots = new java.util.ArrayList<Integer>();
        ProgrammedDialSequence.send(logical, (symbol, last) -> {
            int physical = HandheldDialSymbols.physicalSymbol(map, symbol, logical);
            check(!map.isSymbolOutOfBounds(physical), "ring cannot encode physical symbol " + physical);
            check(last == (physicalSlots.size() == logical.length - 1), "only the final symbol should trigger engagement");
            if (symbol < 36) check(physical == symbol, "remap displaced an in-range route glyph");
            physicalSlots.add(physical);
            return true;
        });
        check(physicalSlots.size() == logical.length, "dial lost symbols");
        for (int i = 0; i < logical.length; i++) {
            check(map.getMappedSymbol(physicalSlots.get(i)) == logical[i],
                    "physical glyph substitution changed the destination");
        }
    }

    private static void verifyHandheldAutoclose() {
        var handheld = java.util.UUID.randomUUID();
        var other = java.util.UUID.randomUUID();
        check(!HandheldAutoclosePolicy.shouldClose(handheld, handheld, false, false, 201, 200),
                "never close before the first crossing");
        check(!HandheldAutoclosePolicy.shouldClose(handheld, handheld, true, false, 200, 200)
                        && HandheldAutoclosePolicy.shouldClose(handheld, handheld, true, false, 201, 200),
                "close only after the native idle threshold");
        check(!HandheldAutoclosePolicy.shouldClose(handheld, handheld, true, true, 201, 200),
                "preserve a Kino hold");
        check(!HandheldAutoclosePolicy.shouldClose(handheld, other, true, false, 201, 200)
                        && !HandheldAutoclosePolicy.shouldClose(null, other, true, false, 201, 200),
                "never apply the handheld policy to a different or ordinary dial");
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

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
