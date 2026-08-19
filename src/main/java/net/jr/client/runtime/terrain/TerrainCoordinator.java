package net.jr.client.runtime.terrain;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.bridge.LevelRendererRuntimeAccessor;
import net.jr.client.runtime.context.LocalClientScope;
import net.jr.client.runtime.context.SlotScope;
import net.jr.client.runtime.client.LocalClient;
import net.jr.client.runtime.client.LocalClientReadinessPolicy;
import net.jr.client.runtime.context.LocalClientExecution;
import net.jr.client.runtime.render.state.WorldEngineStateScope;
import net.jr.client.runtime.bridge.LevelRendererStateAccess;
import net.jr.client.runtime.slot.LocalClientSlot;
import net.jr.client.runtime.slot.LocalClientSlotRegistry;
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

    public static void beginExtractionFrame(List<LocalClientSlot> extractionSlots) {
        sharedGeometryRebuildRequestedThisExtraction = false;
        sharedGeometryInvalidatedThisExtraction = false;
        sharedCompileQueueClearedThisExtraction = false;

        int viewDistance = Minecraft.getInstance().options.getEffectiveRenderDistance();
        SharedTerrainStore store = LevelRendererStateAccess.nullableTerrainStore();
        Set<ClientLevel> clearedLevels = java.util.Collections.newSetFromMap(new IdentityHashMap<>());

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
                    && (extraction.shouldInvalidateCompiledGeometry()
                        || extraction.lastViewDistance() != viewDistance)
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

        for (LocalClientSlot slot : extractionSlots) {
            if (slot.renderState().terrain().viewArea() == null) {
                prepareAllChangedState(slot, viewDistance, clearedLevels);
            }
        }
    }

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

    public static boolean routeGlobalAllChanged() {
        if (SlotScope.idOrNull() != null) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        int viewDistance = minecraft.options.getEffectiveRenderDistance();
        Set<ClientLevel> clearedLevels = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        boolean routed = false;
        for (int slotId = 0; slotId < LocalClientSlotRegistry.MAX_SLOTS; slotId++) {
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

    public static void invalidateSharedCompiledGeometry() {
        if (!sharedGeometryRebuildRequestedThisExtraction || sharedGeometryInvalidatedThisExtraction) {
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

    public static void clearSharedCompileQueueOnce(SectionRenderDispatcher dispatcher) {
        if (!sharedGeometryRebuildRequestedThisExtraction || sharedCompileQueueClearedThisExtraction) {
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
        int driverSlotId = SlotScope.requireId();
        var driverCamera = ClientRuntime.INSTANCE.slots().slot(driverSlotId)
            .renderState().levelRenderState().cameraRenderState.pos;
        try {
            for (LocalClientSlot slot : ClientRuntime.INSTANCE.viewports().drawableSlots()) {
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
                    LocalClientExecution.Scope ignoredClient = LocalClientExecution.enterForClient(minecraft, slot.id());
                    WorldEngineStateScope ignoredEngines = WorldEngineStateScope.bindTerrainOnly(minecraft, slot)
                ) {
                    var cameraState = slot.renderState().levelRenderState().cameraRenderState;
                    slot.renderState().terrain().setCameraPosition(cameraState.pos);
                    renderer.splitTest$repositionCamera(cameraState);
                    renderer.splitTest$compileSections(cameraState);
                }
            }
        } finally {
            dispatcher.setCameraPosition(driverCamera);
        }
    }

    private static Map<TerrainKey, SectionUpdateRenderState> selectUniqueUpdates() {
        Map<TerrainKey, SectionUpdateRenderState> selected = new HashMap<>();
        for (LocalClientSlot slot : ClientRuntime.INSTANCE.viewports().drawableSlots()) {
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
        for (int slotId = 0; slotId < LocalClientSlotRegistry.MAX_SLOTS; slotId++) {
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
        for (int slotId = 0; slotId < LocalClientSlotRegistry.MAX_SLOTS; slotId++) {
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
        Integer active = SlotScope.idOrNull();

        return active != null
                ? active
                : LocalClientScope.currentClient().slotId();
    }

    private static boolean canRender(LocalClientSlot slot) {
        LocalClient client = ClientRuntime.INSTANCE.clients().clientOrNull(slot.id());
        return client != null
            && ClientRuntime.INSTANCE.viewports().hasViewport(slot.id())
            && LocalClientReadinessPolicy.gameplayReady(client);
    }
}
