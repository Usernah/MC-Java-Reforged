package net.jr.mixin.runtime;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.systems.RenderPass;
import net.jr.client.runtime.viewport.ViewportPanoramaRenderer;
import net.jr.client.runtime.viewport.ViewportRenderScope;
import net.jr.client.runtime.viewport.ViewportArea;
import net.minecraft.client.renderer.CubeMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/** Constrains vanilla's cubemap renderer while it is serving a local viewport. */
@Mixin(CubeMap.class)
public abstract class CubeMapSSMixin {
    @ModifyArgs(
        method = "render",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/Projection;setupPerspective(FFFFF)V")
    )
    private void splitTest$useViewportAspectRatio(Args args) {
        if (!ViewportPanoramaRenderer.isRendering() || !ViewportRenderScope.hasActiveViewport()) {
            return;
        }
        ViewportArea viewport = ViewportRenderScope.activeViewport();
        args.set(3, (float)viewport.width());
        args.set(4, (float)viewport.height());
    }

    @ModifyExpressionValue(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/CommandEncoder;createRenderPass(Ljava/util/function/Supplier;Lcom/mojang/blaze3d/textures/GpuTextureView;Ljava/util/Optional;Lcom/mojang/blaze3d/textures/GpuTextureView;Ljava/util/OptionalDouble;)Lcom/mojang/blaze3d/systems/RenderPass;"
        )
    )
    private RenderPass splitTest$constrainViewportPanorama(RenderPass renderPass) {
        if (!ViewportPanoramaRenderer.isRendering() || !ViewportRenderScope.hasActiveViewport()) {
            return renderPass;
        }
        ViewportArea viewport = ViewportRenderScope.activeViewport();
        renderPass.setViewport(viewport.glX(), viewport.glY(), viewport.glWidth(), viewport.glHeight());
        renderPass.enableScissor(viewport.glX(), viewport.glY(), viewport.glWidth(), viewport.glHeight());
        return renderPass;
    }
}
