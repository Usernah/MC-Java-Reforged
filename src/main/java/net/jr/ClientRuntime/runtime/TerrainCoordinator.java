package net.jr.ClientRuntime.runtime;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.jr.ClientRuntime.bridge.LevelRendererSSAccessor;
import net.jr.ClientRuntime.slot.PlayerSlot;
import net.jr.ClientRuntime.slot.PlayerSlots;
import net.jr.ClientRuntime.state.TerrainState;
import net.jr.ClientRuntime.terrain.GlobalTerrainStore;
import net.jr.ClientRuntime.terrain.TerrainGraphArea;
import net.jr.ClientRuntime.terrain.TerrainKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.SectionUpdateTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.state.level.SectionUpdateRenderState;
import net.minecraft.core.SectionPos;

/** Coordinates vanilla 26.2 terrain work without duplicating its render engine. */
public final class TerrainCoordinator {
    private static boolean sharedGeometryRebuildRequestedThisExtraction;
    private static boolean sharedGeometryInvalidatedThisExtraction;
    private static boolean sharedCompileQueueClearedThisExtraction;

    private TerrainCoordinator() {
    }

    public static ViewArea createViewArea(
        SectionRenderDispatcher dispatcher,
        ClientLevel level,
        int viewDistance,
        SectionOcclusionGraph graph
    ) {
        GlobalTerrainStore store = ensureStore(dispatcher);
        int slotId = activeSlotId();
        TerrainState terrain = LocalPlayers.INSTANCE.slots().slot(slotId).renderState().terrain();
        if (terrain.sectionOcclusionGraph() != graph) {
            throw new IllegalStateException("LevelRenderer terrain graph does not belong to active slot " + slotId);
        }
        return terrain.ensureViewArea(store, level, viewDistance, slotId);
    }

    public static void captureViewArea(@Nullable ViewArea viewArea) {
        TerrainState terrain = LevelRendererFields.terrain();
        if (viewArea == null) {
            terrain.setViewArea(null);
            return;
        }
        if (!(viewArea instanceof TerrainGraphArea terrainArea)) {
            throw new IllegalStateException("Split terrain received a non-shared ViewArea");
        }
        terrain.setViewArea(terrainArea);
    }

    public static void resetActiveSlotTerrain() {
        LevelRendererFields.terrain().releaseViewArea();
    }

    /**
     * Starts one vanilla extraction cycle shared by every local player.
     *
     * <p>Creating a missing logical view is local to one player and must not
     * discard the shared compiled meshes. A real hard invalidation, such as a
     * render-distance or resource change, rebuilds every player's dirty tracker
     * before the one shared mesh pool is reset.</p>
     */
    public static void beginExtractionFrame(List<PlayerSlot> extractionSlots) {
        sharedGeometryRebuildRequestedThisExtraction = false;
        sharedGeometryInvalidatedThisExtraction = false;
        sharedCompileQueueClearedThisExtraction = false;

        int viewDistance = Minecraft.getInstance().options.getEffectiveRenderDistance();
        boolean requiresSharedRebuild = false;
        for (PlayerSlot slot : extractionSlots) {
            var extraction = slot.renderState().levelExtractionState();
            if (
                slot.renderState().terrain().viewArea() != null
                    && (
                        extraction.shouldInvalidateCompiledGeometry()
                            || extraction.lastViewDistance() != viewDistance
                    )
            ) {
                requiresSharedRebuild = true;
                break;
            }
        }

        Set<ClientLevel> clearedLevels = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        if (requiresSharedRebuild) {
            sharedGeometryRebuildRequestedThisExtraction = true;
            for (PlayerSlot slot : extractionSlots) {
                prepareAllChangedState(slot, viewDistance, clearedLevels);
            }
            return;
        }

        // A player joining (or recreating only its logical view) starts with a
        // fresh dirty tracker, while existing players keep their compiled mesh
        // and pending terrain work untouched.
        for (PlayerSlot slot : extractionSlots) {
            if (slot.renderState().terrain().viewArea() == null) {
                prepareAllChangedState(slot, viewDistance, clearedLevels);
            }
        }
    }

    /**
     * Mirrors {@link net.minecraft.client.renderer.extract.LevelExtractor#allChanged()}
     * into every player-owned extraction state when vanilla invokes it outside a
     * player scope (for example after a resource reload).
     */
    public static boolean routeGlobalAllChanged() {
        if (ActiveSlot.idOrNull() != null) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        int viewDistance = minecraft.options.getEffectiveRenderDistance();
        Set<ClientLevel> clearedLevels = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        boolean routed = false;
        for (int slotId = 0; slotId < PlayerSlots.MAX_SLOTS; slotId++) {
            PlayerSlot slot = LocalPlayers.INSTANCE.slots().slot(slotId);
            ClientLevel level = slot.renderState().level();
            if (level == null) {
                continue;
            }

            prepareAllChangedState(slot, viewDistance, clearedLevels);
            routed = true;
        }
        return routed;
    }

    /**
     * Performs vanilla's hard compiled-geometry invalidation once for the shared
     * terrain engine. Logical views are released first so no player retains a
     * section that is about to have its mesh reset.
     */
    public static void invalidateSharedCompiledGeometry() {
        if (!sharedGeometryRebuildRequestedThisExtraction) {
            return;
        }
        if (sharedGeometryInvalidatedThisExtraction) {
            return;
        }
        sharedGeometryInvalidatedThisExtraction = true;

        GlobalTerrainStore store = LevelRendererFields.nullableTerrainStore();
        if (store == null) {
            return;
        }

        releaseAllViews();
        store.invalidateCompiledGeometry();
    }

    /** Clears vanilla's one terrain compilation queue once per shared invalidation cycle. */
    public static void clearSharedCompileQueueOnce(SectionRenderDispatcher dispatcher) {
        if (!sharedGeometryRebuildRequestedThisExtraction) {
            return;
        }
        if (sharedCompileQueueClearedThisExtraction) {
            return;
        }
        sharedCompileQueueClearedThisExtraction = true;
        dispatcher.clearCompileQueue();
    }

    public static void prepareFrame() {
        GlobalTerrainStore store = LevelRendererFields.nullableTerrainStore();
        if (store != null) {
            store.beginFrame();
        }
    }

    /**
     * Runs Minecraft's own compileSections method for every extracted player.
     * The call is made only from the frame's terrain-driver render pass; duplicated coordinates are
     * removed before scheduling so one shared RenderSection is never compiled twice.
     */
    public static void compileVisibleSlots(LevelRenderer levelRenderer) {
        if (!TerrainPhase.canUpdateTerrain()) {
            return;
        }
        LevelRendererSSAccessor renderer = (LevelRendererSSAccessor)levelRenderer;
        SectionRenderDispatcher dispatcher = renderer.splitTest$getSectionRenderDispatcher();
        if (dispatcher == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Map<TerrainKey, SectionUpdateRenderState> selectedUpdates = selectUniqueUpdates();
        int driverSlotId = ActiveSlot.requireId();
        var driverCamera = LocalPlayers.INSTANCE.slots().slot(driverSlotId)
            .renderState().levelRenderState().cameraRenderState.pos;
        try {
            for (PlayerSlot slot : LocalPlayers.INSTANCE.slots().visibleSlots()) {
                if (!canRender(slot)) {
                    continue;
                }
                ClientLevel level = slot.renderState().level();
                var updates = slot.renderState().levelRenderState().sectionUpdateRenderStates;
                updates.removeIf(update -> selectedUpdates.get(key(level, update)) != update);
                if (updates.isEmpty() && slot.renderState().terrain().visibleSections().isEmpty()) {
                    continue;
                }

                try (
                    ClientBoundary.Scope ignoredClient = ClientBoundary.enterForSlot(minecraft, slot);
                    WorldEngineStateScope ignoredEngines = WorldEngineStateScope.bindTerrainOnly(minecraft, slot)
                ) {
                    var cameraState = slot.renderState().levelRenderState().cameraRenderState;
                    slot.renderState().terrain().setCameraPosition(cameraState.pos);
                    dispatcher.setCameraPosition(cameraState.pos);
                    renderer.splitTest$compileSections(cameraState);
                }
            }
        } finally {
            // Vanilla owns one priority queue; restore the camera of this frame's sole authority.
            dispatcher.setCameraPosition(driverCamera);
        }
    }

    private static Map<TerrainKey, SectionUpdateRenderState> selectUniqueUpdates() {
        Map<TerrainKey, SectionUpdateRenderState> selected = new HashMap<>();
        for (PlayerSlot slot : LocalPlayers.INSTANCE.slots().visibleSlots()) {
            ClientLevel level = slot.renderState().level();
            if (level == null) {
                continue;
            }
            for (SectionUpdateRenderState update : slot.renderState().levelRenderState().sectionUpdateRenderStates) {
                TerrainKey key = key(level, update);
                SectionUpdateRenderState current = selected.get(key);
                if (current == null || !current.playerChanged() && update.playerChanged()) {
                    selected.put(key, update);
                }
            }
        }
        return selected;
    }

    public static void onSectionCompiled(SectionRenderDispatcher.RenderSection section) {
        for (int slotId = 0; slotId < PlayerSlots.MAX_SLOTS; slotId++) {
            TerrainState terrain = LocalPlayers.INSTANCE.slots().slot(slotId).renderState().terrain();
            if (terrain.contains(section)) {
                terrain.sectionOcclusionGraph().schedulePropagationFrom(section);
            }
        }
    }

    public static boolean isPrimaryTerrainPass() {
        return TerrainPhase.canUpdateTerrain();
    }

    private static GlobalTerrainStore ensureStore(SectionRenderDispatcher dispatcher) {
        GlobalTerrainStore current = LevelRendererFields.nullableTerrainStore();
        if (current != null && current.dispatcher() == dispatcher) {
            return current;
        }
        if (current != null) {
            releaseAllViews();
            current.close();
        }
        GlobalTerrainStore created = new GlobalTerrainStore(dispatcher);
        LevelRendererFields.setTerrainStore(created);
        return created;
    }

    public static void closeSharedTerrain() {
        GlobalTerrainStore current = LevelRendererFields.nullableTerrainStore();
        if (current == null) {
            return;
        }
        releaseAllViews();
        current.close();
        LevelRendererFields.setTerrainStore(null);
    }

    private static void releaseAllViews() {
        for (int slotId = 0; slotId < PlayerSlots.MAX_SLOTS; slotId++) {
            LocalPlayers.INSTANCE.slots().slot(slotId).renderState().terrain().releaseViewArea();
        }
    }

    private static TerrainKey key(ClientLevel level, SectionUpdateRenderState update) {
        long node = update.sectionNode();
        return new TerrainKey(level.dimension(), SectionPos.x(node), SectionPos.y(node), SectionPos.z(node));
    }

    private static void prepareAllChangedState(
        PlayerSlot slot,
        int viewDistance,
        Set<ClientLevel> clearedLevels
    ) {
        ClientLevel level = slot.renderState().level();
        if (level == null) {
            return;
        }
        if (clearedLevels.add(level)) {
            level.clearTintCaches();
        }

        SectionUpdateTracker tracker = new SectionUpdateTracker(level, viewDistance);
        tracker.repositionCamera(SectionPos.of(slot.renderState().camera().position()));
        var extraction = slot.renderState().levelExtractionState();
        extraction.setSectionUpdateTracker(tracker);
        extraction.setLastViewDistance(viewDistance);
        extraction.setShouldInvalidateCompiledGeometry(true);
    }

    private static int activeSlotId() {
        Integer active = ActiveSlot.idOrNull();
        return active != null ? active : Client.slotId();
    }

    private static boolean canRender(PlayerSlot slot) {
        return slot.drawable()
            && slot.renderState().level() != null
            && slot.gameplayState().player() != null
            && slot.gameplayState().gameMode() != null
            && !LocalPlayers.INSTANCE.sessions().isJoining(slot.id());
    }
}
