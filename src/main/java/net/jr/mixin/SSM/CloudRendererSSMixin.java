package net.jr.mixin.SSM;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.renderer.CloudRenderer;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps each viewport's cloud uniforms alive until the shared GPU submit ends. */
@Mixin(CloudRenderer.class)
public abstract class CloudRendererSSMixin {
    @Shadow
    @Final
    private MappableRingBuffer ubo;

    @Inject(method = "render", at = @At("RETURN"))
    private void splitTest$rotateUniformsAfterViewport(
        int color,
        CloudStatus cloudStatus,
        float bottomY,
        int range,
        Vec3 cameraPosition,
        long gameTime,
        float partialTicks,
        CallbackInfo callback
    ) {
        this.ubo.rotate();
    }

    @Inject(method = "endFrame", at = @At("HEAD"), cancellable = true)
    private void splitTest$uniformsAlreadyRotatedPerViewport(CallbackInfo callback) {
        callback.cancel();
    }
}
