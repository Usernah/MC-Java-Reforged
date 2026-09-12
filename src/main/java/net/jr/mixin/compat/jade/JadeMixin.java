package net.jr.mixin.compat.jade;

import net.jr.client.compat.jade.JadeRuntimeState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import snownee.jade.Jade;
import snownee.jade.impl.config.WailaConfig;

@Mixin(value = Jade.class, remap = false)
abstract class JadeMixin {
    @Inject(method = "config", at = @At("RETURN"), cancellable = true)
    private static void javaReforged$currentOptions(CallbackInfoReturnable<WailaConfig> cir) {
        cir.setReturnValue(JadeRuntimeState.options(cir.getReturnValue()));
    }

    @Inject(method = "saveConfig", at = @At("HEAD"), cancellable = true)
    private static void javaReforged$saveCurrentOptions(CallbackInfo ci) {
        if (JadeRuntimeState.saveOptions()) {
            ci.cancel();
        }
    }

    @Inject(method = "invalidateConfig", at = @At("HEAD"), cancellable = true)
    private static void javaReforged$invalidateCurrentOptions(CallbackInfo ci) {
        if (JadeRuntimeState.invalidateOptions()) {
            ci.cancel();
        }
    }

    @Inject(method = "resetConfig", at = @At("HEAD"), cancellable = true)
    private static void javaReforged$resetCurrentOptions(CallbackInfo ci) {
        if (JadeRuntimeState.resetOptions(Jade.rootConfig())) {
            ci.cancel();
        }
    }
}
