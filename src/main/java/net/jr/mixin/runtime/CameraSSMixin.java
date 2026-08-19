package net.jr.mixin.runtime;

import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.context.SlotScope;
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
        Integer slotId = SlotScope.idOrNull();
        if (slotId != null && ClientRuntime.INSTANCE.viewports().hasViewport(slotId)) {
            args.set(3, (float)ClientRuntime.INSTANCE.viewports().viewport(slotId).width());
            args.set(4, (float)ClientRuntime.INSTANCE.viewports().viewport(slotId).height());
        }
    }

    @ModifyArgs(
        method = "createProjectionMatrixForCulling",
        at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4f;perspective(FFFFZ)Lorg/joml/Matrix4f;")
    )
    private void splitTest$useViewportCullingAspect(Args args) {
        Integer slotId = SlotScope.idOrNull();
        if (slotId != null && ClientRuntime.INSTANCE.viewports().hasViewport(slotId)) {
            args.set(1, ClientRuntime.INSTANCE.viewports().viewport(slotId).aspectRatio());
        }
    }
}
