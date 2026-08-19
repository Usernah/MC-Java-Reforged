package net.jr.mixin.runtime;

import javax.annotation.Nullable;
import net.jr.client.input.InputApi;
import net.jr.client.input.runtime.GamepadInputProcessor;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.bridge.GuiScreenAccess;
import net.jr.client.runtime.context.SlotScope;
import net.jr.client.runtime.ui.LocalScreenManager;
import net.jr.client.runtime.ui.LocalScreenTransitionHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiSSScreenMixin {
    @Redirect(
        method = "setScreen",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/gui/Gui;screen:Lnet/minecraft/client/gui/screens/Screen;",
            opcode = Opcodes.PUTFIELD
        )
    )
    private void splitTest$captureResolvedScreenWrite(Gui gui, @Nullable Screen screen) {
        GuiScreenAccess.setScreen(gui, screen);
    }

    @Redirect(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Gui;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V"
        )
    )
    private void splitTest$routeDeathScreenAutoOpen(Gui gui, Screen screen) {
        gui.setScreen(screen);
    }

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void splitTest$handleScreenFlow(@Nullable Screen screen, CallbackInfo callback) {
        LocalScreenTransitionHandler.onSetScreen(screen, callback);
    }

    @Redirect(
        method = "setScreen",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;init(II)V")
    )
    private void splitTest$initializeScreenForSlot(Screen screen, int width, int height) {
        LocalScreenManager.initialize(screen, Minecraft.getInstance(), width, height);
    }

    @Redirect(
        method = "setScreen",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MouseHandler;releaseMouse()V"),
        require = 0
    )
    private void splitTest$releaseMouseForSlotScreen(MouseHandler mouseHandler) {
        int slotId = currentSlotId();
        if (!InputApi.canPhysicalMouseDriveSlot(slotId)) {
            return;
        }
        mouseHandler.releaseMouse();
        if (ClientRuntime.INSTANCE.viewports().presentedCount() > 1) {
            GamepadInputProcessor.centerPhysicalCursorForScreen(slotId);
        }
    }

    @Redirect(
        method = "setScreen",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MouseHandler;grabMouse()V"),
        require = 0
    )
    private void splitTest$grabMouseForSlotScreen(MouseHandler mouseHandler) {
        if (!InputApi.canPhysicalMouseDriveSlot(currentSlotId())) {
            return;
        }
        mouseHandler.grabMouse();
    }

    private static int currentSlotId() {
        Integer slotId = SlotScope.idOrNull();
        return slotId != null ? slotId : 0;
    }
}
