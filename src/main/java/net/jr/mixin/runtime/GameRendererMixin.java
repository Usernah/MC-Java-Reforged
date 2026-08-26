package net.jr.mixin.runtime;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.jr.client.runtime.render.tick.CameraTicker;
import net.jr.client.runtime.viewport.ViewportProjection;
import net.jr.client.runtime.render.tick.HandRendererTicker;
import net.jr.client.runtime.render.target.ViewportRenderTargets;
import net.jr.client.runtime.render.pass.WorldExtractionPass;
import net.jr.client.runtime.render.pass.WorldRenderPass;
import net.jr.client.ui.hint.render.ControlHintCompositor;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.Lightmap;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
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

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "mainRenderTarget", at = @At("HEAD"), cancellable = true)
    private void javaReforged$resolveActiveMainTarget(CallbackInfoReturnable<RenderTarget> cir) {
        RenderTarget activeTarget = ViewportRenderTargets.activeMainOrNull();
        if (activeTarget != null) {
            cir.setReturnValue(activeTarget);
        }
    }

    @ModifyExpressionValue(
        method = "renderLevel",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/GameRenderer;mainRenderTarget:Lcom/mojang/blaze3d/pipeline/RenderTarget;")
    )
    private RenderTarget javaReforged$resolveDirectMainTargetAccess(RenderTarget original) {
        return ViewportRenderTargets.activeMainOr(original);
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void javaReforged$closeSlotRenderTargets(CallbackInfo ci) {
        ControlHintCompositor.closeAll();
        ViewportRenderTargets.closeAll();
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;tick()V"))
    private void javaReforged$tickCamerasByClient(Camera ignoredVanillaCamera) {
        CameraTicker.tickConnectedClients();
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;tick()V"))
    private void javaReforged$tickHandsBySlot(ItemInHandRenderer renderer) {
        HandRendererTicker.tickClients(renderer);
    }

    @Redirect(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;update(Lnet/minecraft/client/DeltaTracker;)V"))
    private void javaReforged$updateCamerasByClient(Camera ignoredVanillaCamera, DeltaTracker deltaTracker) {
        WorldExtractionPass.updateCameras(deltaTracker);
    }

    @Redirect(
        method = "extract",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LightmapRenderStateExtractor;extract(Lnet/minecraft/client/renderer/state/LightmapRenderState;F)V")
    )
    private void javaReforged$deferLightmapExtraction(LightmapRenderStateExtractor extractor, LightmapRenderState state, float partialTicks) {
    }

    @Redirect(
        method = "extract",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;extractCamera(Lnet/minecraft/client/DeltaTracker;FF)V")
    )
    private void javaReforged$deferCameraExtraction(GameRenderer renderer, DeltaTracker deltaTracker, float worldPartialTicks, float cameraPartialTicks) {
    }

    @Redirect(
        method = "extract",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/extract/LevelExtractor;extract(Lnet/minecraft/client/DeltaTracker;Lnet/minecraft/client/Camera;F)V")
    )
    private void javaReforged$extractVisibleSlots(LevelExtractor extractor, DeltaTracker deltaTracker, Camera camera, float worldPartialTicks) {
        WorldExtractionPass.extractVisibleSlots((GameRenderer)(Object)this, extractor, deltaTracker, worldPartialTicks);
    }

    @Redirect(
        method = "render",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/Lightmap;render(Lnet/minecraft/client/renderer/state/LightmapRenderState;)V")
    )
    private void javaReforged$deferLightmapRender(Lightmap lightmap, LightmapRenderState state) {
    }

    @Redirect(
        method = "render",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;renderLevel(Lnet/minecraft/client/DeltaTracker;)V")
    )
    private void javaReforged$renderLevelForVisibleSlots(GameRenderer gameRenderer, DeltaTracker deltaTracker) {
        WorldRenderPass.renderLevelForVisibleSlots(gameRenderer, deltaTracker);
    }

    @Redirect(
        method = "render",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;doEntityOutline()V")
    )
    private void javaReforged$deferEntityOutlineToEachSlot(LevelRenderer levelRenderer) {
    }

    @ModifyExpressionValue(
        method = "render",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/GameRenderer;postEffectId:Lnet/minecraft/resources/Identifier;")
    )
    private net.minecraft.resources.Identifier javaReforged$deferPostEffectToEachSlot(net.minecraft.resources.Identifier original) {
        return null;
    }

    @ModifyArgs(
        method = "renderLevel",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/Projection;setupPerspective(FFFFF)V")
    )
    private void javaReforged$useViewportForHudProjection(Args args) {
        ViewportProjection.Size size = ViewportProjection.activeSizeOrNull();
        if (size != null) {
            args.set(3, (float) size.width());
            args.set(4, (float) size.height());
        }
    }
}
