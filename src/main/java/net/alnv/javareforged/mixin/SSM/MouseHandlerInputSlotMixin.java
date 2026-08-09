package net.alnv.javareforged.mixin.SSM;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.alnv.javareforged.ClientRuntime.input.ScreenInput;
import net.alnv.javareforged.ClientRuntime.runtime.ClientBoundary;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.annotation.Nullable;

@Mixin(value = MouseHandler.class, priority = 2000)
public abstract class MouseHandlerInputSlotMixin {
    @WrapMethod(method = "onPress(JIII)V")
    private void splitTest$mousePressForPrimaryClient(long windowPointer, int button, int action, int mods, Operation<Void> original) {
        ClientBoundary.runPrimary(Minecraft.getInstance(), () -> original.call(windowPointer, button, action, mods));
    }

    @WrapMethod(method = "onScroll(JDD)V")
    private void splitTest$mouseScrollForPrimaryClient(long windowPointer, double xOffset, double yOffset, Operation<Void> original) {
        ClientBoundary.runPrimary(Minecraft.getInstance(), () -> original.call(windowPointer, xOffset, yOffset));
    }

    @WrapMethod(method = "onMove(JDD)V")
    private void splitTest$mouseMoveForPrimaryClient(long windowPointer, double xpos, double ypos, Operation<Void> original) {
        ClientBoundary.runPrimary(Minecraft.getInstance(), () -> original.call(windowPointer, xpos, ypos));
    }

    @WrapMethod(method = "handleAccumulatedMovement()V")
    private void splitTest$mouseMovementForPrimaryClient(Operation<Void> original) {
        ClientBoundary.runPrimary(Minecraft.getInstance(), () -> original.call());
    }

    @Redirect(
            method = {"onPress", "onScroll", "handleAccumulatedMovement"},
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;screen:Lnet/minecraft/client/gui/screens/Screen;")
    )
    @Nullable
    private Screen splitTest$readTargetScreen(Minecraft minecraft) {
        return ScreenInput.screen();
    }

    @Redirect(
            method = {"onPress", "onScroll", "handleAccumulatedMovement"},
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/MouseHandler;xpos:D")
    )
    private double splitTest$readLocalMouseX(MouseHandler mouseHandler) {
        return ScreenInput.localWindowX(mouseHandler);
    }

    @Redirect(
            method = {"onPress", "onScroll", "handleAccumulatedMovement"},
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/MouseHandler;ypos:D")
    )
    private double splitTest$readLocalMouseY(MouseHandler mouseHandler) {
        return ScreenInput.localWindowY(mouseHandler);
    }

    @Redirect(
            method = {"onPress", "onScroll", "handleAccumulatedMovement"},
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;getGuiScaledWidth()I")
    )
    private int splitTest$readViewportGuiWidth(com.mojang.blaze3d.platform.Window window) {
        return ScreenInput.guiWidth();
    }

    @Redirect(
            method = {"onPress", "onScroll", "handleAccumulatedMovement"},
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;getGuiScaledHeight()I")
    )
    private int splitTest$readViewportGuiHeight(com.mojang.blaze3d.platform.Window window) {
        return ScreenInput.guiHeight();
    }

    @Redirect(
            method = {"onPress", "onScroll", "handleAccumulatedMovement"},
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;getScreenWidth()I")
    )
    private int splitTest$readViewportWindowWidth(com.mojang.blaze3d.platform.Window window) {
        return ScreenInput.windowWidth();
    }

    @Redirect(
            method = {"onPress", "onScroll", "handleAccumulatedMovement"},
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;getScreenHeight()I")
    )
    private int splitTest$readViewportWindowHeight(com.mojang.blaze3d.platform.Window window) {
        return ScreenInput.windowHeight();
    }

    @Redirect(
            method = {"onPress", "handleAccumulatedMovement"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/Screen;wrapScreenError(Ljava/lang/Runnable;Ljava/lang/String;Ljava/lang/String;)V"
            ),
            require = 0
    )
    private void splitTest$runScreenEventForTargetSlot(Runnable event, String errorTitle, String screenName) {
        ScreenInput.runEvent(event, errorTitle, screenName);
    }
}
