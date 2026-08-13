package net.jr.mixin.SSM;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.jr.ClientRuntime.runtime.Cameras;
import net.jr.ClientRuntime.runtime.Client;
import net.jr.ClientRuntime.runtime.Hands;
import net.jr.ClientRuntime.runtime.SlotRenderTargets;
import net.jr.ClientRuntime.runtime.WorldExtractions;
import net.jr.ClientRuntime.runtime.WorldPasses;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.Lightmap;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/** Admits every local player through Minecraft's single 26.2 GameRenderer engine. */
@Mixin(GameRenderer.class)
public abstract class GameRendererSSMixin {
    @Inject(method = "mainRenderTarget", at = @At("HEAD"), cancellable = true)
    private void splitTest$resolveActiveMainTarget(CallbackInfoReturnable<RenderTarget> cir) {
        RenderTarget activeTarget = SlotRenderTargets.activeMainOrNull();
        if (activeTarget != null) {
            cir.setReturnValue(activeTarget);
        }
    }

    @ModifyExpressionValue(
        method = "renderLevel",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/GameRenderer;mainRenderTarget:Lcom/mojang/blaze3d/pipeline/RenderTarget;")
    )
    private RenderTarget splitTest$resolveDirectMainTargetAccess(RenderTarget original) {
        return SlotRenderTargets.activeMainOr(original);
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void splitTest$closeSlotRenderTargets(CallbackInfo ci) {
        SlotRenderTargets.closeAll();
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;tick()V"))
    private void splitTest$tickCamerasByClient(Camera ignoredVanillaCamera) {
        Cameras.tickConnectedClients();
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;tick()V"))
    private void splitTest$tickHandsBySlot(ItemInHandRenderer renderer) {
        Hands.tickConnectedClients(renderer);
    }

    @Redirect(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;update(Lnet/minecraft/client/DeltaTracker;)V"))
    private void splitTest$updateCamerasByClient(Camera ignoredVanillaCamera, DeltaTracker deltaTracker) {
        WorldExtractions.updateCameras(deltaTracker);
    }

    @Redirect(
        method = "extract",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LightmapRenderStateExtractor;extract(Lnet/minecraft/client/renderer/state/LightmapRenderState;F)V")
    )
    private void splitTest$deferLightmapExtraction(LightmapRenderStateExtractor extractor, LightmapRenderState state, float partialTicks) {
        // WorldExtractions performs this once per slot after installing its state tree.
    }

    @Redirect(
        method = "extract",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;extractCamera(Lnet/minecraft/client/DeltaTracker;FF)V")
    )
    private void splitTest$deferCameraExtraction(GameRenderer renderer, DeltaTracker deltaTracker, float worldPartialTicks, float cameraPartialTicks) {
        // WorldExtractions performs this once per slot with that slot's Camera.
    }

    @Redirect(
        method = "extract",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/extract/LevelExtractor;extract(Lnet/minecraft/client/DeltaTracker;Lnet/minecraft/client/Camera;F)V")
    )
    private void splitTest$extractVisibleSlots(LevelExtractor extractor, DeltaTracker deltaTracker, Camera camera, float worldPartialTicks) {
        WorldExtractions.extractVisibleSlots((GameRenderer)(Object)this, extractor, deltaTracker, worldPartialTicks);
    }

    @Redirect(
        method = "render",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/Lightmap;render(Lnet/minecraft/client/renderer/state/LightmapRenderState;)V")
    )
    private void splitTest$deferLightmapRender(Lightmap lightmap, LightmapRenderState state) {
        // WorldPasses uploads the matching lightmap immediately before each slot.
    }

    @Redirect(
        method = "render",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;renderLevel(Lnet/minecraft/client/DeltaTracker;)V")
    )
    private void splitTest$renderLevelForVisibleSlots(GameRenderer gameRenderer, DeltaTracker deltaTracker) {
        WorldPasses.renderLevelForVisibleSlots(gameRenderer, deltaTracker);
    }

    @Redirect(
        method = "render",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;doEntityOutline()V")
    )
    private void splitTest$deferEntityOutlineToEachSlot(LevelRenderer levelRenderer) {
        // WorldPasses composites the outline while the matching slot targets are active.
    }

    @ModifyExpressionValue(
        method = "render",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/GameRenderer;postEffectId:Lnet/minecraft/resources/Identifier;")
    )
    private net.minecraft.resources.Identifier splitTest$deferPostEffectToEachSlot(
        net.minecraft.resources.Identifier original
    ) {
        // WorldPasses processes it before that slot is copied into the shared window target.
        return null;
    }

    @ModifyArgs(
        method = "renderLevel",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/Projection;setupPerspective(FFFFF)V")
    )
    private void splitTest$useViewportForHudProjection(Args args) {
        if (Client.currentOrNull() != null && Client.hasViewport(Client.slotId())) {
            args.set(3, (float)Client.viewport(Client.slotId()).width());
            args.set(4, (float)Client.viewport(Client.slotId()).height());
        }
    }
}
