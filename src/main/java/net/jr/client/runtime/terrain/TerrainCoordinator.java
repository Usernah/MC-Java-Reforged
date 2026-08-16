package net.jr.client.runtime.terrain;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.bridge.LevelRendererRuntimeAccessor;
import net.jr.client.runtime.context.ActiveClientSlot;
import net.jr.client.runtime.context.LocalClientAcces;
import net.jr.client.runtime.context.LocalClientExecution;
import net.jr.client.runtime.render.state.WorldEngineStateScope;
import net.jr.client.runtime.bridge.LevelRendererStateAccess;
import net.jr.client.runtime.slot.LocalClientSlot;
import net.jr.client.runtime.slot.LocalClientSlots;
import net.jr.client.runtime.state.TerrainState;
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
        SharedTerrainStore store = ensureStore(dispatcher);
        int slotId = activeSlotId();
        TerrainState terrain = ClientRuntime.INSTANCE.slots().slot(slotId).renderState().terrain();
        if (terrain.sectionOcclusionGraph() != graph) {
            throw new IllegalStateException("LevelRenderer terrain graph does not belong to active slot " + slotId);
        }
        return terrain.ensureViewArea(store, level, viewDistance, slotId);
    }

    public static void captureViewArea(@Nullable ViewArea viewArea) {
        TerrainState terrain = LevelRendererStateAccess.terrain();
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
        LevelRendererStateAccess.terrain().releaseViewArea();
    }

    /**
     * Starts one vanilla extraction cycle shared by every local player.
     *
     * <p>Creating a missing logical view is local to one player and must not
     * discard the shared compiled meshes. A real hard invalidation, such as a
     * render-distance or resource change, rebuilds every player's dirty tracker
     * before the one shared mesh pool is reset.</p>
     */
    public static void beginExtractionFrame(List<LocalClientSlot> extractionSlots) {
        sharedGeometryRebuildRequestedThisExtraction = false;
        sharedGeometryInvalidatedThisExtraction = false;
        sharedCompileQueueClearedThisExtraction = false;

        int viewDistance = Minecraft.getInstance().options.getEffectiveRenderDistance();
        SharedTerrainStore store = LevelRendererStateAccess.nullableTerrainStore();
        Set<ClientLevel> clearedLevels = java.util.Collections.newSetFromMap(new IdentityHashMap<>());

        // A dimension/ClientLevel replacement is local to the player. Consume it
        // before looking for global invalidations so vanilla's setLevel flags do
        // not turn one player's transition into a reset of the shared mesh pool.
        for (LocalClientSlot slot : extractionSlots) {
            var extraction = slot.renderState().levelExtractionState();
            if (!extraction.consumeLocalLevelResetRequest()) {
                continue;
            }
            if (store != null) {
                prepareLocalLevelState(slot, store, viewDistance, clearedLevels);
            }
        }

        boolean requiresSharedRebuild = false;
        for (LocalClientSlot slot : extractionSlots) {
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

        if (requiresSharedRebuild) {
            sharedGeometryRebuildRequestedThisExtraction = true;
            for (LocalClientSlot slot : extractionSlots) {
                prepareAllChangedState(slot, viewDistance, clearedLevels);
            }
            return;
        }

        // A player joining (or recreating only its logical view) starts with a
        // fresh dirty tracker, while existing players keep their compiled mesh
        // and pending terrain work untouched.
        for (LocalClientSlot slot : extractionSlots) {
            if (slot.renderState().terrain().viewArea() == null) {
                prepareAllChangedState(slot, viewDistance, clearedLevels);
            }
        }
    }

    /**
     * Rebinds one player's logical terrain window to its new ClientLevel while
     * retaining the single dispatcher and the compiled sections used by every
     * other player.
     */
    private static void prepareLocalLevelState(
        LocalClientSlot slot,
        SharedTerrainStore store,
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

        TerrainState terrain = slot.renderState().terrain();
        terrain.releaseViewArea();
        TerrainGraphArea viewArea = terrain.ensureViewArea(store, level, viewDistance, slot.id());
        SectionPos cameraSectionPos = SectionPos.of(slot.renderState().camera().position());
        viewArea.repositionCamera(cameraSectionPos);
        terrain.setCameraPosition(slot.renderState().camera().position());
        slot.renderState().levelRenderState().reset();

        SectionUpdateTracker tracker = new SectionUpdateTracker(level, viewDistance);
        tracker.repositionCamera(cameraSectionPos);
        var extraction = slot.renderState().levelExtractionState();
        extraction.setSectionUpdateTracker(tracker);
        extraction.setPrevCamRotX(Double.MIN_VALUE);
        extraction.setPrevCamRotY(Double.MIN_VALUE);
        extraction.setLastViewDistance(viewDistance);
        extraction.setShouldInvalidateCompiledGeometry(false);
        extraction.setShouldResetLevelRenderData(false);
    }

    /**
     * Mirrors {@link net.minecraft.client.renderer.extract.LevelExtractor#allChanged()}
     * into every player-owned extraction state when vanilla invokes it outside a
     * player scope (for example after a resource reload).
     */
    public static boolean routeGlobalAllChanged() {
        if (ActiveClientSlot.idOrNull() != null) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        int viewDistance = minecraft.options.getEffectiveRenderDistance();
        Set<ClientLevel> clearedLevels = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        boolean routed = false;
        for (int slotId = 0; slotId < LocalClientSlots.MAX_SLOTS; slotId++) {
            LocalClientSlot slot = ClientRuntime.INSTANCE.slots().slot(slotId);
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

        SharedTerrainStore store = LevelRendererStateAccess.nullableTerrainStore();
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
        SharedTerrainStore store = LevelRendererStateAccess.nullableTerrainStore();
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
        if (!TerrainWorkPhase.canUpdateTerrain()) {
            return;
        }
        LevelRendererRuntimeAccessor renderer = (LevelRendererRuntimeAccessor)levelRenderer;
        SectionRenderDispatcher dispatcher = renderer.splitTest$getSectionRenderDispatcher();
        if (dispatcher == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Map<TerrainKey, SectionUpdateRenderState> selectedUpdates = selectUniqueUpdates();
        int driverSlotId = ActiveClientSlot.requireId();
        var driverCamera = ClientRuntime.INSTANCE.slots().slot(driverSlotId)
            .renderState().levelRenderState().cameraRenderState.pos;
        try {
            for (LocalClientSlot slot : ClientRuntime.INSTANCE.slots().visibleSlots()) {
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
                        LocalClientExecution.Scope ignoredClient = LocalClientExecution.enterForSlot(minecraft, slot);
                        WorldEngineStateScope ignoredEngines = WorldEngineStateScope.bindTerrainOnly(minecraft, slot)
                ) {
                    var cameraState = slot.renderState().levelRenderState().cameraRenderState;
                    slot.renderState().terrain().setCameraPosition(cameraState.pos);
                    // Preserve Vanilla's ordering invariant: the ViewArea must cover the camera
                    // that produced these section updates before compileSections resolves them.
                    renderer.splitTest$repositionCamera(cameraState);
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
        for (LocalClientSlot slot : ClientRuntime.INSTANCE.slots().visibleSlots()) {
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
        for (int slotId = 0; slotId < LocalClientSlots.MAX_SLOTS; slotId++) {
            TerrainState terrain = ClientRuntime.INSTANCE.slots().slot(slotId).renderState().terrain();
            if (terrain.contains(section)) {
                terrain.sectionOcclusionGraph().schedulePropagationFrom(section);
            }
        }
    }

    public static boolean isPrimaryTerrainPass() {
        return TerrainWorkPhase.canUpdateTerrain();
    }

    private static SharedTerrainStore ensureStore(SectionRenderDispatcher dispatcher) {
        SharedTerrainStore current = LevelRendererStateAccess.nullableTerrainStore();
        if (current != null && current.dispatcher() == dispatcher) {
            return current;
        }
        if (current != null) {
            releaseAllViews();
            current.close();
        }
        SharedTerrainStore created = new SharedTerrainStore(dispatcher);
        LevelRendererStateAccess.setTerrainStore(created);
        return created;
    }

    public static void closeSharedTerrain() {
        SharedTerrainStore current = LevelRendererStateAccess.nullableTerrainStore();
        if (current == null) {
            return;
        }
        releaseAllViews();
        current.close();
        LevelRendererStateAccess.setTerrainStore(null);
    }

    private static void releaseAllViews() {
        for (int slotId = 0; slotId < LocalClientSlots.MAX_SLOTS; slotId++) {
            ClientRuntime.INSTANCE.slots().slot(slotId).renderState().terrain().releaseViewArea();
        }
    }

    private static TerrainKey key(ClientLevel level, SectionUpdateRenderState update) {
        long node = update.sectionNode();
        return new TerrainKey(level.dimension(), SectionPos.x(node), SectionPos.y(node), SectionPos.z(node));
    }

    private static void prepareAllChangedState(
        LocalClientSlot slot,
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
        Integer active = ActiveClientSlot.idOrNull();
        return active != null ? active : LocalClientAcces.slotId();
    }

    private static boolean canRender(LocalClientSlot slot) {
        return slot.drawable()
            && slot.renderState().level() != null
            && slot.gameplayState().player() != null
            && slot.gameplayState().gameMode() != null
            && !ClientRuntime.INSTANCE.sessions().isJoining(slot.id());
    }
}
