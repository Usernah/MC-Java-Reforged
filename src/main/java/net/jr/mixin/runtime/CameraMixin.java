package net.jr.mixin.runtime;

import net.jr.client.runtime.viewport.ViewportProjection;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @ModifyArgs(
        method = "update",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setupPerspective(FFFFF)V")
    )
    private void javaReforged$useViewportProjectionSize(Args args) {
        ViewportProjection.Size size = ViewportProjection.activeSizeOrNull();
        if (size != null) {
            args.set(3, (float) size.width());
            args.set(4, (float) size.height());
        }
    }

    @ModifyArgs(
        method = "createProjectionMatrixForCulling",
        at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4f;perspective(FFFFZ)Lorg/joml/Matrix4f;")
    )
    private void javaReforged$useViewportCullingAspect(Args args) {
        ViewportProjection.Size size = ViewportProjection.activeSizeOrNull();
        if (size != null) {
            args.set(1, size.aspectRatio());
        }
    }
}
