package net.jr.mixin.runtime;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.nio.file.Path;
import java.util.List;

import com.mojang.blaze3d.platform.Window;
import net.jr.api.client.ui.Viewport;
import net.jr.client.runtime.input.InputActionHandler;
import net.jr.client.runtime.context.SlotExecution;
import net.jr.client.runtime.viewport.ViewportArea;
import net.jr.client.runtime.viewport.ViewportGuiScale;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = MouseHandler.class, priority = 2000)
public abstract class MouseHandlerMixin {
    @WrapMethod(method = "onButton")
    private void javaReforged$mouseButtonForInputOwner(long windowPointer, MouseButtonInfo buttonInfo, int action, Operation<Void> original) {
        SlotExecution.runForKeyboardMouse(() -> original.call(windowPointer, buttonInfo, action));
    }

    @WrapMethod(method = "onScroll")
    private void javaReforged$mouseScrollForInputOwner(long windowPointer, double xOffset, double yOffset, Operation<Void> original) {
        SlotExecution.runForKeyboardMouse(() -> original.call(windowPointer, xOffset, yOffset));
    }

    @WrapMethod(method = "onMove")
    private void javaReforged$mouseMoveForInputOwner(long windowPointer, double xpos, double ypos, Operation<Void> original) {
        SlotExecution.runForKeyboardMouse(() -> original.call(windowPointer, xpos, ypos));
    }

    @WrapMethod(method = "onDrop")
    private void javaReforged$fileDropForInputOwner(long windowPointer, List<Path> files, int failedCount, Operation<Void> original) {
        SlotExecution.runForKeyboardMouse(() -> original.call(windowPointer, files, failedCount));
    }

    @WrapMethod(method = "handleAccumulatedMovement")
    private void javaReforged$mouseMovementForInputOwner(Operation<Void> original) {
        SlotExecution.runForKeyboardMouse(original::call);
    }

    @Redirect(
        method = {"onButton", "onScroll", "handleAccumulatedMovement"},
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MouseHandler;getScaledXPos(Lcom/mojang/blaze3d/platform/Window;)D")
    )
    private double javaReforged$readLocalMouseX(MouseHandler mouseHandler, com.mojang.blaze3d.platform.Window window) {
        ViewportArea viewport = Viewport.current();
        return viewport.windowMouseToLocalGuiX(mouseHandler.xpos())
            * ViewportGuiScale.screenLogicalWidth(viewport) / viewport.guiWidth();
    }

    @Redirect(
        method = {"onButton", "onScroll", "handleAccumulatedMovement"},
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MouseHandler;getScaledYPos(Lcom/mojang/blaze3d/platform/Window;)D")
    )
    private double javaReforged$readLocalMouseY(MouseHandler mouseHandler, com.mojang.blaze3d.platform.Window window) {
        ViewportArea viewport = Viewport.current();
        return viewport.windowMouseToLocalGuiY(mouseHandler.ypos())
            * ViewportGuiScale.screenLogicalHeight(viewport) / viewport.guiHeight();
    }

    @Redirect(
            method = "handleAccumulatedMovement",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MouseHandler;getScaledXPos(Lcom/mojang/blaze3d/platform/Window;D)D")
    )
    private double javaReforged$readAccumulatedLocalMouseX(Window window, double x) {
        ViewportArea viewport = Viewport.current();
        return x * ViewportGuiScale.screenLogicalWidth(viewport) / viewport.windowWidth();
    }

    @Redirect(
            method = "handleAccumulatedMovement",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MouseHandler;getScaledYPos(Lcom/mojang/blaze3d/platform/Window;D)D")
    )
    private double javaReforged$readAccumulatedLocalMouseY(Window window, double y) {
        ViewportArea viewport = Viewport.current();
        return y * ViewportGuiScale.screenLogicalHeight(viewport) / viewport.windowHeight();
    }

    @Redirect(
        method = "onButton",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/Screen;mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z"
        )
    )
    private boolean javaReforged$playContainerFeedbackFromRealMouseEvent(
        Screen screen,
        MouseButtonEvent event,
        boolean doubleClick
    ) {
        InputActionHandler.playPhysicalMouseContainerActionFeedback(screen, event);
        return screen.mouseClicked(event, doubleClick);
    }
}
