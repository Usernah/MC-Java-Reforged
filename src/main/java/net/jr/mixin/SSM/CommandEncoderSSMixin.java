package net.jr.mixin.SSM;

import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderPassDescriptor;
import com.mojang.blaze3d.textures.GpuTexture;
import net.jr.ClientRuntime.runtime.ViewportPass;
import org.joml.Vector4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Applies the active split viewport through Mojang's backend-neutral GPU API. */
@Mixin(CommandEncoder.class)
public abstract class CommandEncoderSSMixin {
    @Inject(
        method = "clearColorAndDepthTextures(Lcom/mojang/blaze3d/textures/GpuTexture;Lorg/joml/Vector4fc;Lcom/mojang/blaze3d/textures/GpuTexture;D)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void splitTest$clearOnlyActiveViewport(
        GpuTexture colorTexture,
        Vector4fc clearColor,
        GpuTexture depthTexture,
        double clearDepth,
        CallbackInfo ci
    ) {
        if (!ViewportPass.hasActiveViewport()) {
            return;
        }

        RenderPass.RenderArea area = ViewportPass.areaFor(
            ViewportPass.activeViewport(),
            colorTexture.getWidth(0),
            colorTexture.getHeight(0)
        );
        ((CommandEncoder)(Object)this).clearColorAndDepthTextures(
            colorTexture,
            clearColor,
            depthTexture,
            clearDepth,
            area.x(),
            area.y(),
            area.width(),
            area.height()
        );
        ci.cancel();
    }

    @Inject(method = "createRenderPass(Lcom/mojang/blaze3d/systems/RenderPassDescriptor;)Lcom/mojang/blaze3d/systems/RenderPass;", at = @At("HEAD"))
    private void splitTest$constrainRenderArea(RenderPassDescriptor descriptor, CallbackInfoReturnable<RenderPass> cir) {
        ViewportPass.constrain(descriptor);
    }

    @Inject(method = "createRenderPass(Lcom/mojang/blaze3d/systems/RenderPassDescriptor;)Lcom/mojang/blaze3d/systems/RenderPass;", at = @At("RETURN"))
    private void splitTest$applyViewport(RenderPassDescriptor descriptor, CallbackInfoReturnable<RenderPass> cir) {
        if (!ViewportPass.hasActiveViewport() || descriptor.renderArea == null) {
            return;
        }

        RenderPass.RenderArea area = descriptor.renderArea;
        ((RenderPassSSAccessor)(Object)cir.getReturnValue()).splitTest$getBackend().setViewport(area.x(), area.y(), area.width(), area.height());
    }
}
