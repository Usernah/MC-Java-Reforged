package net.jr.mixin.compat.jade;

import net.jr.client.compat.jade.JadeRuntimeState;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import snownee.jade.JadeClient;
import snownee.jade.overlay.WailaTickHandler;

@Mixin(value = JadeClient.class, remap = false)
abstract class JadeClientMixin {
    @Shadow
    @Final
    private static WailaTickHandler tickHandler;

    @Inject(method = "tickHandler", at = @At("HEAD"), cancellable = true)
    private static void javaReforged$currentTickHandler(CallbackInfoReturnable<WailaTickHandler> cir) {
        cir.setReturnValue(JadeRuntimeState.tickHandler(tickHandler));
    }

    @Redirect(
        method = {"onKeyPressed", "lookupRecipes", "narrateKey"},
        at = @At(
            value = "FIELD",
            target = "Lsnownee/jade/JadeClient;tickHandler:Lsnownee/jade/overlay/WailaTickHandler;",
            opcode = Opcodes.GETSTATIC
        )
    )
    private static WailaTickHandler javaReforged$directTickHandler() {
        return JadeRuntimeState.tickHandler(tickHandler);
    }
}
