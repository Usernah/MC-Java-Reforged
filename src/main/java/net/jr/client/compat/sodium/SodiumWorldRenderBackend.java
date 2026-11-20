package net.jr.client.compat.sodium;

import java.util.List;

import net.jr.api.client.local.Client;
import net.jr.api.client.render.world.WorldRenderContext;
import net.jr.api.client.render.world.WorldRenderContexts;
import net.jr.client.runtime.render.world.WorldRenderBackend;
import net.jr.client.runtime.render.world.WorldRenderView;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.context.SlotExecution;
import net.jr.client.runtime.slot.ClientSlots;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SodiumWorldRenderBackend implements WorldRenderBackend {
    private static final Logger LOGGER = LoggerFactory.getLogger(SodiumWorldRenderBackend.class);
    private final SodiumViewState[] viewStates = new SodiumViewState[ClientSlots.MAX_SLOTS];
    private final boolean[] captureLogged = new boolean[ClientSlots.MAX_SLOTS];
    private final SodiumRendererStateRouter terrainStates = new SodiumRendererStateRouter();
    private Object renderer;
    private int renderViewCount;
    private int viewListEpoch = Integer.MIN_VALUE;
    private long frameSequence;

    public SodiumWorldRenderBackend() {
        for (int slotId = 0; slotId < this.viewStates.length; slotId++) {
            this.viewStates[slotId] = new SodiumViewState(slotId);
        }
    }

    @Override
    public String id() {
        return "sodium";
    }

    @Override
    public void beginExtractionFrame(List<WorldRenderView> views) {
        long currentFrame = ++this.frameSequence;
        for (WorldRenderView view : views) {
            this.viewStates[view.slotId()].beginFrame(currentFrame, view);
        }
    }

    @Override
    public void beginRenderFrame(List<WorldRenderView> views) {
        this.renderViewCount = views.size();
    }

    @Override
    public void resourcesReloaded() {
        if (this.renderer != null) {
            SodiumRendererBridge.reloadRenderer(this.renderer);
        }
    }

    @Override
    public Scope enterExtractionView(WorldRenderView view) {
        if (view.updatesTerrain()) {
            return Scope.NOOP;
        }
        var extraction = view.renderState().levelExtractionState();
        extraction.consumeLocalLevelResetRequest();
        extraction.setLastViewDistance(Minecraft.getInstance().options.getEffectiveRenderDistance());
        extraction.setShouldInvalidateCompiledGeometry(false);
        return Scope.NOOP;
    }

    @Override
    public Scope enterRenderView(WorldRenderView view) {
        SodiumViewState state = this.viewStates[view.slotId()];
        if (this.renderer == null || !state.setupCaptured() || this.renderViewCount <= 1) {
            return Scope.NOOP;
        }

        ClientLevel level = state.level();
        if (level == null) {
            return Scope.NOOP;
        }
        this.terrainStates.bindForView(this.renderer, level);
        SodiumRendererBridge.activateView(this.renderer, state, nextViewListEpoch());
        return Scope.NOOP;
    }

    public boolean captureTerrainSetup(
        Object renderer,
        Camera camera,
        Object viewport,
        Object fogParameters,
        boolean useOcclusionCulling,
        boolean updateChunksImmediately,
        Matrix4f cullMatrix
    ) {
        this.renderer = renderer;
        WorldRenderContext context = WorldRenderContexts.currentOrNull();
        if (context == null) {
            return false;
        }
        this.terrainStates.bindForView(renderer, context.level());
        int slotId = context.slotId();
        this.viewStates[slotId].captureSetup(
            camera,
            viewport,
            fogParameters,
            useOcclusionCulling,
            updateChunksImmediately,
            cullMatrix
        );
        if (!this.captureLogged[slotId]) {
            this.captureLogged[slotId] = true;
            LOGGER.info("Captured Sodium terrain view state for local slot {}", slotId);
        }
        return false;
    }

    public void terrainRendererInitialized(Object renderer) {
        this.renderer = renderer;
        this.terrainStates.registerInitial(renderer);
    }

    public SodiumViewState viewState(int slotId) {
        ClientSlots.requireSlotId(slotId);
        return this.viewStates[slotId];
    }

    public boolean routeLevelChange(Object renderer, @javax.annotation.Nullable ClientLevel level) {
        if (level == null) {
            return this.renderer == renderer && WorldRenderContexts.currentOrNull() != null;
        }
        return this.terrainStates.bindExistingRenderer(renderer, level);
    }

    public boolean releaseRendererState(Object renderer) {
        return this.terrainStates.releaseRendererState(renderer);
    }

    public void releaseAllRendererStates(Object renderer) {
        this.terrainStates.releaseAllRendererStates(renderer);
        this.viewListEpoch = Integer.MIN_VALUE;
    }

    public void activateTerrainSourceForCurrentExecution(Object renderer) {
        ClientLevel level = this.currentExecutionLevel();
        if (level != null) {
            this.terrainStates.bindForView(renderer, level);
        }
    }

    private int nextViewListEpoch() {
        return this.viewListEpoch++;
    }

    private @javax.annotation.Nullable ClientLevel currentExecutionLevel() {
        WorldRenderContext context = WorldRenderContexts.currentOrNull();
        if (context != null) {
            return context.level();
        }
        var localClient = Client.currentOrNull();
        if (localClient != null) {
            return localClient.level();
        }
        Integer slotId = SlotExecution.currentSlotIdOrNull();
        if (slotId != null) {
            return ClientRuntime.INSTANCE.slots().slot(slotId).renderState().level();
        }
        return null;
    }

    public long frameSequence() {
        return this.frameSequence;
    }
}
