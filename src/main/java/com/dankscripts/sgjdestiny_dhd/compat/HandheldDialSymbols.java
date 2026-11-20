package com.dankscripts.sgjdestiny_dhd.compat;

import net.povstalec.sgjourney.common.sgjourney.SymbolMap;

public final class HandheldDialSymbols {
    private HandheldDialSymbols() {}

    public static int physicalSymbol(SymbolMap map, int logicalSymbol, int[] address) {
        if (map.isSymbolMapped(logicalSymbol)) return map.getOriginalSymbol(logicalSymbol);
        // Do not repurpose a glyph needed by a later chevron of this address.
        return map.remapToRandomSymbol(logicalSymbol, address);
    }
}
