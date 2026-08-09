package net.jr.mixin.SSM;

import net.jr.ClientRuntime.runtime.Client;
import net.minecraft.client.Camera;
import org.joml.Matrix4f;
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
        if (Client.currentOrNull() != null && Client.hasViewport(Client.slotId())) {
            args.set(3, (float)Client.viewport(Client.slotId()).width());
            args.set(4, (float)Client.viewport(Client.slotId()).height());
        }
    }

    @ModifyArgs(
        method = "createProjectionMatrixForCulling",
        at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4f;perspective(FFFFZ)Lorg/joml/Matrix4f;")
    )
    private void splitTest$useViewportCullingAspect(Args args) {
        if (Client.currentOrNull() != null && Client.hasViewport(Client.slotId())) {
            args.set(1, Client.viewport(Client.slotId()).aspectRatio());
        }
    }
}
