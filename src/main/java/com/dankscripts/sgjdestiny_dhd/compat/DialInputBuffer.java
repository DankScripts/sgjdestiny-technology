package com.dankscripts.sgjdestiny_dhd.compat;

import java.util.ArrayList;
import java.util.List;

final class DialInputBuffer {
    private static final int MAX_ACTIONS = 16;
    private final List<Action> actions = new ArrayList<>();
    private int ticksRemaining;

    DialInputBuffer(int firstSymbol, int ticks) {
        actions.add(new Action(false, firstSymbol));
        ticksRemaining = ticks;
    }

    boolean addSymbol(int symbol) {
        return add(new Action(false, symbol));
    }

    boolean addEngage() {
        return add(new Action(true, 0));
    }

    private boolean add(Action action) {
        if (actions.size() >= MAX_ACTIONS) return false;
        actions.add(action);
        return true;
    }

    boolean tick() {
        return --ticksRemaining <= 0;
    }

    List<Action> actions() {
        return List.copyOf(actions);
    }

    record Action(boolean engage, int symbol) {}
}
