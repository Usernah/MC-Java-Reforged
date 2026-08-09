package net.jr.mixin.controlhints;

import net.jr.client.input.InputApi;
import net.jr.client.input.runtime.GamepadInputProcessor;
import net.jr.client.input.runtime.MappedActionProcessor;
import net.jr.client.input.simulation.InputSimulation;
import net.jr.client.ui.navigation.UiInputModeController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonInfo;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerInputModeMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "onMove", at = @At("HEAD"), cancellable = true)
    private void javareforged$trackMouseInputMode(long windowPointer, double xpos, double ypos, CallbackInfo ci) {
        if (InputSimulation.handleMouseMove(windowPointer, xpos, ypos)) {
            InputApi.markGamepadInput();
            ci.cancel();
            return;
        }

        GamepadInputProcessor.notifyPhysicalMouseMove(xpos, ypos);
    }

    @Inject(
        method = "onButton",
        at = @At(
            value = "INVOKE",
            target = "Lnet/neoforged/neoforge/client/ClientHooks;onMouseButtonPre(Lnet/minecraft/client/input/MouseButtonInfo;I)Z"
        ),
        cancellable = true
    )
    private void javareforged$consumeMappedScreenMouseButton(
        long windowPointer,
        MouseButtonInfo buttonInfo,
        int action,
        CallbackInfo ci
    ) {
        if (windowPointer != this.minecraft.getWindow().handle()) {
            return;
        }

        int button = buttonInfo.button();
        if (InputSimulation.handleMouseButton(windowPointer, button, action, buttonInfo.modifiers())) {
            if (InputSimulation.isActive()) {
                InputApi.markGamepadInput();
            }
            ci.cancel();
            return;
        }

        if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_RELEASE) {
            InputApi.markKeyboardMouseInput();
            UiInputModeController.notifyPhysicalPointerActivity();
        }

        Screen screen = this.minecraft.gui.screen();
        if (MappedActionProcessor.suppressConsumedScreenMouseButton(screen, button, action)) {
            if (screen != null) {
                screen.afterMouseAction();
            }
            ci.cancel();
            return;
        }

        if (this.minecraft.gui.overlay() != null || screen == null) {
            return;
        }

        boolean handled = action == GLFW.GLFW_PRESS
            ? MappedActionProcessor.handleScreenMouseButton(screen, button)
            : action == GLFW.GLFW_RELEASE && MappedActionProcessor.consumeHandledMouseRelease(screen, button);
        if (!handled) {
            return;
        }

        if (action == GLFW.GLFW_PRESS) {
            MappedActionProcessor.rememberSuppressedScreenMouseButton(button);
        }
        screen.afterMouseAction();
        ci.cancel();
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void javareforged$consumeMappedScreenMouseScroll(long windowPointer, double xOffset, double yOffset, CallbackInfo ci) {
        if (InputSimulation.handleMouseScroll(windowPointer, xOffset, yOffset)) {
            InputApi.markGamepadInput();
            ci.cancel();
            return;
        }

        if (yOffset != 0.0D || xOffset != 0.0D) {
            InputApi.markKeyboardMouseInput();
            UiInputModeController.notifyPhysicalPointerActivity();
        }

        if (windowPointer != this.minecraft.getWindow().handle()
            || this.minecraft.gui.overlay() != null
            || this.minecraft.gui.screen() == null) {
            return;
        }

        Screen screen = this.minecraft.gui.screen();
        if (!MappedActionProcessor.handleScreenMouseScroll(screen, yOffset)) {
            return;
        }

        screen.afterMouseAction();
        ci.cancel();
    }
}

