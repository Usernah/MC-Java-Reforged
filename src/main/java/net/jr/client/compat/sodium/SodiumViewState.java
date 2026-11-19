package net.jr.client.compat.sodium;

import javax.annotation.Nullable;
import net.jr.client.runtime.render.world.WorldRenderView;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import org.joml.Matrix4f;


public final class SodiumViewState {
    private final int slotId;
    private long frameSequence;
    private boolean setupCaptured;
    @Nullable
    private LocalPlayer player;
    @Nullable
    private ClientLevel level;
    @Nullable
    private Camera camera;
    @Nullable
    private Object viewport;
    @Nullable
    private Object fogParameters;
    private final Matrix4f cullMatrix = new Matrix4f();
    private boolean useOcclusionCulling;
    private boolean updateChunksImmediately;

    SodiumViewState(int slotId) {
        this.slotId = slotId;
    }

    void beginFrame(long frameSequence, WorldRenderView view) {
        this.frameSequence = frameSequence;
        this.setupCaptured = false;
        this.player = view.player();
        this.level = view.level();
        this.camera = view.camera();
        this.viewport = null;
        this.fogParameters = null;
        this.useOcclusionCulling = false;
        this.updateChunksImmediately = false;
    }

    void captureSetup(
        Camera camera,
        Object viewport,
        Object fogParameters,
        boolean useOcclusionCulling,
        boolean updateChunksImmediately,
        Matrix4f cullMatrix
    ) {
        this.camera = camera;
        this.viewport = viewport;
        this.fogParameters = fogParameters;
        this.useOcclusionCulling = useOcclusionCulling;
        this.updateChunksImmediately = updateChunksImmediately;
        this.cullMatrix.set(cullMatrix);
        this.setupCaptured = true;
    }

    public int slotId() {
        return this.slotId;
    }

    public long frameSequence() {
        return this.frameSequence;
    }

    public boolean setupCaptured() {
        return this.setupCaptured;
    }

    public @Nullable LocalPlayer player() {
        return this.player;
    }

    public @Nullable ClientLevel level() {
        return this.level;
    }

    public @Nullable Camera camera() {
        return this.camera;
    }

    public @Nullable Object viewport() {
        return this.viewport;
    }

    public @Nullable Object fogParameters() {
        return this.fogParameters;
    }

    public Matrix4f cullMatrix() {
        return this.cullMatrix;
    }

    public boolean useOcclusionCulling() {
        return this.useOcclusionCulling;
    }

    public boolean updateChunksImmediately() {
        return this.updateChunksImmediately;
    }
}
