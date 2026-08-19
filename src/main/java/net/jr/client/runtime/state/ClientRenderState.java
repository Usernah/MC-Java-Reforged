package net.jr.client.runtime.state;

import javax.annotation.Nullable;
import java.util.Objects;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.LightmapRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;

public final class ClientRenderState {
    @Nullable
    private ClientLevel level;
    private Camera camera = new Camera();
    private final HandState hands = new HandState();
    private final HudState hud = new HudState();
    private final FovState fovState = new FovState();
    private final TerrainState terrain = new TerrainState();
    private final ParticleState particles = new ParticleState();
    private final LevelRenderState levelRenderState = new LevelRenderState();
    private final LightmapRenderState lightmapRenderState = new LightmapRenderState();
    private final LevelExtractionState levelExtractionState = new LevelExtractionState();
    @Nullable
    private Frustum frustum;
    @Nullable
    private Entity cameraEntity;
    @Nullable
    private Entity crosshairPickEntity;
    @Nullable
    private HitResult hitResult;
    private boolean noRender;
    private float fov;
    private float aspectRatio = 1.0F;
    private int rainSoundTime;

    public void setLevel(@Nullable ClientLevel level) {
        if (this.level != level) {
            this.levelExtractionState.requestLocalLevelReset();
        }
        this.level = level;
        this.camera.setLevel(level);
    }

    public void bindLevel(@Nullable ClientLevel level) {
        this.setLevel(level);
    }

    public void setCamera(Camera camera) {
        this.camera = Objects.requireNonNull(camera, "camera");
        this.camera.setLevel(this.level);
        this.camera.setEntity(this.cameraEntity);
    }

    public void setFrustum(@Nullable Frustum frustum) {
        this.frustum = frustum;
    }

    public void bindFrustum(@Nullable Frustum frustum) {
        this.setFrustum(frustum);
    }

    public void setCameraEntity(@Nullable Entity cameraEntity) {
        this.cameraEntity = cameraEntity;
        this.camera.setEntity(cameraEntity);
    }

    public void bindCameraEntity(@Nullable Entity cameraEntity) {
        this.setCameraEntity(cameraEntity);
    }

    public void setCrosshairPickEntity(@Nullable Entity crosshairPickEntity) {
        this.crosshairPickEntity = crosshairPickEntity;
    }

    public void bindCrosshairPickEntity(@Nullable Entity crosshairPickEntity) {
        this.setCrosshairPickEntity(crosshairPickEntity);
    }

    public void setHitResult(@Nullable HitResult hitResult) {
        this.hitResult = hitResult;
    }

    public void bindHitResult(@Nullable HitResult hitResult) {
        this.setHitResult(hitResult);
    }

    public void setNoRender(boolean noRender) {
        this.noRender = noRender;
    }

    public void setFov(float fov) {
        this.fov = fov;
    }

    public void setAspectRatio(float aspectRatio) {
        if (aspectRatio <= 0.0F || Float.isNaN(aspectRatio) || Float.isInfinite(aspectRatio)) {
            throw new IllegalArgumentException("aspectRatio must be a positive finite number");
        }
        this.aspectRatio = aspectRatio;
    }

    public void setRainSoundTime(int rainSoundTime) {
        this.rainSoundTime = rainSoundTime;
    }

    @Nullable
    public ClientLevel level() {
        return this.level;
    }

    public Camera camera() {
        return this.camera;
    }

    public HandState hands() {
        return this.hands;
    }

    public HudState hud() {
        return this.hud;
    }

    public FovState fovState() {
        return this.fovState;
    }

    public TerrainState terrain() {
        return this.terrain;
    }

    public ParticleState particles() {
        return this.particles;
    }

    public LevelRenderState levelRenderState() {
        return this.levelRenderState;
    }

    public LightmapRenderState lightmapRenderState() {
        return this.lightmapRenderState;
    }

    public LevelExtractionState levelExtractionState() {
        return this.levelExtractionState;
    }

    @Nullable
    public Frustum frustum() {
        return this.frustum;
    }

    @Nullable
    public Entity cameraEntity() {
        return this.cameraEntity;
    }

    @Nullable
    public Entity crosshairPickEntity() {
        return this.crosshairPickEntity;
    }

    @Nullable
    public HitResult hitResult() {
        return this.hitResult;
    }

    public boolean noRender() {
        return this.noRender;
    }

    public float fov() {
        return this.fov;
    }

    public float aspectRatio() {
        return this.aspectRatio;
    }

    public int rainSoundTime() {
        return this.rainSoundTime;
    }

    public void clearWorldBinding() {
        this.level = null;
        this.cameraEntity = null;
        this.crosshairPickEntity = null;
        this.hitResult = null;
        this.frustum = null;
        this.noRender = false;
        this.fov = 0.0F;
        this.aspectRatio = 1.0F;
        this.rainSoundTime = 0;
        this.fovState.clear();
        this.hands.clear();
        this.hud.clear();
        this.terrain.clear();
        this.particles.clear();
        this.levelRenderState.reset();
        this.lightmapRenderState.needsUpdate = false;
        this.levelExtractionState.clear();
        this.camera.reset();
    }
}
