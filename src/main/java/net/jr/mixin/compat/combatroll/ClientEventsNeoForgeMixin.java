package net.jr.mixin.compat.combatroll;

import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.combat_roll.neoforge.client.ClientEventsNeoForge", remap = false)
abstract class ClientEventsNeoForgeMixin {
    @Inject(method = "onRenderHud", at = @At("HEAD"), cancellable = true)
    private static void javaReforged$useRegisteredHudObject(
        RenderGuiEvent.Post event,
        CallbackInfo callback
    ) {
        callback.cancel();
    }
}
