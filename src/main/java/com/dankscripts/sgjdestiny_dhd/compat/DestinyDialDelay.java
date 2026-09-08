package com.dankscripts.sgjdestiny_dhd.compat;

import com.dankscripts.sgjdestiny_dhd.registry.ModBlocks;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.povstalec.sgjourney.common.block_entities.dhd.AbstractDHDEntity;
import net.povstalec.sgjourney.common.block_entities.dhd.UniverseDHDEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/** Queues dial requests and announces known nine-chevron routes before rotation begins. */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DestinyDialDelay {
    private static final Logger LOGGER = LoggerFactory.getLogger("SGJ Destiny DHD/Dial");
    private static final int PRE_DIAL_TICKS = 3;
    private static final int FIRST_CHEVRON_TIMEOUT_TICKS = 200;
    private static final Map<AbstractDHDEntity, PendingSymbol> PENDING = new IdentityHashMap<>();
    private static final Map<AbstractDHDEntity, PendingAddress> PROGRAMMED_DIALS = new IdentityHashMap<>();
    private static final Map<AbstractDHDEntity, Long> HOLD_UNTIL_GATE_ACTIVITY = new IdentityHashMap<>();
    private static final Set<AbstractDHDEntity> RELEASING = java.util.Collections.newSetFromMap(new IdentityHashMap<>());

    private DestinyDialDelay() {}

    /** Queues a complete seed-database address and pre-lights only known nine-chevron routes. */
    public static boolean queueProgrammedDial(UniverseDHDEntity dhd, int[] symbols) {
        if (!(dhd.getLevel() instanceof ServerLevel level) || !dhd.getAddress().isEmpty()
                || PENDING.containsKey(dhd) || PROGRAMMED_DIALS.containsKey(dhd)) return false;
        // SGJourney stores a seven-chevron galactic address as its six
        // destination symbols. The seventh lock is the dialing gate's point of
        // origin; Destiny uses symbol 0 for its universal origin.
        int[] dialSymbols;
        if (symbols.length == 6) {
            dialSymbols = java.util.Arrays.copyOf(symbols, 7);
            dialSymbols[6] = 0;
        } else {
            dialSymbols = symbols.clone();
        }
        if (dialSymbols.length == 9) DestinyBearingCompat.beginNineChevronDial(level, dhd);
        PROGRAMMED_DIALS.put(dhd, new PendingAddress(dialSymbols, PRE_DIAL_TICKS));
        HOLD_UNTIL_GATE_ACTIVITY.put(dhd, level.getGameTime() + FIRST_CHEVRON_TIMEOUT_TICKS);
        return true;
    }

    /** @return true when the original SGJourney call must be cancelled for now. */
    public static boolean interceptFirstSymbol(AbstractDHDEntity dhd, int symbol) {
        if (RELEASING.contains(dhd) || !(dhd.getLevel() instanceof ServerLevel level)
                || !dhd.getBlockState().is(ModBlocks.DESTINY_DHD.get())) {
            return false;
        }

        // A nine-chevron address is complete as its ninth symbol is submitted.
        // Start the separate discharge effect before SGJourney processes that
        // symbol so it cannot lag behind the gate's own animation.
        DestinyBearingCompat.observeEncodedSymbol(level, dhd, symbol);

        var gate = dhd.stargateCache.get();
        if (gate == null || gate.isConnected() || gate.isDialingOut()
                || gate.getChevronsEngaged() > 0 || !gate.getEncodedSymbols().isEmpty()) {
            return false;
        }

        if (HOLD_UNTIL_GATE_ACTIVITY.containsKey(dhd) && !PENDING.containsKey(dhd)) {
            return false;
        }

        if (!PENDING.containsKey(dhd)) {
            PENDING.put(dhd, new PendingSymbol(symbol, PRE_DIAL_TICKS));
            HOLD_UNTIL_GATE_ACTIVITY.put(dhd, level.getGameTime() + FIRST_CHEVRON_TIMEOUT_TICKS);
            LOGGER.info("[Destiny Dial] first symbol {} queued at {}",
                    symbol, dhd.getBlockPos());
        }
        return true;
    }

    /** Fallback for integrations that populate the address without nine button calls. */
    public static void observeEngage(AbstractDHDEntity dhd) {
        if (!(dhd.getLevel() instanceof ServerLevel level)
                || !dhd.getBlockState().is(ModBlocks.DESTINY_DHD.get())) {
            return;
        }
        DestinyBearingCompat.observeEngage(level, dhd);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || (PENDING.isEmpty() && PROGRAMMED_DIALS.isEmpty())) return;

        Iterator<Map.Entry<AbstractDHDEntity, PendingSymbol>> iterator = PENDING.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<AbstractDHDEntity, PendingSymbol> entry = iterator.next();
            PendingSymbol pending = entry.getValue();
            int ticks = pending.ticks() - 1;
            if (ticks > 0) {
                entry.setValue(new PendingSymbol(pending.symbol(), ticks));
                continue;
            }

            AbstractDHDEntity dhd = entry.getKey();
            iterator.remove();
            if (dhd.isRemoved() || !(dhd.getLevel() instanceof ServerLevel)) continue;
            RELEASING.add(dhd);
            try {
                LOGGER.info("[Destiny Dial] releasing first symbol {} at {}",
                        pending.symbol(), dhd.getBlockPos());
                dhd.encodeSymbol(pending.symbol());
            } finally {
                RELEASING.remove(dhd);
            }
        }

        Iterator<Map.Entry<AbstractDHDEntity, PendingAddress>> dialIterator = PROGRAMMED_DIALS.entrySet().iterator();
        while (dialIterator.hasNext()) {
            Map.Entry<AbstractDHDEntity, PendingAddress> entry = dialIterator.next();
            PendingAddress pending = entry.getValue();
            int ticks = pending.ticks() - 1;
            if (ticks > 0) {
                entry.setValue(new PendingAddress(pending.symbols(), ticks));
                continue;
            }
            AbstractDHDEntity dhd = entry.getKey();
            dialIterator.remove();
            if (dhd.isRemoved() || !(dhd.getLevel() instanceof ServerLevel) || !dhd.getAddress().isEmpty()) continue;
            RELEASING.add(dhd);
            try {
                for (int symbol : pending.symbols()) dhd.encodeSymbol(symbol);
                dhd.engageStargate();
            } finally {
                RELEASING.remove(dhd);
            }
        }
    }

    public static boolean shouldHoldLight(AbstractDHDEntity dhd, long gameTime) {
        Long expiresAt = HOLD_UNTIL_GATE_ACTIVITY.get(dhd);
        if (expiresAt == null) return PENDING.containsKey(dhd);
        if (gameTime <= expiresAt) return true;
        HOLD_UNTIL_GATE_ACTIVITY.remove(dhd);
        return PENDING.containsKey(dhd);
    }

    public static void gateActivityObserved(AbstractDHDEntity dhd) {
        HOLD_UNTIL_GATE_ACTIVITY.remove(dhd);
    }

    public static void cancel(AbstractDHDEntity dhd) {
        PENDING.remove(dhd);
        PROGRAMMED_DIALS.remove(dhd);
        HOLD_UNTIL_GATE_ACTIVITY.remove(dhd);
    }

    private record PendingSymbol(int symbol, int ticks) {}
    private record PendingAddress(int[] symbols, int ticks) {}
}
