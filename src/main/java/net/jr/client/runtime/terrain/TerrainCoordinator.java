package net.jr.client.runtime.terrain;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

import net.jr.api.client.local.Client;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.slot.ClientSlots;
import net.jr.client.runtime.slot.Slots;
import net.jr.mixin.accessors.LevelRendererAccessor;
import net.jr.client.runtime.context.SlotScope;
import net.jr.client.runtime.client.ClientInstance;
import net.jr.client.runtime.client.ReadinessPolicy;
import net.jr.client.runtime.context.InstanceExecution;
import net.jr.client.runtime.render.state.WorldEngineStateScope;
import net.jr.client.runtime.render.experimental.TerrainMultiDraw;
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

    public static ViewArea createActiveViewArea(
            SectionRenderDispatcher dispatcher,
            int viewDistance,
            SectionOcclusionGraph graph
    ) {
        ClientLevel level = Client.current().level();
        if (level == null) {
            throw new IllegalStateException("Cannot create terrain ViewArea without an active ClientLevel");
        }
        return createViewArea(dispatcher, level, viewDistance, graph);
    }

    public static void captureViewArea(@Nullable ViewArea viewArea) {
        TerrainState terrain = TerrainStateStore.terrain();
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
        TerrainMultiDraw.invalidateSlot(activeSlotId());
        TerrainStateStore.terrain().releaseViewArea();
    }

    public static boolean resetActiveSlotTerrainIfScoped() {
        if (SlotScope.idOrNull() == null) {
            return false;
        }
        resetActiveSlotTerrain();
        return true;
    }

    public static void uploadSharedTerrain(SectionRenderDispatcher dispatcher) {
        if (TerrainWorkPhase.canUpdateTerrain()) {
            dispatcher.uploadTerrainBuffersToGpu();
        }
    }

    public static void beginExtractionFrame(List<Slots> extractionSlots) {
        sharedGeometryRebuildRequestedThisExtraction = false;
        sharedGeometryInvalidatedThisExtraction = false;
        sharedCompileQueueClearedThisExtraction = false;

        int viewDistance = Minecraft.getInstance().options.getEffectiveRenderDistance();
        SharedTerrainStore store = TerrainStateStore.nullableTerrainStore();
        Set<ClientLevel> clearedLevels = java.util.Collections.newSetFromMap(new IdentityHashMap<>());

        for (Slots slot : extractionSlots) {
            var extraction = slot.renderState().levelExtractionState();
            if (!extraction.consumeLocalLevelResetRequest()) {
                continue;
            }
            if (store != null) {
                prepareLocalLevelState(slot, store, viewDistance, clearedLevels);
            }
        }

        boolean requiresSharedRebuild = false;
        for (Slots slot : extractionSlots) {
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
            for (Slots slot : extractionSlots) {
                prepareAllChangedState(slot, viewDistance, clearedLevels);
            }
            return;
        }

        for (Slots slot : extractionSlots) {
            if (slot.renderState().terrain().viewArea() == null) {
                prepareAllChangedState(slot, viewDistance, clearedLevels);
            }
        }
    }

    private static void prepareLocalLevelState(
        Slots slot,
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
        TerrainMultiDraw.invalidateSlot(slot.id());
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

        return requestGlobalGeometryRebuild();
    }

    public static boolean requestGlobalGeometryRebuild() {
        Minecraft minecraft = Minecraft.getInstance();
        int viewDistance = minecraft.options.getEffectiveRenderDistance();
        Set<ClientLevel> clearedLevels = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        boolean routed = false;
        for (int slotId = 0; slotId < ClientSlots.MAX_SLOTS; slotId++) {
            Slots slot = ClientRuntime.INSTANCE.slots().slot(slotId);
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

        SharedTerrainStore store = TerrainStateStore.nullableTerrainStore();
        if (store == null) {
            return;
        }

        store.invalidateCompiledGeometry();
        invalidateAllViewGraphs();
    }

    public static void clearSharedCompileQueueOnce(SectionRenderDispatcher dispatcher) {
        if (!sharedGeometryRebuildRequestedThisExtraction || sharedCompileQueueClearedThisExtraction) {
            return;
        }
        sharedCompileQueueClearedThisExtraction = true;
        dispatcher.clearCompileQueue();
    }

    public static void prepareFrame() {
        TerrainMultiDraw.beginFrame();
        SharedTerrainStore store = TerrainStateStore.nullableTerrainStore();
        if (store != null) {
            store.beginFrame();
        }
    }

    public static void compileVisibleSlots(LevelRenderer levelRenderer) {
        if (!TerrainWorkPhase.canUpdateTerrain()) {
            return;
        }
        LevelRendererAccessor renderer = (LevelRendererAccessor)levelRenderer;
        SectionRenderDispatcher dispatcher = renderer.javaReforged$getSectionRenderDispatcher();
        if (dispatcher == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        Map<TerrainKey, SectionUpdateRenderState> selectedUpdates = selectUniqueUpdates();
        int driverSlotId = SlotScope.requireId();
        var driverCamera = ClientRuntime.INSTANCE.slots().slot(driverSlotId)
            .renderState().levelRenderState().cameraRenderState.pos;
        try {
            for (Slots slot : ClientRuntime.INSTANCE.viewports().drawableSlots()) {
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
                        InstanceExecution.Scope ignoredClient = InstanceExecution.enterForClient(minecraft, slot.id());
                        WorldEngineStateScope ignoredEngines = WorldEngineStateScope.bindTerrainOnly(minecraft, slot)
                ) {
                    var cameraState = slot.renderState().levelRenderState().cameraRenderState;
                    slot.renderState().terrain().setCameraPosition(cameraState.pos);
                    renderer.javaReforged$repositionCamera(cameraState);
                    renderer.javaReforged$compileSections(cameraState);
                }
            }
        } finally {
            dispatcher.setCameraPosition(driverCamera);
        }
    }

    private static Map<TerrainKey, SectionUpdateRenderState> selectUniqueUpdates() {
        Map<TerrainKey, SectionUpdateRenderState> selected = new HashMap<>();
        for (Slots slot : ClientRuntime.INSTANCE.viewports().drawableSlots()) {
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
        TerrainMultiDraw.invalidateSection(section);
        for (int slotId = 0; slotId < ClientSlots.MAX_SLOTS; slotId++) {
            TerrainState terrain = ClientRuntime.INSTANCE.slots().slot(slotId).renderState().terrain();
            if (terrain.contains(section)) {
                terrain.sectionOcclusionGraph().schedulePropagationFrom(section);
            }
        }
    }

    public static void onSectionBuffersChanged(SectionRenderDispatcher.RenderSection section) {
        TerrainMultiDraw.invalidateSection(section);
    }

    public static boolean isPrimaryTerrainPass() {
        return TerrainWorkPhase.canUpdateTerrain();
    }

    private static SharedTerrainStore ensureStore(SectionRenderDispatcher dispatcher) {
        SharedTerrainStore current = TerrainStateStore.nullableTerrainStore();
        if (current != null && current.dispatcher() == dispatcher) {
            return current;
        }
        if (current != null) {
            releaseAllViews();
            current.close();
        }
        SharedTerrainStore created = new SharedTerrainStore(dispatcher);
        TerrainStateStore.setTerrainStore(created);
        return created;
    }

    public static void closeSharedTerrain() {
        TerrainMultiDraw.close();
        SharedTerrainStore current = TerrainStateStore.nullableTerrainStore();
        if (current == null) {
            return;
        }
        releaseAllViews();
        current.close();
        TerrainStateStore.setTerrainStore(null);
    }

    private static void releaseAllViews() {
        for (int slotId = 0; slotId < ClientSlots.MAX_SLOTS; slotId++) {
            TerrainMultiDraw.invalidateSlot(slotId);
            ClientRuntime.INSTANCE.slots().slot(slotId).renderState().terrain().releaseViewArea();
        }
    }

    private static void invalidateAllViewGraphs() {
        for (int slotId = 0; slotId < ClientSlots.MAX_SLOTS; slotId++) {
            TerrainMultiDraw.invalidateSlot(slotId);
            TerrainState terrain = ClientRuntime.INSTANCE.slots().slot(slotId).renderState().terrain();
            if (terrain.viewArea() != null) {
                terrain.sectionOcclusionGraph().invalidate();
            }
        }
    }

    private static TerrainKey key(ClientLevel level, SectionUpdateRenderState update) {
        long node = update.sectionNode();
        return new TerrainKey(level.dimension(), SectionPos.x(node), SectionPos.y(node), SectionPos.z(node));
    }

    private static void prepareAllChangedState(
        Slots slot,
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
                : Client.slotId();
    }

    private static boolean canRender(Slots slot) {
        ClientInstance client = ClientRuntime.INSTANCE.clients().clientOrNull(slot.id());
        return client != null
            && ClientRuntime.INSTANCE.viewports().hasViewport(slot.id())
            && ReadinessPolicy.gameplayReady(client);
    }
}
