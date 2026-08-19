package net.jr.mixin.runtime;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps each viewport's fog uniform alive until the shared GPU submit ends. */
@Mixin(FogRenderer.class)
public abstract class FogRendererSSMixin {
    @Shadow
    @Final
    private MappableRingBuffer regularBuffer;

    @Inject(method = "getBuffer", at = @At("RETURN"))
    private void splitTest$rotateUniformAfterViewport(
        FogRenderer.FogMode mode,
        CallbackInfoReturnable<GpuBufferSlice> callback
    ) {
        if (mode == FogRenderer.FogMode.WORLD) {
            this.regularBuffer.rotate();
        }
    }

    @Inject(method = "endFrame", at = @At("HEAD"), cancellable = true)
    private void splitTest$uniformAlreadyRotatedPerViewport(CallbackInfo callback) {
        callback.cancel();
    }
}
