package net.jr.client.compat.xaero;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.client.LocalClient;
import net.jr.client.runtime.context.LocalClientExecution;
import net.jr.client.runtime.context.SlotScope;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.jetbrains.annotations.Nullable;
import xaero.map.MapRunner;
import xaero.map.WorldMapSession;
import xaero.map.core.XaeroWorldMapCore;
import xaero.map.core.IWorldMapClientPlayNetHandler;

public final class XaeroWorldMapRuntime {

    private static final ConcurrentMap<ClientPacketListener, SessionBinding> SESSIONS =
            new ConcurrentHashMap<>();

    private static final ConcurrentMap<Integer, XaeroWorldMapViewState> VIEW_STATES =
            new ConcurrentHashMap<>();

    /*
     * Xaero World Map keeps a single static "currentSession".  That is fine
     * for vanilla, but joining a local secondary player used to make Xaero
     * clean the primary player's session.  Keep the authoritative session per
     * Java Reforged slot and temporarily project it into Xaero's static field
     * only while its connection lifecycle code is executing.
     */
    private static final ConcurrentMap<Integer, WorldMapSession> CORE_SESSIONS =
            new ConcurrentHashMap<>();

    private static final ThreadLocal<Integer> CORE_LIFECYCLE_SLOT = new ThreadLocal<>();

    private XaeroWorldMapRuntime() {
    }

    public static void register(ClientPacketListener listener) {
        WorldMapSession session =
                ((IWorldMapClientPlayNetHandler) listener).getXaero_worldmapSession();

        if (session == null) {
            return;
        }

        int slotId =
                ClientRuntime.INSTANCE.clients().slotForClientPacketListener(listener);

        SESSIONS.entrySet().removeIf(entry ->
                entry.getKey() != listener && entry.getValue().slotId() == slotId
        );

        SESSIONS.put(
                listener,
                new SessionBinding(slotId, session)
        );
        CORE_SESSIONS.put(slotId, session);

        /*
         * Ensure the slot has a persistent visual state as soon as
         * Xaero registers its client session.
         */
        VIEW_STATES.computeIfAbsent(
                slotId,
                ignored -> new XaeroWorldMapViewState()
        );
    }

    public static void unregister(ClientPacketListener listener) {
        SessionBinding removed = SESSIONS.remove(listener);

        if (removed == null) {
            return;
        }

        /*
         * A reconnect can replace a ClientPacketListener while keeping
         * the same Java Reforged slot alive.
         *
         * Only remove the visual state if no remaining Xaero session
         * still belongs to this slot.
         */
        int slotId = removed.slotId();

        CORE_SESSIONS.remove(slotId, removed.session());

        boolean slotStillRegistered =
                SESSIONS.values()
                        .stream()
                        .anyMatch(binding -> binding.slotId() == slotId);

        if (!slotStillRegistered) {
            VIEW_STATES.remove(slotId);
        }
    }

    public static boolean hasSessions() {
        return !SESSIONS.isEmpty();
    }

    @Nullable
    public static WorldMapSession currentSessionOrNull() {
        Integer slotId = SlotScope.idOrNull();
        if (slotId != null && slotId.equals(CORE_LIFECYCLE_SLOT.get())) {
            return XaeroWorldMapCore.currentSession;
        }
        return slotId != null ? sessionForSlotOrNull(slotId) : null;
    }

    public static CoreSessionScope bindCoreSessionForJoin(ClientPacketListener listener) {
        int slotId = ClientRuntime.INSTANCE.clients().slotForClientPacketListener(listener);
        return bindCoreSession(slotId, sessionForListenerOrNull(listener));
    }

    public static CoreSessionScope bindCoreSessionForCleanup(ClientPacketListener listener) {
        int slotId = ClientRuntime.INSTANCE.clients().slotForClientPacketListener(listener);
        return bindCoreSession(slotId, sessionForListenerOrNull(listener));
    }

    private static CoreSessionScope bindCoreSession(int slotId, @Nullable WorldMapSession session) {
        WorldMapSession previous = XaeroWorldMapCore.currentSession;
        Integer previousLifecycleSlot = CORE_LIFECYCLE_SLOT.get();
        XaeroWorldMapCore.currentSession = session;
        CORE_LIFECYCLE_SLOT.set(slotId);
        return new CoreSessionScope(
                slotId,
                previous,
                previousLifecycleSlot,
                SlotScope.enter(slotId)
        );
    }

    @Nullable
    private static WorldMapSession sessionForListenerOrNull(ClientPacketListener listener) {
        SessionBinding binding = SESSIONS.get(listener);
        return binding != null ? binding.session() : null;
    }

    public static XaeroWorldMapViewState viewState(int slotId) {
        return VIEW_STATES.computeIfAbsent(
                slotId,
                ignored -> new XaeroWorldMapViewState()
        );
    }

    @Nullable
    public static XaeroWorldMapViewState currentViewStateOrNull() {
        Integer slotId = SlotScope.idOrNull();

        if (slotId == null) {
            return null;
        }

        return viewState(slotId);
    }

    public static boolean runProcessors(MapRunner runner) {
        boolean[] ran = {false};

        for (SessionBinding binding : SESSIONS.values()) {
            LocalClient client =
                    ClientRuntime.INSTANCE.clients()
                            .clientOrNull(binding.slotId());

            WorldMapSession session =
                    binding.session();

            if (client == null || !session.isUsable()) {
                continue;
            }

            ran[0] = true;

            LocalClientExecution.runForClient(
                    binding.slotId(),
                    () -> session.getMapProcessor().run(runner)
            );
        }

        return ran[0];
    }

    public static void runEveryClient(Runnable action) {
        for (LocalClient client :
                ClientRuntime.INSTANCE.clients().all()) {

            if (client.player() != null) {
                LocalClientExecution.runForClient(
                        client.slotId(),
                        action
                );
            }
        }
    }

    public static void runForPlayer(
            PlayerAction action,
            net.minecraft.world.entity.player.Player player
    ) {
        for (LocalClient client :
                ClientRuntime.INSTANCE.clients().all()) {

            if (client.player() == player) {
                LocalClientExecution.runForClient(
                        client.slotId(),
                        () -> action.run(player)
                );
                return;
            }
        }

        action.run(player);
    }

    @FunctionalInterface
    public interface PlayerAction {
        void run(net.minecraft.world.entity.player.Player player);
    }

    private record SessionBinding(
            int slotId,
            WorldMapSession session
    ) {
    }

    @Nullable
    private static WorldMapSession sessionForSlotOrNull(int slotId) {
        WorldMapSession session = CORE_SESSIONS.get(slotId);
        if (session != null) {
            return session;
        }

        return SESSIONS.values()
                .stream()
                .filter(binding -> binding.slotId() == slotId)
                .map(SessionBinding::session)
                .findFirst()
                .orElse(null);
    }

    public static final class CoreSessionScope implements AutoCloseable {
        private final int slotId;
        @Nullable
        private final WorldMapSession previous;
        @Nullable
        private final Integer previousLifecycleSlot;
        private final SlotScope.Scope activeSlotScope;
        private boolean closed;

        private CoreSessionScope(
            int slotId,
            @Nullable WorldMapSession previous,
            @Nullable Integer previousLifecycleSlot,
            SlotScope.Scope activeSlotScope
        ) {
            this.slotId = slotId;
            this.previous = previous;
            this.previousLifecycleSlot = previousLifecycleSlot;
            this.activeSlotScope = activeSlotScope;
        }

        @Override
        public void close() {
            if (this.closed) {
                return;
            }
            this.closed = true;

            try {
                WorldMapSession active = XaeroWorldMapCore.currentSession;
                if (active == null) {
                    CORE_SESSIONS.remove(this.slotId);
                } else {
                    CORE_SESSIONS.put(this.slotId, active);
                }

                WorldMapSession primarySession = sessionForSlotOrNull(0);
                XaeroWorldMapCore.currentSession =
                        primarySession != null ? primarySession : this.previous;
            } finally {
                try {
                    this.activeSlotScope.close();
                } finally {
                    if (this.previousLifecycleSlot == null) {
                        CORE_LIFECYCLE_SLOT.remove();
                    } else {
                        CORE_LIFECYCLE_SLOT.set(this.previousLifecycleSlot);
                    }
                }
            }
        }
    }

    public static boolean shouldSkipWorldRender(int slotId) {
        XaeroWorldMapViewState state = VIEW_STATES.get(slotId);
        return state != null && state.skipWorldRender();
    }
}
