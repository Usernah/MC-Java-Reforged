package net.jr.mixin.runtime;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.nio.file.Path;
import java.util.List;
import net.jr.client.runtime.input.LocalScreenInput;
import net.jr.client.runtime.context.LocalClientExecution;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Routes physical mouse callbacks through the local client that owns keyboard and mouse input. */
@Mixin(value = MouseHandler.class, priority = 2000)
public abstract class MouseHandlerInputSlotMixin {
    @WrapMethod(method = "onButton")
    private void splitTest$mouseButtonForInputOwner(
        long windowPointer,
        MouseButtonInfo buttonInfo,
        int action,
        Operation<Void> original
    ) {
        LocalClientExecution.runForKeyboardMouse(() -> original.call(windowPointer, buttonInfo, action));
    }

    @WrapMethod(method = "onScroll")
    private void splitTest$mouseScrollForInputOwner(long windowPointer, double xOffset, double yOffset, Operation<Void> original) {
        LocalClientExecution.runForKeyboardMouse(() -> original.call(windowPointer, xOffset, yOffset));
    }

    @WrapMethod(method = "onMove")
    private void splitTest$mouseMoveForInputOwner(long windowPointer, double xpos, double ypos, Operation<Void> original) {
        LocalClientExecution.runForKeyboardMouse(() -> original.call(windowPointer, xpos, ypos));
    }

    @WrapMethod(method = "onDrop")
    private void splitTest$fileDropForInputOwner(long windowPointer, List<Path> files, int failedCount, Operation<Void> original) {
        LocalClientExecution.runForKeyboardMouse(() -> original.call(windowPointer, files, failedCount));
    }

    @WrapMethod(method = "handleAccumulatedMovement")
    private void splitTest$mouseMovementForInputOwner(Operation<Void> original) {
        LocalClientExecution.runForKeyboardMouse(original::call);
    }

    @Redirect(
        method = {"onButton", "onScroll", "handleAccumulatedMovement"},
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/MouseHandler;getScaledXPos(Lcom/mojang/blaze3d/platform/Window;)D"
        )
    )
    private double splitTest$readLocalMouseX(MouseHandler mouseHandler, com.mojang.blaze3d.platform.Window window) {
        return LocalScreenInput.localGuiX(mouseHandler);
    }

    @Redirect(
        method = {"onButton", "onScroll", "handleAccumulatedMovement"},
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/MouseHandler;getScaledYPos(Lcom/mojang/blaze3d/platform/Window;)D"
        )
    )
    private double splitTest$readLocalMouseY(MouseHandler mouseHandler, com.mojang.blaze3d.platform.Window window) {
        return LocalScreenInput.localGuiY(mouseHandler);
    }
}
