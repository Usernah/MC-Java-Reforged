package net.jr.mixin.runtime;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.systems.RenderPass;
import net.jr.client.runtime.viewport.ViewportPanoramaRenderer;
import net.jr.client.runtime.viewport.ViewportProjection;
import net.minecraft.client.renderer.CubeMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(CubeMap.class)
public abstract class CubeMapMixin {
    @ModifyArgs(
        method = "render",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/Projection;setupPerspective(FFFFF)V")
    )
    private void javaReforged$useViewportAspectRatio(Args args) {
        ViewportProjection.Size size = ViewportProjection.activePanoramaSizeOrNull();
        if (size != null) {
            args.set(3, (float) size.width());
            args.set(4, (float) size.height());
        }
    }

    @ModifyExpressionValue(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/CommandEncoder;createRenderPass(Ljava/util/function/Supplier;Lcom/mojang/blaze3d/textures/GpuTextureView;Ljava/util/Optional;Lcom/mojang/blaze3d/textures/GpuTextureView;Ljava/util/OptionalDouble;)Lcom/mojang/blaze3d/systems/RenderPass;"
        )
    )
    private RenderPass javaReforged$constrainViewportPanorama(RenderPass renderPass) {
        return ViewportPanoramaRenderer.constrain(renderPass);
    }
}
