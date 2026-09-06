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

/** Delays the first dialing symbol so the optional bearing update reaches clients first. */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DestinyDialDelay {
    private static final Logger LOGGER = LoggerFactory.getLogger("SGJ Destiny DHD/Dial");
    private static final int PRE_DIAL_TICKS = 3;
    private static final int FIRST_CHEVRON_TIMEOUT_TICKS = 200;
    private static final Map<AbstractDHDEntity, PendingSymbol> PENDING = new IdentityHashMap<>();
    private static final Map<AbstractDHDEntity, Long> HOLD_UNTIL_GATE_ACTIVITY = new IdentityHashMap<>();
    private static final Set<AbstractDHDEntity> RELEASING = java.util.Collections.newSetFromMap(new IdentityHashMap<>());

    private DestinyDialDelay() {}

    /** @return true when the original SGJourney call must be cancelled for now. */
    public static boolean interceptFirstSymbol(AbstractDHDEntity dhd, int symbol) {
        if (RELEASING.contains(dhd) || !(dhd.getLevel() instanceof ServerLevel level)
                || !dhd.getBlockState().is(ModBlocks.DESTINY_DHD.get())) {
            return false;
        }

        var gate = dhd.stargateCache.get();
        if (gate == null || gate.isConnected() || gate.isDialingOut()
                || gate.getChevronsEngaged() > 0 || !gate.getEncodedSymbols().isEmpty()) {
            return false;
        }

        if (HOLD_UNTIL_GATE_ACTIVITY.containsKey(dhd) && !PENDING.containsKey(dhd)) {
            return false;
        }

        if (!PENDING.containsKey(dhd)) {
            if (dhd instanceof UniverseDHDEntity universeDhd) {
                DestinyBearingCompat.lightBeforeDial(level, universeDhd);
            }
            PENDING.put(dhd, new PendingSymbol(symbol, PRE_DIAL_TICKS));
            HOLD_UNTIL_GATE_ACTIVITY.put(dhd, level.getGameTime() + FIRST_CHEVRON_TIMEOUT_TICKS);
            LOGGER.info("[Destiny Dial] bearing pre-light queued before first symbol {} at {}",
                    symbol, dhd.getBlockPos());
        }
        return true;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || PENDING.isEmpty()) return;

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
                LOGGER.info("[Destiny Dial] releasing first symbol {} after bearing pre-light at {}",
                        pending.symbol(), dhd.getBlockPos());
                dhd.encodeSymbol(pending.symbol());
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

    private record PendingSymbol(int symbol, int ticks) {}
}
