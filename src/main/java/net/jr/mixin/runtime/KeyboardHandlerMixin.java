package net.jr.mixin.runtime;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.jr.client.runtime.context.SlotExecution;
import net.jr.client.runtime.ui.SlotScreenManager;
import org.lwjgl.glfw.GLFW;
import net.jr.client.ui.dsl.ScreenEditorHooks;
import net.minecraft.client.gui.screens.Screen;
import net.jr.api.client.systemui.SystemUI;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.PreeditEvent;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = KeyboardHandler.class, priority = 2000)
public abstract class KeyboardHandlerMixin {
    @WrapMethod(method = "keyPress")
    private void javaReforged$keyPressForPrimaryClient(long windowPointer, int action, KeyEvent event, Operation<Void> original) {
        SlotExecution.runForKeyboardMouse(() -> {
            Screen screen = SlotScreenManager.activeScreenOrNull();
            // A Screen receives raw keyboard events, but its key mappings may
            // never enqueue consumeClick(). F10 must work with menus open.
            if (action == GLFW.GLFW_PRESS && event.key() == GLFW.GLFW_KEY_F10
                    && ScreenEditorHooks.toggle(screen)) {
                return;
            }
            if (screen == null || !ScreenEditorHooks.keyPress(screen, action, event)) {
                original.call(windowPointer, action, event);
            }
        });
    }

    @WrapMethod(method = "charTyped")
    private void javaReforged$charTypedForPrimaryClient(long windowPointer, CharacterEvent event, Operation<Void> original) {
        SlotExecution.runForKeyboardMouse(() -> {
            Screen screen = SlotScreenManager.activeScreenOrNull();
            if (screen != null && ScreenEditorHooks.charTyped(screen, event)) return;
            if (!SystemUI.charTyped(event)) {
                original.call(windowPointer, event);
            }
        });
    }

    @WrapMethod(method = "preeditCallback")
    private void javaReforged$preeditForPrimaryClient(long windowPointer, @Nullable PreeditEvent event, Operation<Void> original) {
        SlotExecution.runForKeyboardMouse(() -> {
            if (!SystemUI.blocksKeyboardMouse()) {
                original.call(windowPointer, event);
            }
        });
    }
}
