package net.jr.mixin.SSM;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.systems.RenderPass;
import net.jr.ClientRuntime.runtime.ViewportPanoramas;
import net.jr.ClientRuntime.runtime.ViewportPass;
import net.jr.ClientRuntime.viewport.ViewportArea;
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
        if (!ViewportPanoramas.isRendering() || !ViewportPass.hasActiveViewport()) {
            return;
        }
        ViewportArea viewport = ViewportPass.activeViewport();
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
        if (!ViewportPanoramas.isRendering() || !ViewportPass.hasActiveViewport()) {
            return renderPass;
        }
        ViewportArea viewport = ViewportPass.activeViewport();
        renderPass.setViewport(viewport.glX(), viewport.glY(), viewport.glWidth(), viewport.glHeight());
        renderPass.enableScissor(viewport.glX(), viewport.glY(), viewport.glWidth(), viewport.glHeight());
        return renderPass;
    }
}
