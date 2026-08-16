package net.jr.mixin.runtime;

import net.jr.client.runtime.context.LocalClientAcces;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/** Makes each per-player Camera build projection and culling matrices for its own viewport. */
@Mixin(Camera.class)
public abstract class CameraSSMixin {
    @ModifyArgs(
        method = "update",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setupPerspective(FFFFF)V")
    )
    private void splitTest$useViewportProjectionSize(Args args) {
        if (LocalClientAcces.currentOrNull() != null && LocalClientAcces.hasViewport(LocalClientAcces.slotId())) {
            args.set(3, (float) LocalClientAcces.viewport(LocalClientAcces.slotId()).width());
            args.set(4, (float) LocalClientAcces.viewport(LocalClientAcces.slotId()).height());
        }
    }

    @ModifyArgs(
        method = "createProjectionMatrixForCulling",
        at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4f;perspective(FFFFZ)Lorg/joml/Matrix4f;")
    )
    private void splitTest$useViewportCullingAspect(Args args) {
        if (LocalClientAcces.currentOrNull() != null && LocalClientAcces.hasViewport(LocalClientAcces.slotId())) {
            args.set(1, LocalClientAcces.viewport(LocalClientAcces.slotId()).aspectRatio());
        }
    }
}
