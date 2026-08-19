package net.jr.mixin.runtime;

import net.jr.client.runtime.bridge.LevelExtractorRuntimeAccessor;
import javax.annotation.Nullable;

import net.jr.client.runtime.client.LocalClient;
import net.jr.client.runtime.context.LocalClientScope;
import net.jr.client.runtime.player.BedrockBridgePlacement;
import net.jr.client.runtime.terrain.TerrainSectionUpdateRouter;
import net.jr.client.runtime.terrain.TerrainCoordinator;
import net.jr.client.runtime.state.LevelExtractionState;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.SectionUpdateTracker;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(LevelExtractor.class)
public abstract class LevelExtractorRuntimeMixin implements LevelExtractorRuntimeAccessor {
    @Shadow @Final
    private Minecraft minecraft;
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

    @Inject(method = "allChanged", at = @At("HEAD"), cancellable = true)
    private void splitTest$routeGlobalGeometryInvalidation(CallbackInfo ci) {
        if (TerrainCoordinator.routeGlobalAllChanged()) {
            ci.cancel();
        }
    }

    @Inject(method = "setSectionDirty(IIIZ)V", at = @At("HEAD"), cancellable = true)
    private void splitTest$routeVanillaSectionUpdate(
        int sectionX,
        int sectionY,
        int sectionZ,
        boolean playerChanged,
        CallbackInfo ci
    ) {
        if (TerrainSectionUpdateRouter.setDirty(this.level, sectionX, sectionY, sectionZ, playerChanged)) {
            ci.cancel();
        }
    }

    /**
     * Slots sharing a ClientLevel must all observe the same vanilla chunk delta
     * buffer. Only the final extraction for that level flips the double buffer.
     */
    @Redirect(
        method = "extract",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientChunkCache;flipUpdateTrackingSets()V")
    )
    private void splitTest$flipChunkUpdatesAfterLastViewer(ClientChunkCache chunkCache) {
        if (TerrainSectionUpdateRouter.isLastVisibleViewer(this.level)) {
            chunkCache.flipUpdateTrackingSets();
        }
    }

    @Inject(
            method = "extract",
            at = @At(
                    value = "INVOKE",
                    target =
                            "Lnet/minecraft/client/renderer/extract/LevelExtractor;" +
                                    "extractBlockOutline(" +
                                    "Lnet/minecraft/client/Camera;" +
                                    "Lnet/minecraft/client/renderer/state/level/LevelRenderState;" +
                                    ")V",
                    shift = At.Shift.AFTER
            )
    )
    private void splitTest$extractBedrockBridgeOutline(
            DeltaTracker deltaTracker,
            Camera camera,
            float deltaPartialTick,
            CallbackInfo ci
    ) {
        LocalClient client = LocalClientScope.currentClientOrNull();
        if (client == null || this.level == null) {
            return;
        }

        LocalPlayer player = client.player();
        if (player == null) {
            return;
        }

        HitResult hitResult = client.hitResult();

        // No pisamos la selección vanilla de un bloque normal.
        // El bridge aparece cuando estamos usando la ruta de "aire".
        if (hitResult != null
                && hitResult.getType() != HitResult.Type.MISS) {
            return;
        }

        var candidate =
                BedrockBridgePlacement.resolveFirst(player);

        if (candidate == null) {
            return;
        }

        BlockState state = candidate.placementState();
        BlockPos pos = candidate.targetPos();

        CollisionContext collisionContext =
                CollisionContext.of(player);

        VoxelShape shape = state.getShape(
                this.level,
                pos,
                collisionContext
        );

        if (shape.isEmpty()) {
            return;
        }

        BlockStateModel model = this.minecraft
                .getModelManager()
                .getBlockStateModelSet()
                .get(state);

        boolean translucent = model.hasMaterialFlag(
                this.level,
                pos,
                state,
                1
        );

        boolean highContrast =
                this.minecraft.options
                        .highContrastBlockOutline()
                        .get();

        this.levelRenderState.blockOutlineRenderState =
                new BlockOutlineRenderState(
                        pos,
                        translucent,
                        highContrast,
                        shape,
                        List.of()
                );
    }

}
