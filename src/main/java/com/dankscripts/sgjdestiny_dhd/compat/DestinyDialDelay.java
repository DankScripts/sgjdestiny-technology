package com.dankscripts.sgjdestiny_dhd.compat;

import com.dankscripts.sgjdestiny_dhd.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.povstalec.sgjourney.common.block_entities.dhd.AbstractDHDEntity;
import net.povstalec.sgjourney.common.block_entities.dhd.UniverseDHDEntity;
import net.povstalec.sgjourney.common.block_entities.stargate.UniverseStargateEntity;
import net.povstalec.sgjourney.common.init.SoundInit;
import net.povstalec.sgjourney.common.sgjourney.Address;
import net.povstalec.sgjourney.common.sgjourney.StargateInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/** Queues dial requests and announces known nine-chevron routes before rotation begins. */
@EventBusSubscriber(bus = EventBusSubscriber.Bus.GAME)
public final class DestinyDialDelay {
    private static final Logger LOGGER = LoggerFactory.getLogger("SGJ Destiny DHD/Dial");
    private static final int PRE_DIAL_TICKS = 3;
    private static final int FIRST_CHEVRON_TIMEOUT_TICKS = 200;
    private static final Map<AbstractDHDEntity, DialInputBuffer> PENDING = new IdentityHashMap<>();
    private static final Map<AbstractDHDEntity, PendingAddress> PROGRAMMED_DIALS = new IdentityHashMap<>();
    private static final Map<AbstractDHDEntity, Long> HOLD_UNTIL_GATE_ACTIVITY = new IdentityHashMap<>();
    private static final Set<AbstractDHDEntity> RELEASING = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Set<AbstractDHDEntity> PROGRAMMED_DIAL_RELEASING = java.util.Collections.newSetFromMap(new IdentityHashMap<>());

    private DestinyDialDelay() {}

    /** Queues a complete seed-database address and pre-lights only known nine-chevron routes. */
    public static boolean queueProgrammedDial(UniverseDHDEntity dhd, int[] symbols, boolean handheld) {
        if (!(dhd.getLevel() instanceof ServerLevel level) || !dhd.getAddress().isEmpty()
                || PENDING.containsKey(dhd) || PROGRAMMED_DIALS.containsKey(dhd)) return false;
        Address.Immutable dialAddress = Address.Immutable.extendWithPointOfOrigin(new Address.Immutable(symbols));
        if (!dialAddress.canBeDialed()) {
            dhd.sendMessageToNearbyPlayers(Component.translatable("message.sgjdestiny_dhd.invalid_destination"),
                    AbstractDHDEntity.DHD_INFO_DISTANCE);
            return false;
        }
        int[] dialSymbols = dialAddress.toArray();
        DestinyShipInstallEvents.releaseOneSidedShipDHDLink(level, dhd);
        if (dialSymbols.length == 9) DestinyBearingCompat.beginNineChevronDial(level, dhd);
        PROGRAMMED_DIALS.put(dhd, new PendingAddress(dialSymbols, PRE_DIAL_TICKS, handheld));
        HOLD_UNTIL_GATE_ACTIVITY.put(dhd, level.getGameTime() + FIRST_CHEVRON_TIMEOUT_TICKS);
        return true;
    }

    /** @return true when the original SGJourney call must be cancelled for now. */
    public static boolean interceptFirstSymbol(AbstractDHDEntity dhd, int symbol) {
        if (!(dhd.getLevel() instanceof ServerLevel level)
                || !dhd.getBlockState().is(ModBlocks.DESTINY_DHD.get())) {
            return false;
        }

        if (RELEASING.contains(dhd)) {
            DestinyBearingCompat.observeEncodedSymbol(level, dhd, symbol);
            return false;
        }
        if (dhd instanceof UniverseDHDEntity universe)
            DestinyShipInstallEvents.releaseOneSidedShipDHDLink(level, universe);
        DialInputBuffer pending = PENDING.get(dhd);
        if (pending != null) {
            if (!pending.addSymbol(symbol)) reportFullQueue(dhd);
            return true;
        }

        DestinyBearingCompat.observeEncodedSymbol(level, dhd, symbol);

        var gate = DestinyDHDApi.connectedStargateEntity(dhd);
        if (gate == null || gate.isConnected() || gate.isDialingOut()
                || gate.getChevronsEngaged() > 0 || !gate.getEncodedSymbols().isEmpty()) {
            return false;
        }

        if (HOLD_UNTIL_GATE_ACTIVITY.containsKey(dhd)) {
            return false;
        }

        PENDING.put(dhd, new DialInputBuffer(symbol, PRE_DIAL_TICKS));
        HOLD_UNTIL_GATE_ACTIVITY.put(dhd, level.getGameTime() + FIRST_CHEVRON_TIMEOUT_TICKS);
        LOGGER.info("[Destiny Dial] first symbol {} queued at {}", symbol, dhd.getBlockPos());
        return true;
    }

    /** Fallback for integrations that populate the address without nine button calls. */
    public static boolean interceptEngage(AbstractDHDEntity dhd) {
        if (!(dhd.getLevel() instanceof ServerLevel level)
                || !dhd.getBlockState().is(ModBlocks.DESTINY_DHD.get())) {
            return false;
        }
        DialInputBuffer pending = PENDING.get(dhd);
        if (!RELEASING.contains(dhd) && pending != null) {
            if (!pending.addEngage()) reportFullQueue(dhd);
            return true;
        }
        DestinyBearingCompat.observeEngage(level, dhd);
        return false;
    }

    private static void reportFullQueue(AbstractDHDEntity dhd) {
        LOGGER.warn("[Destiny Dial] input queue is full at {}", dhd.getBlockPos());
        dhd.sendMessageToNearbyPlayers(Component.literal("Destiny dialer is busy; please retry."),
                AbstractDHDEntity.DHD_INFO_DISTANCE);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if ((PENDING.isEmpty() && PROGRAMMED_DIALS.isEmpty())) return;

        Iterator<Map.Entry<AbstractDHDEntity, DialInputBuffer>> iterator = PENDING.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<AbstractDHDEntity, DialInputBuffer> entry = iterator.next();
            DialInputBuffer pending = entry.getValue();
            if (!pending.tick()) continue;

            AbstractDHDEntity dhd = entry.getKey();
            iterator.remove();
            if (dhd.isRemoved() || !(dhd.getLevel() instanceof ServerLevel level)
                    || !level.hasChunkAt(dhd.getBlockPos())
                    || level.getBlockEntity(dhd.getBlockPos()) != dhd) continue;
            RELEASING.add(dhd);
            try {
                for (DialInputBuffer.Action action : pending.actions()) {
                    if (action.engage()) {
                        dhd.engageStargate();
                    } else {
                        var feedback = dhd.encodeSymbol(action.symbol());
                        if (feedback.feedback().isError())
                            dhd.sendMessageToNearbyPlayers(feedback.getMessageComponent(), AbstractDHDEntity.DHD_INFO_DISTANCE);
                    }
                }
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
                entry.setValue(new PendingAddress(pending.symbols(), ticks, pending.handheld()));
                continue;
            }
            AbstractDHDEntity dhd = entry.getKey();
            dialIterator.remove();
            if (dhd.isRemoved() || !(dhd.getLevel() instanceof ServerLevel level)
                    || !level.hasChunkAt(dhd.getBlockPos())
                    || level.getBlockEntity(dhd.getBlockPos()) != dhd || !dhd.getAddress().isEmpty()) {
                LOGGER.warn("[Destiny Dial] programmed dial cancelled because the console changed at {}", dhd.getBlockPos());
                continue;
            }
            UniverseStargateEntity handheldGate = null;
            if (pending.handheld()) {
                if (!(DestinyDHDApi.connectedStargateEntity(dhd) instanceof UniverseStargateEntity universeGate)) {
                    dhd.sendMessageToNearbyPlayers(Component.translatable("message.sgjourney.dhd.error.not_connected_to_stargate"),
                            AbstractDHDEntity.DHD_INFO_DISTANCE);
                    continue;
                }
                if (universeGate.isConnected() || universeGate.isDialingOut() || universeGate.isDialing()
                        || universeGate.isRotating() || universeGate.getChevronsEngaged() > 0) {
                    dhd.sendMessageToNearbyPlayers(Component.translatable("message.sgjdestiny_dhd.dialer_busy"),
                            AbstractDHDEntity.DHD_INFO_DISTANCE);
                    continue;
                }
                handheldGate = universeGate;
            }
            final UniverseStargateEntity handheldController = handheldGate;
            RELEASING.add(dhd);
            PROGRAMMED_DIAL_RELEASING.add(dhd);
            try {
                ProgrammedDialSequence.send(pending.symbols(), (symbol, last) -> {
                    StargateInfo.FeedbackMessage feedback;
                    if (last || pending.handheld()) {
                        var adapter = dhd.getDHD();
                        if (adapter == null) {
                            LOGGER.warn("[Destiny Dial] console has no DHD adapter at {}", dhd.getBlockPos());
                            dhd.sendMessageToNearbyPlayers(Component.translatable("message.sgjourney.dhd.error.not_connected_to_stargate"),
                                    AbstractDHDEntity.DHD_INFO_DISTANCE);
                            return false;
                        }
                        String before = dhd instanceof UniverseDHDEntity universe
                                ? DestinyShipInstallEvents.shipDHDLinkSnapshot(level, universe) : "not Universe DHD";
                        adapter.receiverCache().markDirtyTwoWays();
                        var gate = adapter.getStargate();
                        if (gate == null || (handheldController != null && handheldController.getStargate() != gate)) {
                            String after = dhd instanceof UniverseDHDEntity universe
                                    ? DestinyShipInstallEvents.shipDHDLinkSnapshot(level, universe) : "not Universe DHD";
                            LOGGER.warn("[Destiny Dial] console lost its Stargate at {}; before: {}; after: {}",
                                    dhd.getBlockPos(), before, after);
                            dhd.sendMessageToNearbyPlayers(Component.translatable("message.sgjourney.dhd.error.not_connected_to_stargate"),
                                    AbstractDHDEntity.DHD_INFO_DISTANCE);
                            return false;
                        }
                        DestinyBearingCompat.observeEncodedSymbol(level, dhd, symbol);
                        int encodedSymbol = handheldController != null
                                ? HandheldDialSymbols.physicalSymbol(handheldController.symbolMap, symbol, pending.symbols())
                                : adapter.advancedProtocolsActive() && !gate.getSymbolMap().isSymbolMapped(symbol)
                                ? gate.getSymbolMap().remapToRandomSymbol(symbol, gate.getAddress().toArray()) : symbol;
                        if (handheldController != null && last) {
                            feedback = HandheldAutoclose.engage(handheldController,
                                    () -> gate.dhdEngageSymbol(adapter, encodedSymbol, true, StargateInfo.ChevronSound.ENGAGE));
                        } else {
                            feedback = gate.dhdEngageSymbol(adapter, encodedSymbol, last, StargateInfo.ChevronSound.ENGAGE);
                        }
                        gate.updateDHD(adapter);
                    } else {
                        feedback = dhd.encodeSymbol(symbol);
                    }
                    if (feedback.feedback().isError()) {
                        if (handheldController != null) handheldController.resetStargate(feedback);
                        LOGGER.warn("[Destiny Dial] programmed dial stopped at symbol {}: {}", symbol, feedback);
                        dhd.sendMessageToNearbyPlayers(feedback.getMessageComponent(), AbstractDHDEntity.DHD_INFO_DISTANCE);
                        return false;
                    }
                    return true;
                });
            } finally {
                PROGRAMMED_DIAL_RELEASING.remove(dhd);
                RELEASING.remove(dhd);
            }
        }
    }

    @SubscribeEvent
    public static void onProgrammedDialSound(PlayLevelSoundEvent.AtPosition event) {
        if (PROGRAMMED_DIAL_RELEASING.isEmpty() || event.getSound() == null) return;
        SoundEvent sound = event.getSound().value();
        if (sound != SoundInit.UNIVERSE_DHD_PRESS.get() && sound != SoundInit.UNIVERSE_DHD_ENTER.get()) return;

        BlockPos soundPos = BlockPos.containing(event.getPosition());
        for (AbstractDHDEntity dhd : PROGRAMMED_DIAL_RELEASING) {
            if (event.getLevel() == dhd.getLevel() && soundPos.equals(dhd.getBlockPos())) {
                // Selecting a stored address is not a series of physical DHD button presses.
                event.setCanceled(true);
                return;
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

    private record PendingAddress(int[] symbols, int ticks, boolean handheld) {}
}
