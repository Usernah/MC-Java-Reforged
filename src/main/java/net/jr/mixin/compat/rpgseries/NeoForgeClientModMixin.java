package net.jr.mixin.compat.rpgseries;

import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.spell_engine.neoforge.client.NeoForgeClientMod", remap = false)
abstract class NeoForgeClientModMixin {
    @Inject(method = "registerGuiLayers", at = @At("HEAD"), cancellable = true)
    private static void javaReforged$useRegisteredHudObject(
        RegisterGuiLayersEvent event,
        CallbackInfo callback
    ) {
        callback.cancel();
    }
}
