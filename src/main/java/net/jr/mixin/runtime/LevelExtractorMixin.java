package net.jr.mixin.runtime;

import javax.annotation.Nullable;

import net.jr.client.runtime.client.LocalClient;
import net.jr.client.runtime.context.LocalClientScope;
import net.jr.client.runtime.render.pass.BedrockBridgeOutlinePass;
import net.jr.client.runtime.terrain.TerrainSectionUpdateRouter;
import net.jr.client.runtime.terrain.TerrainCoordinator;
import net.jr.mixin.accessors.LevelExtractorAccessor;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.extract.LevelExtractor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelExtractor.class)
public abstract class LevelExtractorMixin {
    @Shadow @Final
    private Minecraft minecraft;
    @Shadow @Nullable
    private ClientLevel level;
    @Inject(method = "allChanged", at = @At("HEAD"), cancellable = true)
    private void javaReforged$routeGlobalGeometryInvalidation(CallbackInfo ci) {
        if (TerrainCoordinator.routeGlobalAllChanged()) {
            ci.cancel();
        }
    }

    @Inject(method = "setSectionDirty(IIIZ)V", at = @At("HEAD"), cancellable = true)
    private void javaReforged$routeVanillaSectionUpdate(
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

    @Redirect(
        method = "extract",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientChunkCache;flipUpdateTrackingSets()V")
    )
    private void javaReforged$flipChunkUpdatesAfterLastViewer(ClientChunkCache chunkCache) {
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
    private void javaReforged$extractBedrockBridgeOutline(
            DeltaTracker deltaTracker,
            Camera camera,
            float deltaPartialTick,
            CallbackInfo ci
    ) {
        LocalClient client = LocalClientScope.currentClientOrNull();
        if (client == null || this.level == null) {
            return;
        }
        BedrockBridgeOutlinePass.extract(
                this.minecraft,
                this.level,
                ((LevelExtractorAccessor) this).javaReforged$getLevelRenderState(),
                client
        );
    }

}
