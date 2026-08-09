package net.jr.ClientRuntime.runtime;

import javax.annotation.Nullable;
import net.jr.ClientRuntime.slot.PlayerSlot;
import net.jr.ClientRuntime.state.LevelExtractionState;
import net.jr.ClientRuntime.state.RenderState;
import net.jr.ClientRuntime.bridge.GameRenderStateSSAccessor;
import net.jr.ClientRuntime.bridge.LevelExtractorSSAccessor;
import net.jr.ClientRuntime.bridge.LevelRendererSSAccessor;
import net.jr.mixin.SSM.ParticleEngineSSAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.state.LightmapRenderState;

/**
 * Installs one player's mutable render data into Minecraft's single 26.2 render/extraction engine.
 * The engines themselves are never cloned; this scope only swaps their active data references.
 */
public final class WorldEngineStateScope implements AutoCloseable {
    private final Minecraft minecraft;
    private final PlayerSlot slot;
    private final boolean bindCompleteState;
    private final LevelRendererSSAccessor renderer;
    @Nullable
    private final LevelExtractorSSAccessor extractor;
    @Nullable
    private final GameRenderStateSSAccessor gameState;
    @Nullable private final ClientLevel previousExtractorLevel;
    @Nullable private final ClientLevel previousParticleLevel;
    @Nullable private final LevelExtractionState previousExtractionState;
    @Nullable
    private final LevelRenderState previousGameRenderState;
    @Nullable
    private final LightmapRenderState previousLightmapRenderState;
    private final LevelRenderState previousLevelRenderState;
    private final SectionOcclusionGraph previousGraph;
    private final it.unimi.dsi.fastutil.objects.ObjectArrayList<net.minecraft.client.renderer.chunk.SectionRenderDispatcher.RenderSection> previousVisible;
    private final it.unimi.dsi.fastutil.objects.ObjectArrayList<net.minecraft.client.renderer.chunk.SectionRenderDispatcher.RenderSection> previousNearby;
    @Nullable private final ViewArea previousViewArea;
    private boolean closed;

    private WorldEngineStateScope(Minecraft minecraft, PlayerSlot slot, boolean bindCompleteState) {
        this.minecraft = minecraft;
        this.slot = slot;
        this.bindCompleteState = bindCompleteState;
        this.renderer = (LevelRendererSSAccessor)minecraft.levelRenderer;
        if (bindCompleteState) {
            this.extractor = (LevelExtractorSSAccessor)minecraft.levelExtractor;
            this.gameState = (GameRenderStateSSAccessor)minecraft.gameRenderer.gameRenderState();
            this.previousExtractorLevel = this.extractor.splitTest$getLevel();
            this.previousParticleLevel = ((ParticleEngineSSAccessor)minecraft.particleEngine).splitTest$getLevel();
            this.previousExtractionState = new LevelExtractionState();
            this.extractor.splitTest$capture(this.previousExtractionState);
            this.previousGameRenderState = this.gameState.splitTest$getLevelRenderState();
            this.previousLightmapRenderState = this.gameState.splitTest$getLightmapRenderState();
        } else {
            this.extractor = null;
            this.gameState = null;
            this.previousExtractorLevel = null;
            this.previousParticleLevel = null;
            this.previousExtractionState = null;
            this.previousGameRenderState = null;
            this.previousLightmapRenderState = null;
        }
        this.previousLevelRenderState = this.renderer.splitTest$getLevelRenderState();
        this.previousGraph = this.renderer.splitTest$getSectionOcclusionGraph();
        this.previousVisible = this.renderer.splitTest$getVisibleSections();
        this.previousNearby = this.renderer.splitTest$getNearbyVisibleSections();
        this.previousViewArea = this.renderer.splitTest$getViewArea();
        this.installSlot();
    }

    public static WorldEngineStateScope bind(Minecraft minecraft, PlayerSlot slot) {
        return new WorldEngineStateScope(minecraft, slot, true);
    }

    public static WorldEngineStateScope bind(Minecraft minecraft, ClientLevel level) {
        int slotId = slotIdForLevel(level);
        if (slotId < 0) {
            throw new IllegalStateException("ClientLevel is not owned by a local player slot");
        }
        return bind(minecraft, LocalPlayers.INSTANCE.slots().slot(slotId));
    }

    public static WorldEngineStateScope bindTerrainOnly(Minecraft minecraft, PlayerSlot slot) {
        return new WorldEngineStateScope(minecraft, slot, false);
    }

    private void installSlot() {
        RenderState state = this.slot.renderState();
        ClientLevel level = state.level();
        this.renderer.splitTest$setLevelRenderState(state.levelRenderState());
        this.renderer.splitTest$setSectionOcclusionGraph(state.terrain().sectionOcclusionGraph());
        this.renderer.splitTest$setVisibleSections(state.terrain().visibleSections());
        this.renderer.splitTest$setNearbyVisibleSections(state.terrain().nearbyVisibleSections());
        this.renderer.splitTest$setViewArea(state.terrain().viewArea());
        if (this.bindCompleteState) {
            this.gameState.splitTest$setLevelRenderState(state.levelRenderState());
            this.gameState.splitTest$setLightmapRenderState(state.lightmapRenderState());
            this.extractor.splitTest$install(level, state.levelRenderState(), state.levelExtractionState());
            ((ParticleEngineSSAccessor)this.minecraft.particleEngine).splitTest$setLevel(level);
        }
    }

    @Override
    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        if (this.bindCompleteState) {
            this.extractor.splitTest$capture(this.slot.renderState().levelExtractionState());
            this.extractor.splitTest$install(this.previousExtractorLevel, this.previousLevelRenderState, this.previousExtractionState);
            this.gameState.splitTest$setLevelRenderState(this.previousGameRenderState);
            this.gameState.splitTest$setLightmapRenderState(this.previousLightmapRenderState);
        }
        this.renderer.splitTest$setLevelRenderState(this.previousLevelRenderState);
        this.renderer.splitTest$setSectionOcclusionGraph(this.previousGraph);
        this.renderer.splitTest$setVisibleSections(this.previousVisible);
        this.renderer.splitTest$setNearbyVisibleSections(this.previousNearby);
        this.renderer.splitTest$setViewArea(this.previousViewArea);
        if (this.bindCompleteState) {
            ((ParticleEngineSSAccessor)this.minecraft.particleEngine).splitTest$setLevel(this.previousParticleLevel);
        }
    }

    private static int slotIdForLevel(ClientLevel level) {
        for (int slotId = 0; slotId < net.jr.ClientRuntime.slot.PlayerSlots.MAX_SLOTS; slotId++) {
            if (LocalPlayers.INSTANCE.slots().slot(slotId).renderState().level() == level) {
                return slotId;
            }
        }
        return -1;
    }
}
