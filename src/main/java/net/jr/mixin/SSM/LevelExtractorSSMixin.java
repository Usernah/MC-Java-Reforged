package net.jr.mixin.SSM;

import net.jr.ClientRuntime.bridge.LevelExtractorSSAccessor;
import javax.annotation.Nullable;
import net.jr.ClientRuntime.runtime.LevelRendererFields;
import net.jr.ClientRuntime.runtime.TerrainCoordinator;
import net.jr.ClientRuntime.runtime.TerrainMarks;
import net.jr.ClientRuntime.state.LevelExtractionState;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.SectionUpdateTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelExtractor.class)
public abstract class LevelExtractorSSMixin implements LevelExtractorSSAccessor {
    @Shadow @Nullable
    private ClientLevel level;
    @Shadow @Nullable
    private SectionUpdateTracker sectionUpdateTracker;
    @Shadow @Final @Mutable
    private LevelRenderState levelRenderState;
    @Shadow
    private double prevCamRotX;
    @Shadow
    private double prevCamRotY;
    @Shadow
    private int lastViewDistance;
    @Shadow
    private boolean shouldInvalidateCompiledGeometry;
    @Shadow
    private boolean shouldResetLevelRenderData;
    @Shadow
    private boolean shouldResetChunkLayerSampler;
    @Shadow
    private boolean shouldResetSkyRenderer;

    @Override
    public ClientLevel splitTest$getLevel() {
        return this.level;
    }

    @Override
    public void splitTest$install(ClientLevel level, LevelRenderState renderState, LevelExtractionState state) {
        this.level = level;
        this.levelRenderState = renderState;
        this.sectionUpdateTracker = state.sectionUpdateTracker();
        this.prevCamRotX = state.prevCamRotX();
        this.prevCamRotY = state.prevCamRotY();
        this.lastViewDistance = state.lastViewDistance();
        this.shouldInvalidateCompiledGeometry = state.shouldInvalidateCompiledGeometry();
        this.shouldResetLevelRenderData = state.shouldResetLevelRenderData();
        this.shouldResetChunkLayerSampler = state.shouldResetChunkLayerSampler();
        this.shouldResetSkyRenderer = state.shouldResetSkyRenderer();
    }

    @Override
    public void splitTest$capture(LevelExtractionState state) {
        state.setSectionUpdateTracker(this.sectionUpdateTracker);
        state.setPrevCamRotX(this.prevCamRotX);
        state.setPrevCamRotY(this.prevCamRotY);
        state.setLastViewDistance(this.lastViewDistance);
        state.setShouldInvalidateCompiledGeometry(this.shouldInvalidateCompiledGeometry);
        state.setShouldResetLevelRenderData(this.shouldResetLevelRenderData);
        state.setShouldResetChunkLayerSampler(this.shouldResetChunkLayerSampler);
        state.setShouldResetSkyRenderer(this.shouldResetSkyRenderer);
    }

    @Inject(
        method = "extract",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/extract/LevelExtractor;extractVisibleEntities(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/culling/Frustum;Lnet/minecraft/client/DeltaTracker;Lnet/minecraft/client/renderer/state/level/LevelRenderState;)V"
        )
    )
    private void splitTest$prepareSlotTerrain(DeltaTracker deltaTracker, Camera camera, float partialTick, CallbackInfo ci) {
        if (LevelRendererFields.hasTerrainStore()) {
            TerrainCoordinator.setup(camera, camera.getCullFrustum());
        }
    }

    @Inject(method = "setSectionDirty(IIIZ)V", at = @At("HEAD"), cancellable = true)
    private void splitTest$routeDirtySection(int x, int y, int z, boolean playerChanged, CallbackInfo ci) {
        if (!LevelRendererFields.hasTerrainStore()) {
            return;
        }
        TerrainMarks.setSectionDirty(x, y, z, playerChanged);
        ci.cancel();
    }
}
