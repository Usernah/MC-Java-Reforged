package net.jr.server.chunk;

import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.WeakHashMap;
import net.minecraft.core.SectionPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

/**
 * Legacy-style incremental player-loading map.
 *
 * <p>Vanilla's PlayerTicketTracker receives a new player centre and expands the
 * complete area immediately. This coordinator keeps that tracker without sources,
 * preserves Vanilla's bootstrap path, and thereafter computes only the entered and
 * exited border. A border request is admitted only when its player receives its
 * turn for the server tick.</p>
 */
public final class LegacyChunkAdmissionLimiter {
    private static final int MAX_LOCAL_PLAYERS_PER_TICK = 4;
    private static final int SAFETY_RADIUS = 3;
    private static final int SAFETY_CONE_LENGTH = 5;
    private static final byte NONE = 0;
    private static final byte PENDING = 1;
    private static final byte SUBMITTED = 2;
    private static final byte ACTIVE = 3;
    private static final byte REMOVING = 4;

    private static final Map<DistanceManager, State> STATES = new WeakHashMap<>();
    private static final ThreadLocal<BootstrapContext> BOOTSTRAP_CONTEXT = new ThreadLocal<>();

    private static MinecraftServer activeServer;

    private static int admittedThisTick;
    private static int addedThisTick;
    private static int bootstrapThisTick;
    private static int pendingRequests;
    private static int lastAdmitted;
    private static int lastAdded;
    private static int lastBootstrap;
    private static long totalAdmitted;
    private static long totalAdded;
    private static long totalBootstrap;
    private static int nextPlayerIndex;

    private LegacyChunkAdmissionLimiter() {
    }

    /** Called from ChunkMap only for the initial player-entry path. */
    public static void beginBootstrap(ServerPlayer player, int viewDistance) {
        BOOTSTRAP_CONTEXT.set(new BootstrapContext(player, viewDistance));
    }

    public static void clearBootstrap() {
        BOOTSTRAP_CONTEXT.remove();
    }

    /** Replaces the player-ticket source update inside DistanceManager.addPlayer. */
    public static void addPlayer(NativeTicketBridge bridge, SectionPos position, ServerPlayer player) {
        List<Action> actions = new ArrayList<>();
        synchronized (LegacyChunkAdmissionLimiter.class) {
            State state = STATES.computeIfAbsent(bridge.javaReforged$distanceManager(), State::new);
            state.bridge = bridge;
            if (state.players.isEmpty()) {
                state.simulationDistance = Math.max(0, bridge.javaReforged$simulationDistance());
            }
            BootstrapContext bootstrap = BOOTSTRAP_CONTEXT.get();
            boolean initial = bootstrap != null && bootstrap.player == player;
            int radius = initial ? bootstrap.viewDistance : state.viewDistance;
            if (initial) {
                BOOTSTRAP_CONTEXT.remove();
                state.viewDistance = radius;
            }

            PlayerState playerState = state.players.get(player);
            if (playerState == null) {
                playerState = new PlayerState(state, player);
                state.players.put(player, playerState);
                updateFootprint(state, playerState, position.x(), position.z(), radius, initial, actions);
            } else {
                playerState.pendingRemoval = false;
                updateFootprint(state, playerState, position.x(), position.z(), radius, false, actions);
            }
        }
        run(actions);
    }

    /** Delays actual removal for one tick so ChunkMap.move becomes an in-place move. */
    public static synchronized void removePlayer(NativeTicketBridge bridge, SectionPos position, ServerPlayer player) {
        State state = STATES.get(bridge.javaReforged$distanceManager());
        if (state == null) {
            return;
        }

        PlayerState playerState = state.players.get(player);
        if (playerState != null) {
            playerState.pendingRemoval = true;
        }
    }

    /** Mirrors a runtime view-distance change without recreating the whole tracker graph. */
    public static void updateViewDistance(NativeTicketBridge bridge, int viewDistance) {
        List<Action> actions = new ArrayList<>();
        synchronized (LegacyChunkAdmissionLimiter.class) {
            State state = STATES.computeIfAbsent(bridge.javaReforged$distanceManager(), State::new);
            state.bridge = bridge;
            if (state.viewDistance == viewDistance) {
                return;
            }

            state.viewDistance = viewDistance;
            for (PlayerState player : state.players.values()) {
                if (!player.pendingRemoval) {
                    updateFootprint(state, player, player.centerX, player.centerZ, viewDistance, false, actions);
                }
            }
        }
        run(actions);
    }

    /**
     * Keeps Vanilla's simulation radius, but only over chunks already admitted
     * by the controlled player-loading path.  Modern Vanilla normally creates a
     * PLAYER_SIMULATION source directly under every moving player; that source
     * propagates through an entire radius and can generate terrain outside the
     * player-loading queue.
     */
    public static void updateSimulationDistance(NativeTicketBridge bridge, int simulationDistance) {
        List<Action> actions = new ArrayList<>();
        synchronized (LegacyChunkAdmissionLimiter.class) {
            State state = STATES.computeIfAbsent(bridge.javaReforged$distanceManager(), State::new);
            state.bridge = bridge;
            int clampedDistance = Math.max(0, simulationDistance);
            if (state.simulationDistance == clampedDistance) {
                return;
            }

            state.simulationDistance = clampedDistance;
            for (PlayerState player : state.players.values()) {
                if (!player.pendingRemoval) {
                    updateSimulationFootprint(state, player, player.centerX, player.centerZ, actions);
                }
            }
        }
        run(actions);
    }

    /** The native PlayerTicketTracker source call is intentionally suppressed. */
    public static void suppressVanillaPlayerTicketSource() {
    }

    /** Starts a server tick; requests are selected after movement has finished. */
    public static synchronized void beginServerTick(MinecraftServer server) {
        if (activeServer == null) {
            activeServer = server;
        } else if (activeServer != server) {
            resetSessionState();
            activeServer = server;
        }

        lastAdmitted = admittedThisTick;
        lastAdded = addedThisTick;
        lastBootstrap = bootstrapThisTick;
        admittedThisTick = 0;
        addedThisTick = 0;
        bootstrapThisTick = 0;
    }

    /**
     * Drops every reference and unfinished round owned by a server that has
     * finished closing. A DistanceManager cannot identify a later world even
     * when both worlds live in the same client process, so none of this state
     * may cross the server-session boundary.
     */
    public static synchronized void onServerStopped(MinecraftServer server) {
        if (activeServer == server) {
            resetSessionState();
            activeServer = null;
        }
    }

    /**
     * Applies disconnects, then chooses one nearest border request per player,
     * capped at the four local split-screen slots. This is Legacy's tickAddRequests
     * behaviour rather than a generic worldgen throttle.
     */
    public static void dispatchQueuedTickets() {
        List<Action> actions = new ArrayList<>();
        synchronized (LegacyChunkAdmissionLimiter.class) {
            for (State state : STATES.values()) {
                finalizeRemovals(state, actions);
                promoteSafetyRequests(state, actions);
            }

            List<PlayerState> candidates = new ArrayList<>();
            for (State state : STATES.values()) {
                for (PlayerState player : state.players.values()) {
                    if (!player.pendingRemoval && !player.requests.isEmpty()) {
                        candidates.add(player);
                    }
                }
            }

            int count = Math.min(MAX_LOCAL_PLAYERS_PER_TICK, candidates.size());
            if (!candidates.isEmpty()) {
                int start = Math.floorMod(nextPlayerIndex, candidates.size());
                for (int offset = 0; offset < candidates.size() && count > 0; offset++) {
                    PlayerState player = candidates.get((start + offset) % candidates.size());
                    Request request = pollCurrentRequest(player.state, player);
                    if (request == null) {
                        continue;
                    }

                    player.state.phase.put(request.chunkPos, SUBMITTED);
                    pendingRequests--;
                    admittedThisTick++;
                    totalAdmitted++;
                    actions.add(Action.submit(
                        player.state.bridge,
                        request.chunkPos,
                        Math.max(1, priority(player, request.chunkPos))
                    ));
                    count--;
                    nextPlayerIndex = start + offset + 1;
                }
            }
        }
        run(actions);
    }

    /** Called by the native dispatcher immediately before the ticket is inserted. */
    public static synchronized boolean claimSubmittedTicket(NativeTicketBridge bridge, long chunkPos) {
        State state = STATES.get(bridge.javaReforged$distanceManager());
        if (state == null || state.desiredCounts.get(chunkPos) <= 0 || state.phase.get(chunkPos) != SUBMITTED) {
            return false;
        }

        state.phase.put(chunkPos, ACTIVE);
        return true;
    }

    /** Called after the controlled player-loading ticket is present in TicketStorage. */
    public static void onPlayerLoadingTicketInserted(NativeTicketBridge bridge, long chunkPos) {
        List<Action> actions = new ArrayList<>(1);
        synchronized (LegacyChunkAdmissionLimiter.class) {
            State state = STATES.get(bridge.javaReforged$distanceManager());
            if (state != null && state.phase.get(chunkPos) == ACTIVE) {
                issueSimulationIfReady(state, chunkPos, actions);
            }
        }
        run(actions);
    }

    /** Called after the native dispatcher removes the ticket or cancels its submission. */
    public static void onNativeTicketReleased(NativeTicketBridge bridge, long chunkPos) {
        List<Action> actions = new ArrayList<>(1);
        synchronized (LegacyChunkAdmissionLimiter.class) {
            State state = STATES.get(bridge.javaReforged$distanceManager());
            if (state == null) {
                return;
            }

            releaseSimulationIfIssued(state, chunkPos, actions);
            state.phase.put(chunkPos, NONE);
            if (state.desiredCounts.get(chunkPos) > 0) {
                PlayerState owner = findOwner(state, chunkPos);
                if (owner != null) {
                    enqueueRequest(state, owner, chunkPos, priority(owner, chunkPos));
                }
            }
        }
        run(actions);
    }

    public static synchronized Snapshot snapshot() {
        return new Snapshot(
            MAX_LOCAL_PLAYERS_PER_TICK,
            addedThisTick,
            admittedThisTick,
            bootstrapThisTick,
            pendingRequests,
            lastAdded,
            lastAdmitted,
            lastBootstrap,
            totalAdded,
            totalAdmitted,
            totalBootstrap
        );
    }

    private static void resetSessionState() {
        STATES.clear();
        BOOTSTRAP_CONTEXT.remove();
        admittedThisTick = 0;
        addedThisTick = 0;
        bootstrapThisTick = 0;
        pendingRequests = 0;
        lastAdmitted = 0;
        lastAdded = 0;
        lastBootstrap = 0;
        totalAdmitted = 0L;
        totalAdded = 0L;
        totalBootstrap = 0L;
        nextPlayerIndex = 0;
    }

    private static void updateFootprint(
        State state,
        PlayerState player,
        int newCenterX,
        int newCenterZ,
        int radius,
        boolean bootstrap,
        List<Action> actions
    ) {
        if (!player.initialized) {
            player.initialized = true;
            player.centerX = newCenterX;
            player.centerZ = newCenterZ;
            player.radius = radius;
            updateSimulationFootprint(state, player, newCenterX, newCenterZ, actions);
            forEachSquare(newCenterX, newCenterZ, radius, (x, z) -> addDesired(state, player, ChunkPos.pack(x, z), bootstrap, actions));
            return;
        }

        int oldCenterX = player.centerX;
        int oldCenterZ = player.centerZ;
        int oldRadius = player.radius;
        updateSimulationFootprint(state, player, newCenterX, newCenterZ, actions);
        forEachSquare(oldCenterX, oldCenterZ, oldRadius, (x, z) -> {
            if (!inside(x, z, newCenterX, newCenterZ, radius)) {
                removeDesired(state, player, ChunkPos.pack(x, z), actions);
            }
        });
        forEachSquare(newCenterX, newCenterZ, radius, (x, z) -> {
            if (!inside(x, z, oldCenterX, oldCenterZ, oldRadius)) {
                addDesired(state, player, ChunkPos.pack(x, z), false, actions);
            }
        });

        player.centerX = newCenterX;
        player.centerZ = newCenterZ;
        player.radius = radius;
    }

    private static void addDesired(State state, PlayerState player, long chunkPos, boolean bootstrap, List<Action> actions) {
        if (!player.positions.add(chunkPos)) {
            return;
        }

        int previous = state.desiredCounts.get(chunkPos);
        state.desiredCounts.put(chunkPos, previous + 1);
        if (previous != 0) {
            return;
        }

        enqueueRequest(state, player, chunkPos, priority(player, chunkPos));
        if (bootstrap) {
            bootstrapThisTick++;
            totalBootstrap++;
        }
    }

    private static void removeDesired(State state, PlayerState player, long chunkPos, List<Action> actions) {
        if (!player.positions.remove(chunkPos)) {
            return;
        }

        int remaining = state.desiredCounts.get(chunkPos) - 1;
        if (remaining > 0) {
            state.desiredCounts.put(chunkPos, remaining);
            return;
        }
        state.desiredCounts.remove(chunkPos);
        releaseSimulationIfIssued(state, chunkPos, actions);

        switch (state.phase.get(chunkPos)) {
            case PENDING -> cancelPendingRequest(state, chunkPos);
            case SUBMITTED, ACTIVE -> {
                state.phase.put(chunkPos, REMOVING);
                actions.add(Action.release(state.bridge, chunkPos));
            }
            default -> {
            }
        }
    }

    private static void enqueueRequest(State state, PlayerState player, long chunkPos, int priority) {
        if (state.phase.get(chunkPos) != NONE) {
            return;
        }

        Request request = new Request(player, chunkPos, priority, state.nextSequence++);
        state.pendingByChunk.put(chunkPos, request);
        player.requests.add(request);
        state.phase.put(chunkPos, PENDING);
        pendingRequests++;
        addedThisTick++;
        totalAdded++;
    }

    private static void cancelPendingRequest(State state, long chunkPos) {
        Request request = state.pendingByChunk.remove(chunkPos);
        if (request != null) {
            request.owner.requests.remove(request);
            pendingRequests--;
        }
        state.phase.put(chunkPos, NONE);
    }

    private static Request pollCurrentRequest(State state, PlayerState player) {
        Request nearest = null;
        double nearestDistanceSquared = Double.MAX_VALUE;
        Iterator<Request> iterator = player.requests.iterator();
        while (iterator.hasNext()) {
            Request request = iterator.next();
            if (state.pendingByChunk.get(request.chunkPos) != request || state.desiredCounts.get(request.chunkPos) <= 0) {
                iterator.remove();
                continue;
            }

            double distanceSquared = distanceSquaredToChunkCenter(player.player, request.chunkPos);
            if (nearest == null
                || distanceSquared < nearestDistanceSquared
                || distanceSquared == nearestDistanceSquared && request.sequence < nearest.sequence) {
                nearest = request;
                nearestDistanceSquared = distanceSquared;
            }
        }

        if (nearest == null) {
            return null;
        }

        player.requests.remove(nearest);
        return state.pendingByChunk.remove(nearest.chunkPos) == nearest ? nearest : null;
    }

    private static void promoteSafetyRequests(State state, List<Action> actions) {
        for (PlayerState player : state.players.values()) {
            if (player.pendingRemoval) {
                continue;
            }
            Vec3 look = player.player.getLookAngle();
            double horizontalLength = Math.sqrt(look.x * look.x + look.z * look.z);
            double forwardX = horizontalLength > 1.0E-5 ? look.x / horizontalLength : 0.0;
            double forwardZ = horizontalLength > 1.0E-5 ? look.z / horizontalLength : 0.0;
            for (int offsetX = -SAFETY_CONE_LENGTH; offsetX <= SAFETY_CONE_LENGTH; offsetX++) {
                for (int offsetZ = -SAFETY_CONE_LENGTH; offsetZ <= SAFETY_CONE_LENGTH; offsetZ++) {
                    if (!insideSafetyArea(offsetX, offsetZ, forwardX, forwardZ)) {
                        continue;
                    }
                    long chunkPos = ChunkPos.pack(player.centerX + offsetX, player.centerZ + offsetZ);
                    Request request = state.pendingByChunk.get(chunkPos);
                    if (request == null
                        || state.phase.get(chunkPos) != PENDING
                        || state.desiredCounts.get(chunkPos) <= 0
                        || state.pendingByChunk.remove(chunkPos) != request) {
                        continue;
                    }
                    request.owner.requests.remove(request);
                    state.phase.put(chunkPos, SUBMITTED);
                    pendingRequests--;
                    admittedThisTick++;
                    totalAdmitted++;
                    actions.add(Action.submit(state.bridge, chunkPos, 0));
                }
            }
        }
    }

    private static boolean insideSafetyArea(
        int offsetX,
        int offsetZ,
        double forwardX,
        double forwardZ
    ) {
        int distanceSquared = offsetX * offsetX + offsetZ * offsetZ;
        if (distanceSquared <= SAFETY_RADIUS * SAFETY_RADIUS) {
            return true;
        }
        if (distanceSquared > SAFETY_CONE_LENGTH * SAFETY_CONE_LENGTH) {
            return false;
        }
        double forwardDistance = offsetX * forwardX + offsetZ * forwardZ;
        double sideDistance = Math.abs(offsetX * forwardZ - offsetZ * forwardX);
        return forwardDistance > 0.0 && sideDistance <= forwardDistance;
    }

    /** Matches Legacy: choose again from the player's current position each round. */
    private static double distanceSquaredToChunkCenter(ServerPlayer player, long chunkPos) {
        double deltaX = ChunkPos.getX(chunkPos) * 16.0 + 8.0 - player.getX();
        double deltaZ = ChunkPos.getZ(chunkPos) * 16.0 + 8.0 - player.getZ();
        return deltaX * deltaX + deltaZ * deltaZ;
    }

    private static void updateSimulationFootprint(
        State state,
        PlayerState player,
        int newCenterX,
        int newCenterZ,
        List<Action> actions
    ) {
        int oldCenterX = player.simulationCenterX;
        int oldCenterZ = player.simulationCenterZ;
        int oldRadius = player.simulationRadius;
        int newRadius = state.simulationDistance;

        if (!player.simulationInitialized) {
            player.simulationInitialized = true;
            player.simulationCenterX = newCenterX;
            player.simulationCenterZ = newCenterZ;
            player.simulationRadius = newRadius;
            forEachSquare(newCenterX, newCenterZ, newRadius,
                (x, z) -> addSimulationDesired(state, player, ChunkPos.pack(x, z), actions));
            return;
        }

        forEachSquare(oldCenterX, oldCenterZ, oldRadius, (x, z) -> {
            if (!inside(x, z, newCenterX, newCenterZ, newRadius)) {
                removeSimulationDesired(state, player, ChunkPos.pack(x, z), actions);
            }
        });
        forEachSquare(newCenterX, newCenterZ, newRadius, (x, z) -> {
            if (!inside(x, z, oldCenterX, oldCenterZ, oldRadius)) {
                addSimulationDesired(state, player, ChunkPos.pack(x, z), actions);
            }
        });

        player.simulationCenterX = newCenterX;
        player.simulationCenterZ = newCenterZ;
        player.simulationRadius = newRadius;
    }

    private static void addSimulationDesired(State state, PlayerState player, long chunkPos, List<Action> actions) {
        if (!player.simulationPositions.add(chunkPos)) {
            return;
        }

        int previous = state.simulationCounts.get(chunkPos);
        state.simulationCounts.put(chunkPos, previous + 1);
        if (previous == 0) {
            issueSimulationIfReady(state, chunkPos, actions);
        }
    }

    private static void removeSimulationDesired(State state, PlayerState player, long chunkPos, List<Action> actions) {
        if (!player.simulationPositions.remove(chunkPos)) {
            return;
        }

        int remaining = state.simulationCounts.get(chunkPos) - 1;
        if (remaining > 0) {
            state.simulationCounts.put(chunkPos, remaining);
        } else {
            state.simulationCounts.remove(chunkPos);
            releaseSimulationIfIssued(state, chunkPos, actions);
        }
    }

    private static void issueSimulationIfReady(State state, long chunkPos, List<Action> actions) {
        if (state.simulationCounts.get(chunkPos) > 0
            && state.phase.get(chunkPos) == ACTIVE
            && state.issuedSimulation.add(chunkPos)) {
            actions.add(Action.addSimulation(state.bridge, chunkPos));
        }
    }

    private static void releaseSimulationIfIssued(State state, long chunkPos, List<Action> actions) {
        if (state.issuedSimulation.remove(chunkPos)) {
            actions.add(Action.removeSimulation(state.bridge, chunkPos));
        }
    }

    private static void finalizeRemovals(State state, List<Action> actions) {
        List<PlayerState> removed = new ArrayList<>();
        for (PlayerState player : state.players.values()) {
            if (player.pendingRemoval) {
                removed.add(player);
            }
        }

        for (PlayerState player : removed) {
            LongArrayList positions = new LongArrayList(player.positions);
            for (long chunkPos : positions) {
                removeDesired(state, player, chunkPos, actions);
            }
            state.players.remove(player.player);
        }
    }

    private static PlayerState findOwner(State state, long chunkPos) {
        for (PlayerState player : state.players.values()) {
            if (!player.pendingRemoval && player.positions.contains(chunkPos)) {
                return player;
            }
        }
        return null;
    }

    private static int priority(PlayerState player, long chunkPos) {
        int x = ChunkPos.getX(chunkPos);
        int z = ChunkPos.getZ(chunkPos);
        return Math.max(Math.abs(x - player.centerX), Math.abs(z - player.centerZ));
    }

    private static boolean inside(int x, int z, int centerX, int centerZ, int radius) {
        return Math.abs(x - centerX) <= radius && Math.abs(z - centerZ) <= radius;
    }

    private static void forEachSquare(int centerX, int centerZ, int radius, PositionConsumer consumer) {
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                consumer.accept(x, z);
            }
        }
    }

    private static void run(List<Action> actions) {
        for (Action action : actions) {
            switch (action.type) {
                case SUBMIT_LOADING -> action.bridge.javaReforged$submitPlayerLoadingTicket(action.chunkPos, action.priority);
                case RELEASE_LOADING -> action.bridge.javaReforged$releasePlayerLoadingTicket(action.chunkPos);
                case ADD_SIMULATION -> action.bridge.javaReforged$addManagedSimulationTicket(action.chunkPos);
                case REMOVE_SIMULATION -> action.bridge.javaReforged$removeManagedSimulationTicket(action.chunkPos);
            }
        }
    }

    private static final class State {
        private final Map<ServerPlayer, PlayerState> players = new IdentityHashMap<>();
        private final Long2IntOpenHashMap desiredCounts = new Long2IntOpenHashMap();
        private final Long2ByteOpenHashMap phase = new Long2ByteOpenHashMap();
        private final Long2ObjectOpenHashMap<Request> pendingByChunk = new Long2ObjectOpenHashMap<>();
        private final Long2IntOpenHashMap simulationCounts = new Long2IntOpenHashMap();
        private final LongOpenHashSet issuedSimulation = new LongOpenHashSet();
        private NativeTicketBridge bridge;
        private int viewDistance;
        private int simulationDistance;
        private long nextSequence;

        State(DistanceManager manager) {
            this.desiredCounts.defaultReturnValue(0);
            this.phase.defaultReturnValue(NONE);
            this.simulationCounts.defaultReturnValue(0);
        }
    }

    private static final class PlayerState {
        private final State state;
        private final ServerPlayer player;
        private final LongOpenHashSet positions = new LongOpenHashSet();
        private final LongOpenHashSet simulationPositions = new LongOpenHashSet();
        private final PriorityQueue<Request> requests = new PriorityQueue<>(
            Comparator.comparingInt((Request request) -> request.priority).thenComparingLong(request -> request.sequence)
        );
        private boolean initialized;
        private boolean pendingRemoval;
        private int centerX;
        private int centerZ;
        private int radius;
        private boolean simulationInitialized;
        private int simulationCenterX;
        private int simulationCenterZ;
        private int simulationRadius;

        PlayerState(State state, ServerPlayer player) {
            this.state = state;
            this.player = player;
        }
    }

    private record Request(PlayerState owner, long chunkPos, int priority, long sequence) {
    }

    private record Action(NativeTicketBridge bridge, long chunkPos, int priority, ActionType type) {
        static Action submit(NativeTicketBridge bridge, long chunkPos, int priority) {
            return new Action(bridge, chunkPos, priority, ActionType.SUBMIT_LOADING);
        }

        static Action release(NativeTicketBridge bridge, long chunkPos) {
            return new Action(bridge, chunkPos, 0, ActionType.RELEASE_LOADING);
        }

        static Action addSimulation(NativeTicketBridge bridge, long chunkPos) {
            return new Action(bridge, chunkPos, 0, ActionType.ADD_SIMULATION);
        }

        static Action removeSimulation(NativeTicketBridge bridge, long chunkPos) {
            return new Action(bridge, chunkPos, 0, ActionType.REMOVE_SIMULATION);
        }
    }

    private enum ActionType {
        SUBMIT_LOADING,
        RELEASE_LOADING,
        ADD_SIMULATION,
        REMOVE_SIMULATION
    }

    private record BootstrapContext(ServerPlayer player, int viewDistance) {
    }

    @FunctionalInterface
    private interface PositionConsumer {
        void accept(int x, int z);
    }

    /** Implemented by DistanceManagerMixin; the native dispatcher remains owner of execution. */
    public interface NativeTicketBridge {
        DistanceManager javaReforged$distanceManager();

        void javaReforged$submitPlayerLoadingTicket(long chunkPos, int priority);

        void javaReforged$releasePlayerLoadingTicket(long chunkPos);

        int javaReforged$simulationDistance();

        void javaReforged$addManagedSimulationTicket(long chunkPos);

        void javaReforged$removeManagedSimulationTicket(long chunkPos);

    }

    public record Snapshot(
        int quota,
        int addedThisTick,
        int admittedThisTick,
        int bootstrapThisTick,
        int pendingRequests,
        int lastAdded,
        int lastAdmitted,
        int lastBootstrap,
        long totalAdded,
        long totalAdmitted,
        long totalBootstrap
    ) {
    }
}
