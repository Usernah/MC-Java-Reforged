package net.jr.mixin.SSM;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.jr.ClientRuntime.runtime.ClientBoundary;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.PreeditEvent;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

/** Installs the keyboard/mouse owner's local-client context at the GLFW boundary. */
@Mixin(value = KeyboardHandler.class, priority = 2000)
public abstract class KeyboardHandlerSSMixin {
    @WrapMethod(method = "keyPress")
    private void splitTest$keyPressForPrimaryClient(long windowPointer, int action, KeyEvent event, Operation<Void> original) {
        ClientBoundary.runForKeyboardMouse(() -> original.call(windowPointer, action, event));
    }

    @WrapMethod(method = "charTyped")
    private void splitTest$charTypedForPrimaryClient(long windowPointer, CharacterEvent event, Operation<Void> original) {
        ClientBoundary.runForKeyboardMouse(() -> original.call(windowPointer, event));
    }

    @WrapMethod(method = "preeditCallback")
    private void splitTest$preeditForPrimaryClient(long windowPointer, @Nullable PreeditEvent event, Operation<Void> original) {
        ClientBoundary.runForKeyboardMouse(() -> original.call(windowPointer, event));
    }
}
