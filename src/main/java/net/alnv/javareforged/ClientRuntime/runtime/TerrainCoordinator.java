package net.alnv.javareforged.ClientRuntime.runtime;

import java.util.Collections;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.alnv.javareforged.mixin.SSM.RenderSectionSSAccessor;
import net.alnv.javareforged.ClientRuntime.slot.PlayerSlot;
import net.alnv.javareforged.ClientRuntime.slot.PlayerSlots;
import net.alnv.javareforged.ClientRuntime.state.TerrainState;
import net.alnv.javareforged.ClientRuntime.terrain.GlobalTerrainStore;
import net.alnv.javareforged.ClientRuntime.terrain.SlotTerrainView;
import net.alnv.javareforged.ClientRuntime.terrain.TerrainViewArea;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.phys.Vec3;

public final class TerrainCoordinator {
    private TerrainCoordinator() {
    }

    public static ViewArea createViewArea(
        SectionRenderDispatcher dispatcher,
        Level level,
        int viewDistance,
        LevelRenderer levelRenderer
    ) {
        if (!(level instanceof ClientLevel clientLevel)) {
            throw new IllegalStateException("Terrain requires ClientLevel, got " + level);
        }
        return new TerrainViewArea(dispatcher, clientLevel, viewDistance, levelRenderer);
    }

    public static void prepareFrame() {
        if (!LevelRendererFields.hasTerrainStore()) {
            return;
        }
        GlobalTerrainStore store = LevelRendererFields.terrainStore();
        store.beginFrame();
        for (int slotId = 0; slotId < PlayerSlots.MAX_SLOTS; slotId++) {
            PlayerSlot slot = LocalPlayers.INSTANCE.slots().slot(slotId);
            if (!canRender(slot)) {
                TerrainDebug.recordInactiveSlot(slot);
                slot.renderState().terrain().releaseGraph(store);
                store.releaseSlot(slotId, slot.renderState().terrain().view());
            }
        }
        store.finishRetirements();
    }

    public static void setup(Camera camera, Frustum frustum) {
        Minecraft minecraft = Minecraft.getInstance();
        int viewDistance = minecraft.options.getEffectiveRenderDistance();
        if (LevelRendererFields.viewArea().getViewDistance() != viewDistance) {
            minecraft.levelRenderer.allChanged();
        }
        PlayerSlot slot = LocalPlayers.INSTANCE.runtimeSlot();
        TerrainDebug.recordRawStateCheck("terrain.setup", slot.id(), minecraft);
        ClientLevel level = activeLevel();
        TerrainState terrain = slot.renderState().terrain();
        terrain.setCameraPosition(camera.getPosition());
        SlotTerrainView view = terrain.view();
        GlobalTerrainStore store = LevelRendererFields.terrainStore();
        SlotTerrainView.Update viewUpdate = null;
        if (view.needsUpdate(store, level, viewDistance, camera.getPosition().x, camera.getPosition().z)) {
            viewUpdate = view.update(
                store,
                level,
                viewDistance,
                camera.getPosition().x,
                camera.getPosition().y,
                camera.getPosition().z
            );
            boolean graphReleased = viewUpdate.reset() || !viewUpdate.removed().isEmpty();
            if (graphReleased) {
                terrain.releaseGraph(store);
                store.updateReferences(slot.id(), level, viewUpdate);
            } else {
                store.addReferences(slot.id(), level, viewUpdate.added());
            }
            TerrainDebug.recordViewUpdate(slot, camera.getPosition(), viewDistance, viewUpdate, graphReleased);
        }
        GlobalTerrainStore.MaterializationResult materialization = store.materializePending(
            TerrainWorkload.materializationAllowance(store.dispatcher())
        );
        if (materialization.materialized() > 0) {
            terrain.invalidateGraph();
        }
        store.dispatcher().setCamera(camera.getPosition());
        Frustum offsetFrustum = LevelRenderer.offsetFrustum(frustum);
        terrain.prepareGraph(store, level, viewDistance, minecraft.levelRenderer, slot.id());
        invalidateGraphWhenCameraMoves(terrain, camera);
        terrain.sectionOcclusionGraph().update(minecraft.smartCull, camera, frustum, terrain.visibleSections());
        boolean rebuildVisibility = terrain.consumeGraphUpdate() || consumeCameraRotationUpdate(terrain, camera);
        int visibilityBeforeFallback;
        int occlusionCandidateCount;
        if (rebuildVisibility) {
            terrain.occlusionCandidates().clear();
            terrain.sectionOcclusionGraph().addSectionsInFrustum(offsetFrustum, terrain.occlusionCandidates());
            occlusionCandidateCount = terrain.occlusionCandidates().size();

            var visibleScratch = terrain.visibleScratchSections();
            visibleScratch.clear();
            store.appendVisible(slot.id(), view, terrain.occlusionCandidates(), visibleScratch);
            bootstrapVisibleIfGraphIsEmpty(store, slot, view, offsetFrustum, terrain, visibleScratch);

            if (!visibleScratch.isEmpty() || terrain.visibleSections().isEmpty()) {
                terrain.replaceVisibleSections(visibleScratch);
            } else {
                store.retainVisible(slot.id(), view, terrain.visibleSections());
            }
            visibilityBeforeFallback = terrain.visibleSections().size();
        } else {
            occlusionCandidateCount = terrain.occlusionCandidates().size();
            store.retainVisible(slot.id(), view, terrain.visibleSections());
            if (terrain.visibleSections().isEmpty()) {
                var visibleScratch = terrain.visibleScratchSections();
                visibleScratch.clear();
                store.collectVisibleByKeys(slot.id(), view, offsetFrustum, visibleScratch);
                if (!visibleScratch.isEmpty()) {
                    terrain.replaceVisibleSections(visibleScratch);
                    if (terrain.isGraphReady()) {
                        terrain.invalidateGraph();
                    }
                }
            }
            visibilityBeforeFallback = terrain.visibleSections().size();
        }
        terrain.rebuildLayerSections();
        if (TerrainDebug.enabled()) {
            TerrainDebug.recordVisibilityDiagnostics(
                slot,
                camera.getPosition(),
                viewDistance,
                terrain.isGraphReady(),
                rebuildVisibility,
                occlusionCandidateCount,
                visibilityBeforeFallback,
                terrain.visibleSections().size(),
                store.estimateVisibleByExistingKeys(slot.id(), view, offsetFrustum),
                store.diagnoseView(slot.id(), view, terrain.visibleSections()),
                materialization
            );
        }
        if (viewUpdate != null && !viewUpdate.reset() && viewUpdate.removed().isEmpty()) {
            store.releaseReferences(slot.id(), viewUpdate.removed());
        }
        TerrainDebug.recordSlot(slot, store, materialization);
    }

    private static void invalidateGraphWhenCameraMoves(TerrainState terrain, Camera camera) {
        Vec3 position = camera.getPosition();
        double cameraCubeX = Math.floor(position.x / 8.0D);
        double cameraCubeY = Math.floor(position.y / 8.0D);
        double cameraCubeZ = Math.floor(position.z / 8.0D);
        if (cameraCubeX != terrain.prevCamX() || cameraCubeY != terrain.prevCamY() || cameraCubeZ != terrain.prevCamZ()) {
            terrain.invalidateGraph();
        }
        terrain.setPrevCamX(cameraCubeX);
        terrain.setPrevCamY(cameraCubeY);
        terrain.setPrevCamZ(cameraCubeZ);
    }

    private static boolean consumeCameraRotationUpdate(TerrainState terrain, Camera camera) {
        double cameraRotX = Math.floor(camera.getXRot() / 2.0F);
        double cameraRotY = Math.floor(camera.getYRot() / 2.0F);
        boolean changed = cameraRotX != terrain.prevCamRotX() || cameraRotY != terrain.prevCamRotY();
        terrain.setPrevCamRotX(cameraRotX);
        terrain.setPrevCamRotY(cameraRotY);
        return changed;
    }

    private static void bootstrapVisibleIfGraphIsEmpty(
        GlobalTerrainStore store,
        PlayerSlot slot,
        SlotTerrainView view,
        Frustum offsetFrustum,
        TerrainState terrain,
        ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleScratch
    ) {
        if (!visibleScratch.isEmpty()) {
            return;
        }
        store.collectVisibleByKeys(slot.id(), view, offsetFrustum, visibleScratch);
        if (!visibleScratch.isEmpty() && terrain.isGraphReady()) {
            terrain.invalidateGraph();
        }
    }

    public static void onChunkLoaded(ChunkPos chunkPos) {
        if (!LevelRendererFields.hasTerrainStore()) {
            return;
        }
        GlobalTerrainStore store = LevelRendererFields.terrainStore();
        Integer activeSlotId = ActiveSlot.idOrNull();
        if (activeSlotId != null) {
            handleChunkLoadedForSlot(store, activeSlotId, chunkPos);
            return;
        }

        for (int slotId = 0; slotId < PlayerSlots.MAX_SLOTS; slotId++) {
            handleChunkLoadedForSlot(store, slotId, chunkPos);
        }
    }

    private static void handleChunkLoadedForSlot(GlobalTerrainStore store, int slotId, ChunkPos chunkPos) {
        PlayerSlot slot = LocalPlayers.INSTANCE.slots().slot(slotId);
        ClientLevel level = slot.renderState().level();
        if (level != null) {
            TerrainDebug.recordChunkDiagnostics(store.onChunkLoadedForSlot(slotId, level, chunkPos));
            slot.renderState().terrain().onChunkLoaded(chunkPos);
        }
    }

    public static void onSectionCompiled(SectionRenderDispatcher.RenderSection section) {
        GlobalTerrainStore store = LevelRendererFields.terrainStore();
        int slotId = store.slotIdFor(section);
        if (slotId >= 0 && slotId < PlayerSlots.MAX_SLOTS) {
            LocalPlayers.INSTANCE.slots().slot(slotId).renderState().terrain().onSectionCompiled(section);
        }
    }

    public static void releaseGraphs(GlobalTerrainStore store) {
        for (int slotId = 0; slotId < PlayerSlots.MAX_SLOTS; slotId++) {
            LocalPlayers.INSTANCE.slots().slot(slotId).renderState().terrain().releaseGraph(store);
        }
    }

    public static void compileSections(Camera ignoredCamera) {
        if (!TerrainPhase.canUpdateTerrain()) {
            return;
        }

        PlayerSlot slot = LocalPlayers.INSTANCE.runtimeSlot();
        TerrainDebug.recordRawStateCheck("terrain.compile", slot.id(), Minecraft.getInstance());

        GlobalTerrainStore store = LevelRendererFields.terrainStore();
        SectionRenderDispatcher dispatcher = store.dispatcher();

        TerrainWorkload.drainUploads(dispatcher);
        serviceLightUpdatesForVisibleSlotLevels();

        Map<ClientLevel, RenderRegionCache> regionCaches = new IdentityHashMap<>();

        GlobalTerrainStore.DirtyBatch dirtyBatch = visibleDirtySections(store);
        List<SectionRenderDispatcher.RenderSection> sections = dirtyBatch.sections();
        int asyncStartsRemaining = TerrainWorkload.asyncRebuildStartAllowance(dispatcher, dirtyBatch.visible());
        int started = 0;
        int completedEmpty = 0;
        int skippedNotDirty = 0;
        int skippedInactive = 0;
        int skippedNoOwner = 0;
        int skippedNoCamera = 0;
        int skippedNoLight = 0;
        int skippedActiveTasks = 0;
        int cancelledTasks = 0;
        int allowanceBreak = 0;
        for (int sectionIndex = 0; sectionIndex < sections.size(); sectionIndex++) {
            SectionRenderDispatcher.RenderSection section = sections.get(sectionIndex);
            if (!section.isDirty()) {
                skippedNotDirty++;
                continue;
            }

            if (!store.isActive(section)) {
                skippedInactive++;
                continue;
            }

            ClientLevel owner = store.ownerOrNull(section);
            if (owner == null) {
                skippedNoOwner++;
                store.deferUntilOwner(section, "compile-dispatch");
                continue;
            }
            SectionPos sectionPos = SectionPos.of(section.getOrigin());

            if (!owner.getLightEngine().lightOnInSection(sectionPos)) {
                skippedNoLight++;
                store.deferDirty(section);
                continue;
            }

            if (store.hasActiveTasks(section)) {
                if (!section.isDirtyFromPlayer()) {
                    skippedActiveTasks++;
                    store.deferDirty(section);
                    continue;
                }
                ((RenderSectionSSAccessor)(Object)section).splitTest$cancelTasks();
                cancelledTasks++;
            }

            if (TerrainWorkload.completeIfEmpty(owner, sectionPos, section)) {
                completedEmpty++;
                continue;
            }

            if (asyncStartsRemaining <= 0) {
                allowanceBreak = sections.size() - sectionIndex;
                deferRemaining(store, sections, sectionIndex);
                break;
            }

            int ownerSlotId = store.slotIdFor(section);
            if (ownerSlotId < 0) {
                skippedNoOwner++;
                continue;
            }
            if (!store.hasRequestingCamera(section)) {
                skippedNoCamera++;
                store.deferDirty(section);
                continue;
            }
            PlayerSlot ownerSlot = LocalPlayers.INSTANCE.slots().slot(ownerSlotId);
            RenderRegionCache regionCache = regionCaches.computeIfAbsent(owner, ignored -> new RenderRegionCache());
            Minecraft minecraft = Minecraft.getInstance();
            try (
                ActiveSlot.Scope ignoredActiveSlot = ActiveSlot.enter(ownerSlotId);
                RawMinecraftStateScope ignoredRawState = RawMinecraftStateScope.bind(minecraft, ownerSlot);
                WorldEngineStateScope ignoredEngines = WorldEngineStateScope.bindTerrainOnly(minecraft, ownerSlot)
            ) {
                TerrainDebug.recordRawStateCheck("terrain.compile.owner", ownerSlotId, minecraft);
                section.rebuildSectionAsync(dispatcher, regionCache);
            }
            section.setNotDirty();
            asyncStartsRemaining--;
            started++;
        }
        if (TerrainDebug.enabled()) {
            TerrainDebug.recordCompileDiagnostics(new GlobalTerrainStore.CompileStats(
                sections.size(),
                started,
                completedEmpty,
                skippedNotDirty,
                skippedInactive,
                skippedNoOwner,
                skippedNoCamera,
                skippedNoLight,
                skippedActiveTasks,
                cancelledTasks,
                allowanceBreak,
                asyncStartsRemaining,
                store.pendingCount(),
                store.ownerWaitCount(),
                store.dirtyQueueCount(),
                store.activeTaskCount()
            ));
        }
    }


    private static int slotIdForLevel(ClientLevel level) {
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

    private static void serviceLightUpdatesForVisibleSlotLevels() {
        Set<ClientLevel> servicedLevels = Collections.newSetFromMap(new IdentityHashMap<>());
        for (int slotId = 0; slotId < PlayerSlots.MAX_SLOTS; slotId++) {
            PlayerSlot slot = LocalPlayers.INSTANCE.slots().slot(slotId);
            if (!canRender(slot)) {
                continue;
            }
            ClientLevel level = slot.renderState().level();
            if (level != null && servicedLevels.add(level)) {
                level.pollLightUpdates();
                level.getLightEngine().runLightUpdates();
            }
        }
    }

    private static GlobalTerrainStore.DirtyBatch visibleDirtySections(GlobalTerrainStore store) {
        GlobalTerrainStore.DirtyBatch batch = store.drainDirtySections(TerrainWorkload.dirtyCheckAllowance());
        List<DirtySectionPriority> priorities = new ArrayList<>(batch.sections().size());
        for (SectionRenderDispatcher.RenderSection section : batch.sections()) {
            BlockPos origin = section.getOrigin();
            priorities.add(new DirtySectionPriority(
                section,
                store.compilationPriority(section),
                origin.getX(),
                origin.getY(),
                origin.getZ(),
                System.identityHashCode(section)
            ));
        }
        priorities.sort(
            Comparator.comparingDouble(DirtySectionPriority::priority)
                .thenComparingInt(DirtySectionPriority::x)
                .thenComparingInt(DirtySectionPriority::y)
                .thenComparingInt(DirtySectionPriority::z)
                .thenComparingInt(DirtySectionPriority::identity)
        );
        batch.sections().clear();
        for (DirtySectionPriority priority : priorities) {
            batch.sections().add(priority.section());
        }
        return batch;
    }

    private record DirtySectionPriority(
        SectionRenderDispatcher.RenderSection section,
        double priority,
        int x,
        int y,
        int z,
        int identity
    ) {
    }

    public static ClientLevel activeLevel() {
        ClientLevel level = LocalPlayers.INSTANCE.runtimeSlot().renderState().level();
        if (level == null) {
            throw new IllegalStateException("Active player slot has no ClientLevel");
        }
        return level;
    }

    public static void pollLightUpdates(ClientLevel level) {
        if (TerrainPhase.canUpdateTerrain()) {
            level.pollLightUpdates();
        }
    }

    public static int runLightUpdates(LevelLightEngine lightEngine) {
        return TerrainPhase.canUpdateTerrain() ? lightEngine.runLightUpdates() : 0;
    }

    private static void deferRemaining(
        GlobalTerrainStore store,
        List<SectionRenderDispatcher.RenderSection> sections,
        int firstIndex
    ) {
        for (int index = firstIndex; index < sections.size(); index++) {
            store.deferDirty(sections.get(index));
        }
    }

    public static boolean isSectionCompiled(BlockPos pos) {
        SectionRenderDispatcher.RenderSection section = LevelRendererFields.terrainStore().sectionAt(activeLevel(), pos);
        return section != null && section.getCompiled() != SectionRenderDispatcher.CompiledSection.UNCOMPILED;
    }

    private static boolean canRender(PlayerSlot slot) {
        return slot.drawable()
            && slot.renderState().level() != null
            && slot.gameplayState().player() != null
            && slot.gameplayState().gameMode() != null
            && !LocalPlayers.INSTANCE.sessions().isJoining(slot.id());
    }

}
