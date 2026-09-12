package net.jr.mixin.compat.jade;

import net.jr.client.compat.jade.JadeRuntimeState;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import snownee.jade.overlay.RayTracing;
import snownee.jade.overlay.WailaTickHandler;

@Mixin(value = WailaTickHandler.class, remap = false)
abstract class WailaTickHandlerMixin {
    @Redirect(
        method = "tickClient",
        at = @At(
            value = "FIELD",
            target = "Lsnownee/jade/overlay/RayTracing;INSTANCE:Lsnownee/jade/overlay/RayTracing;",
            opcode = Opcodes.GETSTATIC
        )
    )
    private RayTracing javaReforged$currentRayTracing() {
        return JadeRuntimeState.rayTracing(RayTracing.INSTANCE);
    }
}
