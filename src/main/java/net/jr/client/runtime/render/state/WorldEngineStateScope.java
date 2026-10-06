package net.jr.client.runtime.render.state;

import javax.annotation.Nullable;
import net.jr.bootstrap.OptionalModPresence;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.slot.Slots;
import net.jr.client.runtime.slot.ClientSlots;
import net.jr.client.runtime.state.ClientRenderState;
import net.jr.client.runtime.state.LevelExtractionState;
import net.jr.mixin.accessors.GameRenderStateAccessor;
import net.jr.mixin.accessors.LevelExtractorAccessor;
import net.jr.mixin.accessors.LevelRendererAccessor;
import net.jr.mixin.accessors.ParticleEngineAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.state.LightmapRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;

public final class WorldEngineStateScope implements AutoCloseable {
    private final Minecraft minecraft;
    private final Slots slot;
    private final boolean bindCompleteState;
    private final boolean bindNativeTerrain;
    private final LevelRendererAccessor renderer;
    @Nullable
    private final LevelExtractorAccessor extractor;
    @Nullable
    private final GameRenderStateAccessor gameState;
    @Nullable
    private final ClientLevel previousExtractorLevel;
    @Nullable
    private final ClientLevel previousParticleLevel;
    @Nullable
    private final LevelExtractionState previousExtractionState;
    @Nullable
    private final LevelRenderState previousGameRenderState;
    @Nullable
    private final LightmapRenderState previousLightmapRenderState;
    private final LevelRenderState previousLevelRenderState;
    private final SectionOcclusionGraph previousGraph;
    private final it.unimi.dsi.fastutil.objects.ObjectArrayList<net.minecraft.client.renderer.chunk.SectionRenderDispatcher.RenderSection> previousVisible;
    private final it.unimi.dsi.fastutil.objects.ObjectArrayList<net.minecraft.client.renderer.chunk.SectionRenderDispatcher.RenderSection> previousNearby;
    @Nullable
    private final ViewArea previousViewArea;
    private boolean closed;

    private WorldEngineStateScope(Minecraft minecraft, Slots slot, boolean bindCompleteState) {
        this.minecraft = minecraft;
        this.slot = slot;
        this.bindCompleteState = bindCompleteState;
        this.bindNativeTerrain = !OptionalModPresence.sodiumLoaded();
        this.renderer = (LevelRendererAccessor)minecraft.levelRenderer;
        if (bindCompleteState) {
            this.extractor = (LevelExtractorAccessor)minecraft.levelExtractor;
            this.gameState = (GameRenderStateAccessor)minecraft.gameRenderer.gameRenderState();
            this.previousExtractorLevel = this.extractor.javaReforged$getLevel();
            this.previousParticleLevel = ((ParticleEngineAccessor)minecraft.particleEngine).javaReforged$getLevel();
            this.previousExtractionState = captureExtractionState(this.extractor);
            this.previousGameRenderState = this.gameState.javaReforged$getLevelRenderState();
            this.previousLightmapRenderState = this.gameState.javaReforged$getLightmapRenderState();
        } else {
            this.extractor = null;
            this.gameState = null;
            this.previousExtractorLevel = null;
            this.previousParticleLevel = null;
            this.previousExtractionState = null;
            this.previousGameRenderState = null;
            this.previousLightmapRenderState = null;
        }
        this.previousLevelRenderState = this.renderer.javaReforged$getLevelRenderState();
        this.previousGraph = this.renderer.javaReforged$getSectionOcclusionGraph();
        this.previousVisible = this.renderer.javaReforged$getVisibleSections();
        this.previousNearby = this.renderer.javaReforged$getNearbyVisibleSections();
        this.previousViewArea = this.renderer.javaReforged$getViewArea();
        this.installSlot();
    }

    public static WorldEngineStateScope bind(Minecraft minecraft, Slots slot) {
        return new WorldEngineStateScope(minecraft, slot, true);
    }

    public static WorldEngineStateScope bind(Minecraft minecraft, ClientLevel level) {
        int slotId = slotIdForLevel(level);
        if (slotId < 0) {
            throw new IllegalStateException("ClientLevel is not owned by a local player slot");
        }
        return bind(minecraft, ClientRuntime.INSTANCE.slots().slot(slotId));
    }

    public static WorldEngineStateScope bindTerrainOnly(Minecraft minecraft, Slots slot) {
        return new WorldEngineStateScope(minecraft, slot, false);
    }

    private void installSlot() {
        ClientRenderState state = this.slot.renderState();
        ClientLevel level = state.level();
        this.renderer.javaReforged$setLevelRenderState(state.levelRenderState());
        if (this.bindNativeTerrain) {
            this.renderer.javaReforged$setSectionOcclusionGraph(state.terrain().sectionOcclusionGraph());
            this.renderer.javaReforged$setVisibleSections(state.terrain().visibleSections());
            this.renderer.javaReforged$setNearbyVisibleSections(state.terrain().nearbyVisibleSections());
            this.renderer.javaReforged$setViewArea(state.terrain().viewArea());
        }
        if (this.bindCompleteState) {
            this.gameState.javaReforged$setLevelRenderState(state.levelRenderState());
            this.gameState.javaReforged$setLightmapRenderState(state.lightmapRenderState());
            installExtractionState(this.extractor, level, state.levelRenderState(), state.levelExtractionState());
            ((ParticleEngineAccessor)this.minecraft.particleEngine).javaReforged$setLevel(level);
        }
    }

    @Override
    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        if (this.bindCompleteState) {
            captureExtractionState(this.extractor, this.slot.renderState().levelExtractionState());
            installExtractionState(this.extractor, this.previousExtractorLevel, this.previousLevelRenderState, this.previousExtractionState);
            this.gameState.javaReforged$setLevelRenderState(this.previousGameRenderState);
            this.gameState.javaReforged$setLightmapRenderState(this.previousLightmapRenderState);
        }
        this.renderer.javaReforged$setLevelRenderState(this.previousLevelRenderState);
        if (this.bindNativeTerrain) {
            this.renderer.javaReforged$setSectionOcclusionGraph(this.previousGraph);
            this.renderer.javaReforged$setVisibleSections(this.previousVisible);
            this.renderer.javaReforged$setNearbyVisibleSections(this.previousNearby);
            this.renderer.javaReforged$setViewArea(this.previousViewArea);
        }
        if (this.bindCompleteState) {
            ((ParticleEngineAccessor)this.minecraft.particleEngine).javaReforged$setLevel(this.previousParticleLevel);
        }
    }

    private static int slotIdForLevel(ClientLevel level) {
        for (int slotId = 0; slotId < ClientSlots.MAX_SLOTS; slotId++) {
            if (ClientRuntime.INSTANCE.slots().slot(slotId).renderState().level() == level) {
                return slotId;
            }
        }
        return -1;
    }

    private static LevelExtractionState captureExtractionState(LevelExtractorAccessor extractor) {
        LevelExtractionState state = new LevelExtractionState();
        captureExtractionState(extractor, state);
        return state;
    }

    private static void captureExtractionState(LevelExtractorAccessor extractor, LevelExtractionState state) {
        state.setSectionUpdateTracker(extractor.javaReforged$getSectionUpdateTracker());
        state.setPrevCamRotX(extractor.javaReforged$getPrevCamRotX());
        state.setPrevCamRotY(extractor.javaReforged$getPrevCamRotY());
        state.setLastViewDistance(extractor.javaReforged$getLastViewDistance());
        state.setShouldInvalidateCompiledGeometry(extractor.javaReforged$getShouldInvalidateCompiledGeometry());
        state.setShouldResetLevelRenderData(extractor.javaReforged$getShouldResetLevelRenderData());
        state.setShouldResetChunkLayerSampler(extractor.javaReforged$getShouldResetChunkLayerSampler());
        state.setShouldResetSkyRenderer(extractor.javaReforged$getShouldResetSkyRenderer());
    }

    private static void installExtractionState(
        LevelExtractorAccessor extractor,
        @Nullable ClientLevel level,
        LevelRenderState renderState,
        LevelExtractionState state
    ) {
        extractor.javaReforged$setLevel(level);
        extractor.javaReforged$setLevelRenderState(renderState);
        extractor.javaReforged$setSectionUpdateTracker(state.sectionUpdateTracker());
        extractor.javaReforged$setPrevCamRotX(state.prevCamRotX());
        extractor.javaReforged$setPrevCamRotY(state.prevCamRotY());
        extractor.javaReforged$setLastViewDistance(state.lastViewDistance());
        extractor.javaReforged$setShouldInvalidateCompiledGeometry(state.shouldInvalidateCompiledGeometry());
        extractor.javaReforged$setShouldResetLevelRenderData(state.shouldResetLevelRenderData());
        extractor.javaReforged$setShouldResetChunkLayerSampler(state.shouldResetChunkLayerSampler());
        extractor.javaReforged$setShouldResetSkyRenderer(state.shouldResetSkyRenderer());
    }
}
