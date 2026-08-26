package net.jr.mixin.runtime;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.jr.client.runtime.render.buffer.ViewportUniformRotation;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
    @Shadow
    @Final
    private MappableRingBuffer regularBuffer;

    @Inject(method = "getBuffer", at = @At("RETURN"))
    private void javaReforged$rotateUniformAfterViewport(
        FogRenderer.FogMode mode,
        CallbackInfoReturnable<GpuBufferSlice> callback
    ) {
        ViewportUniformRotation.rotateWorldFog(mode, this.regularBuffer);
    }

    @Inject(method = "endFrame", at = @At("HEAD"), cancellable = true)
    private void javaReforged$uniformAlreadyRotatedPerViewport(CallbackInfo callback) {
        if (ViewportUniformRotation.replacesEndFrameRotation()) {
            callback.cancel();
        }
    }
}
