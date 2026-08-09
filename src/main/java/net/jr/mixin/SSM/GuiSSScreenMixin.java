package net.jr.mixin.SSM;

import javax.annotation.Nullable;
import net.jr.ClientRuntime.runtime.Client;
import net.jr.ClientRuntime.runtime.Screens;
import net.jr.ClientRuntime.test.ScreenProbe;
import net.jr.client.input.InputApi;
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
        ((GuiRawScreenAccessor)gui).splitTest$setRawScreen(screen);
        Client.bindScreen(gui, screen);
    }

    @Redirect(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Gui;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V"
        )
    )
    private void splitTest$skipGlobalDeathScreenAutoOpen(Gui gui, Screen screen) {
        if (!Screens.slotUiPassOwnsScreens()) {
            gui.setScreen(screen);
        }
    }

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void splitTest$handleTestScreenFlow(@Nullable Screen screen, CallbackInfo callback) {
        ScreenProbe.onSetScreen(screen, callback);
    }

    @Redirect(
        method = "setScreen",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;init(II)V")
    )
    private void splitTest$initializeScreenForSlot(Screen screen, int width, int height) {
        Screens.initialize(screen, Minecraft.getInstance(), width, height);
    }

    @Redirect(
        method = "setScreen",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MouseHandler;releaseMouse()V"),
        require = 0
    )
    private void splitTest$releaseMouseForSlotScreen(MouseHandler mouseHandler) {
        if (Screens.shouldOwnMouseModeTransitions() && !splitTest$isPrimaryInputSlot()) {
            return;
        }
        mouseHandler.releaseMouse();
    }

    @Redirect(
        method = "setScreen",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MouseHandler;grabMouse()V"),
        require = 0
    )
    private void splitTest$grabMouseForSlotScreen(MouseHandler mouseHandler) {
        if (Screens.shouldOwnMouseModeTransitions() && !splitTest$isPrimaryInputSlot()) {
            return;
        }
        mouseHandler.grabMouse();
    }

    private static boolean splitTest$isPrimaryInputSlot() {
        var active = Client.currentOrNull();
        return active == null || InputApi.canPhysicalMouseDriveClient(active.slotId());
    }
}
