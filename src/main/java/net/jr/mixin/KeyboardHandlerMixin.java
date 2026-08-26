package net.jr.mixin;

import net.jr.client.input.runtime.KeyboardInputHandler;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void javaReforged$handleScreenMappings(
            long windowPointer,
            int action,
            KeyEvent event,
            CallbackInfo ci
    ) {
        if (KeyboardInputHandler.handle(windowPointer, action, event)) {
            ci.cancel();
        }
    }
}
