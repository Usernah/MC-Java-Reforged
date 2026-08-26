package net.jr.mixin.runtime;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.renderer.CloudRenderer;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.world.phys.Vec3;
import net.jr.client.runtime.render.buffer.ViewportUniformRotation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CloudRenderer.class)
public abstract class CloudRendererMixin {
    @Shadow
    @Final
    private MappableRingBuffer ubo;

    @Inject(method = "render", at = @At("RETURN"))
    private void javaReforged$rotateUniformsAfterViewport(
        int color,
        CloudStatus cloudStatus,
        float bottomY,
        int range,
        Vec3 cameraPosition,
        long gameTime,
        float partialTicks,
        CallbackInfo callback
    ) {
        ViewportUniformRotation.rotate(this.ubo);
    }

    @Inject(method = "endFrame", at = @At("HEAD"), cancellable = true)
    private void javaReforged$uniformsAlreadyRotatedPerViewport(CallbackInfo callback) {
        if (ViewportUniformRotation.replacesEndFrameRotation()) {
            callback.cancel();
        }
    }
}
