package com.dankscripts.sgjdestiny_dhd.compat;

final class ProgrammedDialSequence {
    private ProgrammedDialSequence() {}

    static void send(int[] symbols, SymbolSender sender) {
        for (int i = 0; i < symbols.length; i++) {
            if (!sender.send(symbols[i], i == symbols.length - 1)) return;
        }
    }

    interface SymbolSender {
        boolean send(int symbol, boolean last);
    }
}
