package net.jr.mixin.compat.create;

import net.jr.client.compat.create.CreateRuntimeState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.simibubi.create.CreateClient", remap = false)
abstract class CreateClientMixin {
    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void javaReforged$enableSplitState(CallbackInfo ci) {
        CreateRuntimeState.markReady();
    }
}
