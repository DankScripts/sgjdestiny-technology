package com.dankscripts.sgjdestiny_dhd.compat;

import net.povstalec.sgjourney.common.sgjourney.Address;
import net.povstalec.sgjourney.common.sgjourney.SymbolMap;

public final class HandheldDialSymbolsTest {
    public static void main(String[] args) {
        Address.Immutable sixSymbols = new Address.Immutable(1, 8, 15, 21, 30, 35);
        Address.Immutable withOrigin = Address.Immutable.extendWithPointOfOrigin(sixSymbols);
        check(withOrigin.toArray().length == 7 && withOrigin.toArray()[6] == 0,
                "short destinations must receive the Destiny point of origin");

        Address.Immutable explicitOrigin = new Address.Immutable(1, 8, 15, 21, 30, 35, 0);
        check(Address.Immutable.extendWithPointOfOrigin(explicitOrigin).equals(explicitOrigin),
                "an explicit point of origin must not be duplicated");

        Address.Immutable fullAddress = new Address.Immutable(1, 8, 15, 21, 30, 35, 2, 9, 14);
        check(Address.Immutable.extendWithPointOfOrigin(fullAddress).equals(fullAddress),
                "a native nine-symbol address must not be extended");

        SymbolMap map = new SymbolMap(36);
        int[] sequence = {36, 37, 8, 0};
        int firstPhysical = HandheldDialSymbols.physicalSymbol(map, sequence[0], sequence);
        int secondPhysical = HandheldDialSymbols.physicalSymbol(map, sequence[1], sequence);
        check(firstPhysical >= 0 && firstPhysical < 36 && map.getMappedSymbol(firstPhysical) == 36,
                "logical symbol 36 must be encoded through a valid physical input");
        check(secondPhysical >= 0 && secondPhysical < 36 && map.getMappedSymbol(secondPhysical) == 37,
                "a second out-of-range glyph must retain its logical identity");
        check(firstPhysical != 8 && secondPhysical != 8 && map.getMappedSymbol(8) == 8,
                "remapping must not consume a glyph needed by a later chevron");
        System.out.println("PASS: handheld point-of-origin handling and Universe symbol remapping");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
