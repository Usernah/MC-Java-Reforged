package net.alnv.javareforged.ClientRuntime.runtime;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import com.mojang.logging.LogUtils;
import net.alnv.javareforged.ClientRuntime.slot.PlayerSlot;
import net.alnv.javareforged.ClientRuntime.slot.PlayerSlots;
import net.alnv.javareforged.ClientRuntime.state.TerrainState;
import net.alnv.javareforged.ClientRuntime.terrain.GlobalTerrainStore;
import net.alnv.javareforged.ClientRuntime.terrain.SlotTerrainView;
import net.alnv.javareforged.ClientRuntime.terrain.TerrainKey;
import net.alnv.javareforged.mixin.SSM.ClientChunkCacheAccessor;
import net.alnv.javareforged.mixin.SSM.LevelRendererSSAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.SectionPos;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public final class TerrainDebug {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean ENABLED = Boolean.getBoolean("split.terrainDebug") && !Boolean.getBoolean("split.disableTerrainDebug");
    private static final boolean SUMMARY_LOGS = Boolean.getBoolean("split.terrainDebugSummary");
    private static final int LOG_INTERVAL_FRAMES = 60;
    private static final int TARGETED_LOG_INTERVAL_FRAMES = Integer.getInteger("split.terrainDiagInterval", 120);
    private static final int TRACE_REPEAT_INTERVAL_FRAMES = Integer.getInteger("split.terrainTraceRepeat", 40);
    private static final int VISIBLE_JUMP_THRESHOLD = 180;
    private static final Snapshot[] SNAPSHOTS = new Snapshot[PlayerSlots.MAX_SLOTS];
    private static final TransitionState[] TRANSITIONS = new TransitionState[PlayerSlots.MAX_SLOTS];
    private static final long[] LAST_VIEW_UPDATE_LOG_FRAMES = new long[PlayerSlots.MAX_SLOTS];
    private static final long[] LAST_VISIBLE_JUMP_LOG_FRAMES = new long[PlayerSlots.MAX_SLOTS];
    private static final long[] LAST_VISIBILITY_DIAG_LOG_FRAMES = new long[PlayerSlots.MAX_SLOTS];
    private static final TerrainTrace[] TERRAIN_TRACES = new TerrainTrace[PlayerSlots.MAX_SLOTS];
    private static long lastCompileDiagFrame = -TARGETED_LOG_INTERVAL_FRAMES;
    private static long lastChunkDiagFrame = -TARGETED_LOG_INTERVAL_FRAMES;
    private static long lastReferenceDiagFrame = -TARGETED_LOG_INTERVAL_FRAMES;
    private static final ChunkRouteAggregate[] CHUNK_PACKET_STATS = new ChunkRouteAggregate[PlayerSlots.MAX_SLOTS + 1];
    private static final ChunkRouteAggregate[] CHUNK_CACHE_STATS = new ChunkRouteAggregate[PlayerSlots.MAX_SLOTS + 1];
    private static final Map<String, Long> LAST_RAW_STATE_LOG_FRAMES = new HashMap<>();
    private static final Map<SectionRenderDispatcher, ClientLevel> SECTION_DISPATCHER_LEVELS = new WeakHashMap<>();
    private static long lastChunkPacketRouteFrame = -TARGETED_LOG_INTERVAL_FRAMES;
    private static long lastChunkCacheRouteFrame = -TARGETED_LOG_INTERVAL_FRAMES;
    private static long lastConnectionDiagFrame = -TARGETED_LOG_INTERVAL_FRAMES;
    private static Field clientChunkCacheStorageField;
    private static boolean clientChunkCacheStorageFieldResolved;
    private static Class<?> clientChunkCacheStorageClass;
    private static Field storageViewCenterXField;
    private static Field storageViewCenterZField;
    private static Field storageChunkRadiusField;
    private static Field storageViewRangeField;
    private static long frameId;
    private static long lastSummaryFrame = -LOG_INTERVAL_FRAMES;
    private static boolean overlayVisible;

    private TerrainDebug() {
    }

    public static boolean enabled() {
        return ENABLED;
    }

    public static boolean overlayVisible() {
        return ENABLED && overlayVisible;
    }

    public static boolean toggleOverlay() {
        if (!ENABLED) {
            return false;
        }
        overlayVisible = !overlayVisible;
        return overlayVisible;
    }

    public static void beginFrame() {
        if (!ENABLED) {
            return;
        }
        frameId++;
    }

    public static void recordSlot(
        PlayerSlot slot,
        GlobalTerrainStore store,
        GlobalTerrainStore.MaterializationResult materialization
    ) {
        if (!ENABLED) {
            return;
        }

        TerrainState terrain = slot.renderState().terrain();
        SectionRenderDispatcher dispatcher = store.dispatcher();
        Snapshot previous = SNAPSHOTS[slot.id()];
        Snapshot snapshot = new Snapshot(
            frameId,
            slot.id(),
            slot.renderState().level() != null,
            slot.gameplayState().player() != null,
            slot.gameplayState().gameMode() != null,
            terrain.view().sections().size(),
            store.entryCount(),
            store.referencedEntryCount(),
            store.unmaterializedReferencedCount(),
            store.sectionCount(),
            store.pendingCount(),
            store.ownerWaitCount(),
            materialization.limit(),
            materialization.materialized(),
            materialization.skippedWithoutLoadedOwner(),
            materialization.skippedStale(),
            terrain.visibleSections().size(),
            terrain.layerSize(RenderType.solid()),
            terrain.layerSize(RenderType.cutoutMipped()),
            terrain.layerSize(RenderType.cutout()),
            terrain.layerSize(RenderType.translucent()),
            terrain.layerSize(RenderType.tripwire()),
            terrain.compiledVisibleCount(),
            terrain.nonEmptyVisibleCount(),
            store.dirtyQueueCount(),
            store.activeTaskCount(),
            dispatcher.getToBatchCount(),
            dispatcher.getToUpload(),
            dispatcher.getFreeBufferCount()
        );
        SNAPSHOTS[slot.id()] = snapshot;
        logTransitions(previous, snapshot);
        logVisibleJump(previous, snapshot, terrain.nullableCameraPosition());
    }

    public static void recordInactiveSlot(PlayerSlot slot) {
        if (!ENABLED) {
            return;
        }
        TerrainState terrain = slot.renderState().terrain();
        SNAPSHOTS[slot.id()] = new Snapshot(
            frameId,
            slot.id(),
            slot.renderState().level() != null,
            slot.gameplayState().player() != null,
            slot.gameplayState().gameMode() != null,
            terrain.view().sections().size(),
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            terrain.visibleSections().size(),
            terrain.layerSize(RenderType.solid()),
            terrain.layerSize(RenderType.cutoutMipped()),
            terrain.layerSize(RenderType.cutout()),
            terrain.layerSize(RenderType.translucent()),
            terrain.layerSize(RenderType.tripwire()),
            terrain.compiledVisibleCount(),
            terrain.nonEmptyVisibleCount(),
            0,
            0,
            0,
            0,
            0
        );
    }

    public static void endFrame() {
        if (!ENABLED || !SUMMARY_LOGS || frameId - lastSummaryFrame < LOG_INTERVAL_FRAMES) {
            return;
        }
        lastSummaryFrame = frameId;
        for (Snapshot snapshot : SNAPSHOTS) {
            if (snapshot != null && snapshot.frameId() == frameId) {
                LOGGER.info("split terrain {}", snapshot.toLogLine());
            }
        }
    }

    public static void renderOverlay(GuiGraphics graphics, PlayerSlot slot) {
        if (!overlayVisible()) {
            return;
        }
        Snapshot snapshot = SNAPSHOTS[slot.id()];
        if (snapshot == null) {
            return;
        }

        int x = 4;
        int y = 4;
        int lineHeight = 10;
        int background = 0xA0000000;
        int foreground = 0xFFE6F2FF;
        int warning = snapshot.visibleSections() == 0 || snapshot.nonEmptyVisible() == 0 ? 0xFFFFD166 : foreground;
        Minecraft minecraft = Minecraft.getInstance();

        String[] lines = new String[] {
            "terrain s" + snapshot.slotId() + " f" + snapshot.frameId(),
            "bound l/p/g " + flag(snapshot.hasLevel()) + "/" + flag(snapshot.hasPlayer()) + "/" + flag(snapshot.hasGameMode()),
            "keys " + snapshot.viewKeys() + " entries " + snapshot.entries() + " refs " + snapshot.referenced()
                + " unmat " + snapshot.unmaterialized(),
            "sections " + snapshot.sections(),
            "pending " + snapshot.pending() + " wait " + snapshot.ownerWait() + " mat " + snapshot.materialized() + "/" + snapshot.materializeLimit()
                + " noOwner " + snapshot.noOwner(),
            "visible " + snapshot.visibleSections() + " compiled " + snapshot.compiledVisible()
                + " nonEmpty " + snapshot.nonEmptyVisible(),
            "layers s/cm/c/t/tr " + snapshot.solid() + "/" + snapshot.cutoutMipped() + "/" + snapshot.cutout()
                + "/" + snapshot.translucent() + "/" + snapshot.tripwire(),
            "dirty " + snapshot.dirtyQueued() + " tasks " + snapshot.activeTasks()
                + " batch " + snapshot.toBatch() + " upload " + snapshot.toUpload() + " free " + snapshot.freeBuffers()
        };

        int width = 0;
        for (String line : lines) {
            width = Math.max(width, minecraft.font.width(line));
        }
        graphics.fill(x - 2, y - 2, x + width + 3, y + lines.length * lineHeight + 1, background);
        for (int index = 0; index < lines.length; index++) {
            graphics.drawString(minecraft.font, lines[index], x, y + index * lineHeight, index >= 4 ? warning : foreground, false);
        }
    }


    public static void recordVisibilityDiagnostics(
        PlayerSlot slot,
        Vec3 cameraPosition,
        int viewDistance,
        boolean graphReady,
        boolean rebuildVisibility,
        int occlusionCandidates,
        int visibleBefore,
        int visibleAfter,
        int visibleByExistingKeys,
        GlobalTerrainStore.DiagnosticCounts counts,
        GlobalTerrainStore.MaterializationResult materialization
    ) {
        if (!ENABLED || !counts.suspicious()) {
            return;
        }
        long lastLogFrame = LAST_VISIBILITY_DIAG_LOG_FRAMES[slot.id()];
        if (frameId - lastLogFrame < TARGETED_LOG_INTERVAL_FRAMES) {
            return;
        }
        LAST_VISIBILITY_DIAG_LOG_FRAMES[slot.id()] = frameId;

        LOGGER.info(
            "split terrain diag visibility slot={} frame={} camera={} sec={}/{}/{} vd={} graphReady={} rebuild={} candidates={} visible={} before={} byKeys={} {} mat={}/{} pending={} noOwnerMat={}",
            slot.id(),
            frameId,
            compactPosition(cameraPosition),
            SectionPos.blockToSectionCoord(cameraPosition.x),
            SectionPos.blockToSectionCoord(cameraPosition.y),
            SectionPos.blockToSectionCoord(cameraPosition.z),
            viewDistance,
            graphReady,
            rebuildVisibility,
            occlusionCandidates,
            visibleAfter,
            visibleBefore,
            visibleByExistingKeys,
            counts.toLogLine(),
            materialization.materialized(),
            materialization.limit(),
            materialization.remainingPending(),
            materialization.skippedWithoutLoadedOwner()
        );
    }

    public static void recordCompileDiagnostics(GlobalTerrainStore.CompileStats stats) {
        if (!ENABLED || !stats.hasSignal()) {
            return;
        }
        if (frameId - lastCompileDiagFrame < TARGETED_LOG_INTERVAL_FRAMES) {
            return;
        }
        lastCompileDiagFrame = frameId;
        LOGGER.info("split terrain diag compile frame={} {}", frameId, stats.toLogLine());
    }

    public static void recordChunkDiagnostics(GlobalTerrainStore.ChunkLoadStats stats) {
        if (!ENABLED || !stats.hasSignal()) {
            return;
        }
        if (frameId - lastChunkDiagFrame < TARGETED_LOG_INTERVAL_FRAMES) {
            return;
        }
        lastChunkDiagFrame = frameId;
        LOGGER.info("split terrain diag chunk frame={} {}", frameId, stats.toLogLine());
    }

    public static void recordReferenceDiagnostics(String event, int slotId, GlobalTerrainStore.ReferenceStats stats) {
        if (!ENABLED || !stats.hasSignal()) {
            return;
        }
        if (frameId - lastReferenceDiagFrame < TARGETED_LOG_INTERVAL_FRAMES) {
            return;
        }
        lastReferenceDiagFrame = frameId;
        LOGGER.info("split terrain diag refs frame={} event={} slot={} {}", frameId, event, slotId, stats.toLogLine());
    }

    public static boolean isTracingTerrainKey(TerrainKey key) {
        if (!ENABLED || key == null) {
            return false;
        }
        for (TerrainTrace trace : TERRAIN_TRACES) {
            if (trace != null && key.equals(trace.key)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isTracingTerrainKey(int slotId, TerrainKey key) {
        if (!ENABLED || key == null || slotId < 0 || slotId >= PlayerSlots.MAX_SLOTS) {
            return false;
        }
        TerrainTrace trace = TERRAIN_TRACES[slotId];
        return trace != null && key.equals(trace.key);
    }

    public static void selectTerrainTrace(int slotId, TerrainKey key, String reason, String state) {
        if (!ENABLED || key == null || slotId < 0 || slotId >= PlayerSlots.MAX_SLOTS) {
            return;
        }
        TerrainTrace trace = TERRAIN_TRACES[slotId];
        boolean changed = trace == null || !key.equals(trace.key) || !safeEquals(reason, trace.reason);
        if (!changed) {
            recordTerrainTrace(slotId, key, "watch", reason + " " + state);
            return;
        }
        trace = new TerrainTrace(key, reason, frameId);
        TERRAIN_TRACES[slotId] = trace;
        LOGGER.info(
            "split terrain trace start frame={} slot={} key={} reason={} {}",
            frameId,
            slotId,
            key,
            reason,
            state
        );
        trace.lastLine = "start " + reason + " " + state;
        trace.lastLogFrame = frameId;
    }

    public static void recordTerrainTrace(int slotId, TerrainKey key, String event, String state) {
        if (!ENABLED || key == null || slotId < 0 || slotId >= PlayerSlots.MAX_SLOTS) {
            return;
        }
        TerrainTrace trace = TERRAIN_TRACES[slotId];
        if (trace == null || !key.equals(trace.key)) {
            return;
        }
        String line = event + " " + state;
        if (line.equals(trace.lastLine) && frameId - trace.lastLogFrame < TRACE_REPEAT_INTERVAL_FRAMES) {
            return;
        }
        trace.lastLine = line;
        trace.lastLogFrame = frameId;
        trace.sequence++;
        LOGGER.info(
            "split terrain trace frame={} slot={} seq={} key={} event={} age={} {}",
            frameId,
            slotId,
            trace.sequence,
            key,
            event,
            frameId - trace.startFrame,
            state
        );
    }

    public static void recordTerrainTraceForAnySlot(TerrainKey key, String event, String state) {
        if (!ENABLED || key == null) {
            return;
        }
        for (int slotId = 0; slotId < PlayerSlots.MAX_SLOTS; slotId++) {
            recordTerrainTrace(slotId, key, event, state);
        }
    }

    private static boolean safeEquals(String left, String right) {
        return left == null ? right == null : left.equals(right);
    }


    public static void recordSectionDispatcherLevel(SectionRenderDispatcher dispatcher, ClientLevel level) {
        if (!ENABLED || dispatcher == null) {
            return;
        }
        synchronized (SECTION_DISPATCHER_LEVELS) {
            if (level == null) {
                SECTION_DISPATCHER_LEVELS.remove(dispatcher);
            } else {
                SECTION_DISPATCHER_LEVELS.put(dispatcher, level);
            }
        }
    }

    public static void recordMinecraftConnectionRoute(ClientPacketListener returnedListener) {
        if (!ENABLED) {
            return;
        }
        Integer activeSlot = ActiveSlot.idOrNull();
        if (activeSlot == null || activeSlot <= 0) {
            return;
        }
        int connectionSlot = slotForListener(returnedListener);
        boolean primary = returnedListener != null && LocalPlayers.INSTANCE.isPrimaryPacketListener(returnedListener);
        boolean signal = connectionSlot != activeSlot || primary;
        if (!signal || frameId - lastConnectionDiagFrame < TARGETED_LOG_INTERVAL_FRAMES) {
            return;
        }
        lastConnectionDiagFrame = frameId;
        LOGGER.info(
            "split raw state diag getConnection frame={} active={} returnedSlot={} primary={} listenerId={} expectedSecondary=true",
            frameId,
            activeSlot,
            connectionSlot,
            primary,
            returnedListener == null ? 0 : System.identityHashCode(returnedListener)
        );
    }

    public static void recordRawStateCheck(String phase, int expectedSlot, Minecraft minecraft) {
        if (!ENABLED || minecraft == null) {
            return;
        }
        RawStateSnapshot snapshot = RawStateSnapshot.capture(phase, expectedSlot, minecraft);
        boolean signal = snapshot.hasSignal();
        String key = phase + ":" + expectedSlot;
        Long last = LAST_RAW_STATE_LOG_FRAMES.get(key);
        if (last != null) {
            long interval = signal ? TARGETED_LOG_INTERVAL_FRAMES : LOG_INTERVAL_FRAMES * 5L;
            if (frameId - last < interval) {
                return;
            }
        }
        LAST_RAW_STATE_LOG_FRAMES.put(key, frameId);
        LOGGER.info("split raw state diag frame={} {}", frameId, snapshot.toLogLine());
    }

    private static int slotForSectionDispatcher(SectionRenderDispatcher dispatcher) {
        if (dispatcher == null) {
            return -1;
        }
        ClientLevel level;
        synchronized (SECTION_DISPATCHER_LEVELS) {
            level = SECTION_DISPATCHER_LEVELS.get(dispatcher);
        }
        return slotForLevel(level);
    }

    private static int slotForPlayer(LocalPlayer player) {
        if (player == null) {
            return -1;
        }
        for (int slotId = 0; slotId < PlayerSlots.MAX_SLOTS; slotId++) {
            PlayerSlot slot = LocalPlayers.INSTANCE.slots().slot(slotId);
            if (slot.gameplayState().player() == player) {
                return slotId;
            }
        }
        return -1;
    }

    private static int slotForGameMode(MultiPlayerGameMode gameMode) {
        if (gameMode == null) {
            return -1;
        }
        for (int slotId = 0; slotId < PlayerSlots.MAX_SLOTS; slotId++) {
            PlayerSlot slot = LocalPlayers.INSTANCE.slots().slot(slotId);
            if (slot.gameplayState().gameMode() == gameMode) {
                return slotId;
            }
        }
        return -1;
    }

    private static int slotForEntity(Entity entity) {
        if (entity == null) {
            return -1;
        }
        for (int slotId = 0; slotId < PlayerSlots.MAX_SLOTS; slotId++) {
            PlayerSlot slot = LocalPlayers.INSTANCE.slots().slot(slotId);
            if (slot.gameplayState().player() == entity || slot.renderState().cameraEntity() == entity || slot.renderState().crosshairPickEntity() == entity) {
                return slotId;
            }
        }
        return -1;
    }

    private static int runtimeSlotIdOrMinusOne() {
        try {
            return LocalPlayers.INSTANCE.runtimeSlot().id();
        } catch (RuntimeException ignored) {
            return -1;
        }
    }


    public static ChunkRouteInfo chunkRouteInfo(
        String event,
        ClientPacketListener listener,
        ClientLevel level,
        ClientChunkCache source,
        Integer activeSlot,
        int serverRadius,
        boolean minecraftLevel
    ) {
        int listenerSlot = listener == null ? -1 : slotForListener(listener);
        int levelSlot = slotForLevel(level);
        int cacheSlot = source == null ? -1 : slotForLevel(((ClientChunkCacheAccessor)(Object)source).splitTest$level());
        int centerX = Integer.MIN_VALUE;
        int centerZ = Integer.MIN_VALUE;
        int chunkRadius = -1;
        int viewRange = -1;
        int loaded = -1;
        if (source != null) {
            loaded = source.getLoadedChunksCount();
            Object storage = readClientChunkCacheStorage(source);
            if (storage != null) {
                centerX = readStorageInt(storage, "viewCenterX", centerX);
                centerZ = readStorageInt(storage, "viewCenterZ", centerZ);
                chunkRadius = readStorageInt(storage, "chunkRadius", chunkRadius);
                viewRange = readStorageInt(storage, "viewRange", viewRange);
            }
        }
        int playerChunkX = Integer.MIN_VALUE;
        int playerChunkZ = Integer.MIN_VALUE;
        int resolvedSlot = listenerSlot >= 0 ? listenerSlot : levelSlot;
        if (resolvedSlot >= 0 && resolvedSlot < PlayerSlots.MAX_SLOTS) {
            PlayerSlot slot = LocalPlayers.INSTANCE.slots().slot(resolvedSlot);
            if (slot.gameplayState().player() != null) {
                playerChunkX = SectionPos.blockToSectionCoord(slot.gameplayState().player().getX());
                playerChunkZ = SectionPos.blockToSectionCoord(slot.gameplayState().player().getZ());
            }
        }
        return new ChunkRouteInfo(
            event,
            activeSlot == null ? -1 : activeSlot,
            listenerSlot,
            levelSlot,
            cacheSlot,
            resolvedSlot,
            minecraftLevel,
            serverRadius,
            centerX,
            centerZ,
            chunkRadius,
            viewRange,
            loaded,
            playerChunkX,
            playerChunkZ,
            listener == null ? 0 : System.identityHashCode(listener),
            level == null ? 0 : System.identityHashCode(level),
            source == null ? 0 : System.identityHashCode(source)
        );
    }

    private static Object readClientChunkCacheStorage(ClientChunkCache source) {
        Field field = clientChunkCacheStorageField();
        if (field == null) {
            return null;
        }
        try {
            return field.get(source);
        } catch (IllegalAccessException exception) {
            return null;
        }
    }

    private static Field clientChunkCacheStorageField() {
        if (clientChunkCacheStorageFieldResolved) {
            return clientChunkCacheStorageField;
        }
        clientChunkCacheStorageFieldResolved = true;
        try {
            clientChunkCacheStorageField = ClientChunkCache.class.getDeclaredField("storage");
            clientChunkCacheStorageField.setAccessible(true);
        } catch (NoSuchFieldException exception) {
            LOGGER.warn("split terrain diag could not find ClientChunkCache.storage", exception);
        }
        return clientChunkCacheStorageField;
    }

    private static int readStorageInt(Object storage, String fieldName, int fallback) {
        Field field = storageIntField(storage.getClass(), fieldName);
        if (field == null) {
            return fallback;
        }
        try {
            return field.getInt(storage);
        } catch (IllegalAccessException exception) {
            return fallback;
        }
    }

    private static Field storageIntField(Class<?> storageClass, String fieldName) {
        if (clientChunkCacheStorageClass != storageClass) {
            clientChunkCacheStorageClass = storageClass;
            storageViewCenterXField = declaredField(storageClass, "viewCenterX");
            storageViewCenterZField = declaredField(storageClass, "viewCenterZ");
            storageChunkRadiusField = declaredField(storageClass, "chunkRadius");
            storageViewRangeField = declaredField(storageClass, "viewRange");
        }
        return switch (fieldName) {
            case "viewCenterX" -> storageViewCenterXField;
            case "viewCenterZ" -> storageViewCenterZField;
            case "chunkRadius" -> storageChunkRadiusField;
            case "viewRange" -> storageViewRangeField;
            default -> null;
        };
    }

    private static Field declaredField(Class<?> type, String name) {
        try {
            Field field = type.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException exception) {
            LOGGER.warn("split terrain diag could not find {}.{}", type.getName(), name, exception);
            return null;
        }
    }

    public static void recordChunkPacketRoute(ChunkRouteInfo route, int chunkX, int chunkZ, boolean chunkPresent) {
        if (!ENABLED || route == null) {
            return;
        }
        int index = aggregateIndex(route.resolvedSlot());
        ChunkRouteAggregate stats = packetStats(index);
        stats.add(route, chunkX, chunkZ, chunkPresent);
        boolean signal = stats.hasSignal();
        if (!signal && frameId - lastChunkPacketRouteFrame < TARGETED_LOG_INTERVAL_FRAMES) {
            return;
        }
        if (frameId - lastChunkPacketRouteFrame < 8 && stats.total() < 64) {
            return;
        }
        lastChunkPacketRouteFrame = frameId;
        LOGGER.info("split chunk diag packet frame={} {}", frameId, stats.toLogLine());
        stats.reset();
    }

    public static void recordChunkCacheMutation(ChunkRouteInfo route, int chunkX, int chunkZ, boolean chunkPresent) {
        if (!ENABLED || route == null) {
            return;
        }
        int index = aggregateIndex(route.resolvedSlot());
        ChunkRouteAggregate stats = cacheStats(index);
        stats.add(route, chunkX, chunkZ, chunkPresent);
        boolean signal = stats.hasSignal();
        if (!signal && frameId - lastChunkCacheRouteFrame < TARGETED_LOG_INTERVAL_FRAMES) {
            return;
        }
        if (frameId - lastChunkCacheRouteFrame < 8 && stats.total() < 64) {
            return;
        }
        lastChunkCacheRouteFrame = frameId;
        LOGGER.info("split chunk diag cache frame={} {}", frameId, stats.toLogLine());
        stats.reset();
    }

    public static int slotForLevel(ClientLevel level) {
        if (level == null) {
            return -1;
        }
        for (int slotId = 0; slotId < PlayerSlots.MAX_SLOTS; slotId++) {
            PlayerSlot slot = LocalPlayers.INSTANCE.slots().slot(slotId);
            if (slot.renderState().level() == level) {
                return slotId;
            }
        }
        return -1;
    }

    private static int slotForListener(ClientPacketListener listener) {
        if (listener == null) {
            return -1;
        }
        try {
            return LocalPlayers.INSTANCE.slotForClientPacketListener(listener);
        } catch (RuntimeException ignored) {
            return -1;
        }
    }

    private static int aggregateIndex(int slotId) {
        return slotId >= 0 && slotId < PlayerSlots.MAX_SLOTS ? slotId : PlayerSlots.MAX_SLOTS;
    }

    private static ChunkRouteAggregate packetStats(int index) {
        ChunkRouteAggregate stats = CHUNK_PACKET_STATS[index];
        if (stats == null) {
            stats = new ChunkRouteAggregate(index == PlayerSlots.MAX_SLOTS ? -1 : index);
            CHUNK_PACKET_STATS[index] = stats;
        }
        return stats;
    }

    private static ChunkRouteAggregate cacheStats(int index) {
        ChunkRouteAggregate stats = CHUNK_CACHE_STATS[index];
        if (stats == null) {
            stats = new ChunkRouteAggregate(index == PlayerSlots.MAX_SLOTS ? -1 : index);
            CHUNK_CACHE_STATS[index] = stats;
        }
        return stats;
    }


    public static void recordViewUpdate(
        PlayerSlot slot,
        Vec3 cameraPosition,
        int viewDistance,
        SlotTerrainView.Update update,
        boolean graphReleased
    ) {
        if (!ENABLED || (!update.reset() && update.removed().isEmpty())) {
            return;
        }

        long lastLogFrame = LAST_VIEW_UPDATE_LOG_FRAMES[slot.id()];
        if (!update.reset() && frameId - lastLogFrame < TARGETED_LOG_INTERVAL_FRAMES) {
            return;
        }
        LAST_VIEW_UPDATE_LOG_FRAMES[slot.id()] = frameId;

        LOGGER.info(
            "split terrain view update slot={} frame={} reset={} added={} removed={} graphReleased={} distance={} camera={} section={}/{}/{}",
            slot.id(),
            frameId,
            update.reset(),
            update.added().size(),
            update.removed().size(),
            graphReleased,
            viewDistance,
            compactPosition(cameraPosition),
            SectionPos.blockToSectionCoord(cameraPosition.x),
            SectionPos.blockToSectionCoord(cameraPosition.y),
            SectionPos.blockToSectionCoord(cameraPosition.z)
        );
    }

    private static void logTransitions(Snapshot previous, Snapshot snapshot) {
        if (previous == null) {
            return;
        }
        TransitionState transitions = transitions(snapshot.slotId());
        if (!transitions.loggedViewKeys && previous.viewKeys() == 0 && snapshot.viewKeys() > 0) {
            transitions.loggedViewKeys = true;
            LOGGER.info("split terrain slot {} acquired view keys: {}", snapshot.slotId(), snapshot.viewKeys());
        }
        if (!transitions.loggedSections && previous.sections() == 0 && snapshot.sections() > 0) {
            transitions.loggedSections = true;
            LOGGER.info("split terrain slot {} materialized first sections: {}", snapshot.slotId(), snapshot.sections());
        }
        if (!transitions.loggedVisible && previous.visibleSections() == 0 && snapshot.visibleSections() > 0) {
            transitions.loggedVisible = true;
            LOGGER.info("split terrain slot {} acquired visible sections: {}", snapshot.slotId(), snapshot.visibleSections());
        }
        if (!transitions.loggedRenderable && previous.nonEmptyVisible() == 0 && snapshot.nonEmptyVisible() > 0) {
            transitions.loggedRenderable = true;
            LOGGER.info("split terrain slot {} acquired renderable terrain: {}", snapshot.slotId(), snapshot.nonEmptyVisible());
        }
    }

    private static void logVisibleJump(Snapshot previous, Snapshot snapshot, Vec3 cameraPosition) {
        if (previous == null) {
            return;
        }

        int visibleDelta = snapshot.visibleSections() - previous.visibleSections();
        if (Math.abs(visibleDelta) < VISIBLE_JUMP_THRESHOLD) {
            return;
        }

        long lastLogFrame = LAST_VISIBLE_JUMP_LOG_FRAMES[snapshot.slotId()];
        if (frameId - lastLogFrame < TARGETED_LOG_INTERVAL_FRAMES) {
            return;
        }
        LAST_VISIBLE_JUMP_LOG_FRAMES[snapshot.slotId()] = frameId;

        LOGGER.info(
            "split terrain visible jump slot={} frame={} visible {}->{} compiled {}->{} nonEmpty {}->{} entries {}->{} camera={}",
            snapshot.slotId(),
            frameId,
            previous.visibleSections(),
            snapshot.visibleSections(),
            previous.compiledVisible(),
            snapshot.compiledVisible(),
            previous.nonEmptyVisible(),
            snapshot.nonEmptyVisible(),
            previous.entries(),
            snapshot.entries(),
            cameraPosition == null ? "unknown" : compactPosition(cameraPosition)
        );
    }

    private static TransitionState transitions(int slotId) {
        TransitionState transitions = TRANSITIONS[slotId];
        if (transitions == null) {
            transitions = new TransitionState();
            TRANSITIONS[slotId] = transitions;
        }
        return transitions;
    }

    private static String flag(boolean value) {
        return value ? "Y" : "N";
    }

    private static String compactPosition(Vec3 position) {
        return String.format(Locale.ROOT, "%.2f/%.2f/%.2f", position.x, position.y, position.z);
    }

    private static final class TransitionState {
        private boolean loggedViewKeys;
        private boolean loggedSections;
        private boolean loggedVisible;
        private boolean loggedRenderable;
    }


    private static final class TerrainTrace {
        private final TerrainKey key;
        private final String reason;
        private final long startFrame;
        private long lastLogFrame = Long.MIN_VALUE;
        private String lastLine = "";
        private int sequence;

        private TerrainTrace(TerrainKey key, String reason, long startFrame) {
            this.key = key;
            this.reason = reason;
            this.startFrame = startFrame;
        }
    }


    private record RawStateSnapshot(
        String phase,
        int expectedSlot,
        int activeSlot,
        int scheduledSlot,
        int runtimeSlot,
        int minecraftLevelSlot,
        int minecraftPlayerSlot,
        int minecraftGameModeSlot,
        int minecraftCameraEntitySlot,
        int minecraftCrosshairEntitySlot,
        int minecraftConnectionSlot,
        boolean minecraftConnectionPrimary,
        int levelRendererSlot,
        int sectionDispatcherSlot,
        int levelId,
        int playerId,
        int gameModeId,
        int connectionId,
        int levelRendererLevelId,
        int sectionDispatcherId
    ) {
        static RawStateSnapshot capture(String phase, int expectedSlot, Minecraft minecraft) {
            ClientPacketListener connection = minecraft.getConnection();
            ClientLevel levelRendererLevel = ((LevelRendererSSAccessor)minecraft.levelRenderer).splitTest$getLevel();
            SectionRenderDispatcher dispatcher = ((LevelRendererSSAccessor)minecraft.levelRenderer).splitTest$getSectionRenderDispatcher();
            Integer active = ActiveSlot.idOrNull();
            Integer scheduled = ActiveSlot.scheduledIdOrNull();
            return new RawStateSnapshot(
                phase,
                expectedSlot,
                active == null ? -1 : active,
                scheduled == null ? -1 : scheduled,
                runtimeSlotIdOrMinusOne(),
                slotForLevel(minecraft.level),
                slotForPlayer(minecraft.player),
                slotForGameMode(minecraft.gameMode),
                slotForEntity(minecraft.cameraEntity),
                slotForEntity(minecraft.crosshairPickEntity),
                slotForListener(connection),
                connection != null && LocalPlayers.INSTANCE.isPrimaryPacketListener(connection),
                slotForLevel(levelRendererLevel),
                slotForSectionDispatcher(dispatcher),
                minecraft.level == null ? 0 : System.identityHashCode(minecraft.level),
                minecraft.player == null ? 0 : System.identityHashCode(minecraft.player),
                minecraft.gameMode == null ? 0 : System.identityHashCode(minecraft.gameMode),
                connection == null ? 0 : System.identityHashCode(connection),
                levelRendererLevel == null ? 0 : System.identityHashCode(levelRendererLevel),
                dispatcher == null ? 0 : System.identityHashCode(dispatcher)
            );
        }

        boolean hasSignal() {
            if (this.expectedSlot < 0) {
                return false;
            }
            return isMismatch(this.activeSlot)
                || isMismatch(this.runtimeSlot)
                || isMismatch(this.minecraftLevelSlot)
                || isMismatch(this.minecraftPlayerSlot)
                || isMismatch(this.minecraftGameModeSlot)
                || isMismatch(this.minecraftConnectionSlot)
                || isMismatch(this.levelRendererSlot)
                || isMismatch(this.sectionDispatcherSlot);
        }

        private boolean isMismatch(int actualSlot) {
            return actualSlot >= 0 && actualSlot != this.expectedSlot;
        }

        String toLogLine() {
            return String.format(
                Locale.ROOT,
                "phase=%s expected=%d active=%d scheduled=%d runtime=%d mc=l%d/p%d/g%d/cam%d/pick%d conn=%d primary=%s renderer=%d dispatcher=%d ids=l%08x/p%08x/g%08x/c%08x/lr%08x/sd%08x signal=%s",
                this.phase,
                this.expectedSlot,
                this.activeSlot,
                this.scheduledSlot,
                this.runtimeSlot,
                this.minecraftLevelSlot,
                this.minecraftPlayerSlot,
                this.minecraftGameModeSlot,
                this.minecraftCameraEntitySlot,
                this.minecraftCrosshairEntitySlot,
                this.minecraftConnectionSlot,
                this.minecraftConnectionPrimary,
                this.levelRendererSlot,
                this.sectionDispatcherSlot,
                this.levelId,
                this.playerId,
                this.gameModeId,
                this.connectionId,
                this.levelRendererLevelId,
                this.sectionDispatcherId,
                this.hasSignal()
            );
        }
    }

    public record ChunkRouteInfo(
        String event,
        int activeSlot,
        int listenerSlot,
        int levelSlot,
        int cacheSlot,
        int resolvedSlot,
        boolean minecraftLevel,
        int serverRadius,
        int centerX,
        int centerZ,
        int chunkRadius,
        int viewRange,
        int loadedChunks,
        int playerChunkX,
        int playerChunkZ,
        int listenerId,
        int levelId,
        int cacheId
    ) {
        private boolean wrongLevel() {
            return this.listenerSlot >= 0 && this.levelSlot >= 0 && this.listenerSlot != this.levelSlot;
        }

        private boolean wrongCache() {
            return this.levelSlot >= 0 && this.cacheSlot >= 0 && this.levelSlot != this.cacheSlot;
        }

        private boolean activeMismatch() {
            return this.activeSlot >= 0 && this.listenerSlot >= 0 && this.activeSlot != this.listenerSlot;
        }

        private boolean primaryGlobalLevelForSecondary() {
            return this.resolvedSlot > 0 && this.minecraftLevel;
        }
    }

    private static final class ChunkRouteAggregate {
        private final int slotId;
        private String lastEvent = "none";
        private int total;
        private int present;
        private int absent;
        private int wrongLevel;
        private int wrongCache;
        private int activeMismatch;
        private int primaryGlobalLevel;
        private int offCenter;
        private int maxCenterDistance;
        private int maxPlayerDistance;
        private int lastChunkX;
        private int lastChunkZ;
        private int lastActiveSlot;
        private int lastListenerSlot;
        private int lastLevelSlot;
        private int lastCacheSlot;
        private int lastResolvedSlot;
        private int lastServerRadius;
        private int lastCenterX;
        private int lastCenterZ;
        private int lastChunkRadius;
        private int lastViewRange;
        private int lastLoadedChunks;
        private int lastPlayerChunkX;
        private int lastPlayerChunkZ;
        private int lastListenerId;
        private int lastLevelId;
        private int lastCacheId;

        private ChunkRouteAggregate(int slotId) {
            this.slotId = slotId;
        }

        private void add(ChunkRouteInfo route, int chunkX, int chunkZ, boolean chunkPresent) {
            this.total++;
            this.lastEvent = route.event();
            this.lastChunkX = chunkX;
            this.lastChunkZ = chunkZ;
            this.lastActiveSlot = route.activeSlot();
            this.lastListenerSlot = route.listenerSlot();
            this.lastLevelSlot = route.levelSlot();
            this.lastCacheSlot = route.cacheSlot();
            this.lastResolvedSlot = route.resolvedSlot();
            this.lastServerRadius = route.serverRadius();
            this.lastCenterX = route.centerX();
            this.lastCenterZ = route.centerZ();
            this.lastChunkRadius = route.chunkRadius();
            this.lastViewRange = route.viewRange();
            this.lastLoadedChunks = route.loadedChunks();
            this.lastPlayerChunkX = route.playerChunkX();
            this.lastPlayerChunkZ = route.playerChunkZ();
            this.lastListenerId = route.listenerId();
            this.lastLevelId = route.levelId();
            this.lastCacheId = route.cacheId();
            if (chunkPresent) {
                this.present++;
            } else {
                this.absent++;
            }
            if (route.wrongLevel()) {
                this.wrongLevel++;
            }
            if (route.wrongCache()) {
                this.wrongCache++;
            }
            if (route.activeMismatch()) {
                this.activeMismatch++;
            }
            if (route.primaryGlobalLevelForSecondary()) {
                this.primaryGlobalLevel++;
            }
            if (route.centerX() != Integer.MIN_VALUE && route.centerZ() != Integer.MIN_VALUE) {
                int distance = Math.max(Math.abs(chunkX - route.centerX()), Math.abs(chunkZ - route.centerZ()));
                this.maxCenterDistance = Math.max(this.maxCenterDistance, distance);
                int limit = route.viewRange() >= 0 ? route.viewRange() : route.chunkRadius();
                if (limit >= 0 && distance > limit + 1) {
                    this.offCenter++;
                }
            }
            if (route.playerChunkX() != Integer.MIN_VALUE && route.playerChunkZ() != Integer.MIN_VALUE) {
                int playerDistance = Math.max(Math.abs(chunkX - route.playerChunkX()), Math.abs(chunkZ - route.playerChunkZ()));
                this.maxPlayerDistance = Math.max(this.maxPlayerDistance, playerDistance);
            }
        }

        private int total() {
            return this.total;
        }

        private boolean hasSignal() {
            return this.total > 0 && (
                this.wrongLevel > 0
                    || this.wrongCache > 0
                    || this.activeMismatch > 0
                    || this.primaryGlobalLevel > 0
                    || this.offCenter > 0
                    || this.absent > 0
                    || this.lastEvent.contains("drop")
                    || this.lastEvent.contains("forget")
                    || this.lastEvent.contains("center")
                    || this.lastEvent.contains("radius")
            );
        }

        private String toLogLine() {
            return "slot=" + this.slotId
                + " event=" + this.lastEvent
                + " total=" + this.total
                + " present=" + this.present
                + " absent=" + this.absent
                + " wrongLevel=" + this.wrongLevel
                + " wrongCache=" + this.wrongCache
                + " activeMismatch=" + this.activeMismatch
                + " primaryGlobal=" + this.primaryGlobalLevel
                + " offCenter=" + this.offCenter
                + " maxCenterDist=" + this.maxCenterDistance
                + " maxPlayerDist=" + this.maxPlayerDistance
                + " lastChunk=" + this.lastChunkX + "/" + this.lastChunkZ
                + " slots=a" + this.lastActiveSlot + "/ltn" + this.lastListenerSlot + "/lvl" + this.lastLevelSlot + "/cache" + this.lastCacheSlot + "/res" + this.lastResolvedSlot
                + " radius=server" + this.lastServerRadius + "/chunk" + this.lastChunkRadius + "/view" + this.lastViewRange
                + " center=" + this.lastCenterX + "/" + this.lastCenterZ
                + " playerChunk=" + this.lastPlayerChunkX + "/" + this.lastPlayerChunkZ
                + " loaded=" + this.lastLoadedChunks
                + " ids=ltn" + Integer.toHexString(this.lastListenerId) + "/lvl" + Integer.toHexString(this.lastLevelId) + "/cache" + Integer.toHexString(this.lastCacheId);
        }

        private void reset() {
            this.lastEvent = "none";
            this.total = 0;
            this.present = 0;
            this.absent = 0;
            this.wrongLevel = 0;
            this.wrongCache = 0;
            this.activeMismatch = 0;
            this.primaryGlobalLevel = 0;
            this.offCenter = 0;
            this.maxCenterDistance = 0;
            this.maxPlayerDistance = 0;
        }
    }

    private record Snapshot(
        long frameId,
        int slotId,
        boolean hasLevel,
        boolean hasPlayer,
        boolean hasGameMode,
        int viewKeys,
        int entries,
        int referenced,
        int unmaterialized,
        int sections,
        int pending,
        int ownerWait,
        int materializeLimit,
        int materialized,
        int noOwner,
        int stale,
        int visibleSections,
        int solid,
        int cutoutMipped,
        int cutout,
        int translucent,
        int tripwire,
        int compiledVisible,
        int nonEmptyVisible,
        int dirtyQueued,
        int activeTasks,
        int toBatch,
        int toUpload,
        int freeBuffers
    ) {
        private String toLogLine() {
            return "slot=" + this.slotId
                + " frame=" + this.frameId
                + " bound=" + flag(this.hasLevel) + "/" + flag(this.hasPlayer) + "/" + flag(this.hasGameMode)
                + " keys=" + this.viewKeys
                + " entries=" + this.entries
                + " refs=" + this.referenced
                + " unmat=" + this.unmaterialized
                + " sections=" + this.sections
                + " pending=" + this.pending
                + " wait=" + this.ownerWait
                + " mat=" + this.materialized + "/" + this.materializeLimit
                + " noOwner=" + this.noOwner
                + " stale=" + this.stale
                + " visible=" + this.visibleSections
                + " compiled=" + this.compiledVisible
                + " nonEmpty=" + this.nonEmptyVisible
                + " layers=" + this.solid + "/" + this.cutoutMipped + "/" + this.cutout + "/" + this.translucent + "/" + this.tripwire
                + " dirty=" + this.dirtyQueued
                + " tasks=" + this.activeTasks
                + " batch=" + this.toBatch
                + " upload=" + this.toUpload
                + " free=" + this.freeBuffers;
        }
    }
}
