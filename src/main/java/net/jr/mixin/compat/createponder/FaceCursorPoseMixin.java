package net.jr.mixin.compat.createponder;

import com.mojang.blaze3d.platform.Window;
import net.jr.client.compat.create.CreateScreenInput;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.createmod.ponder.api.client.element.ParrotPose$FaceCursorPose", remap = false)
abstract class FaceCursorPoseMixin {
    @Redirect(method = "getFacedVec", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MouseHandler;xpos()D"))
    private double javaReforged$localMouseX(MouseHandler mouse) {
        return CreateScreenInput.localWindowX(mouse);
    }

    @Redirect(method = "getFacedVec", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MouseHandler;ypos()D"))
    private double javaReforged$localMouseY(MouseHandler mouse) {
        return CreateScreenInput.localWindowY(mouse);
    }

    @Redirect(method = "getFacedVec", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;getGuiScaledWidth()I"))
    private int javaReforged$viewportGuiWidth(Window window) {
        return CreateScreenInput.guiWidth();
    }

    @Redirect(method = "getFacedVec", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;getGuiScaledHeight()I"))
    private int javaReforged$viewportGuiHeight(Window window) {
        return CreateScreenInput.guiHeight();
    }

    @Redirect(method = "getFacedVec", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;getScreenWidth()I"))
    private int javaReforged$viewportScreenWidth(Window window) {
        return CreateScreenInput.screenWidth();
    }

    @Redirect(method = "getFacedVec", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;getScreenHeight()I"))
    private int javaReforged$viewportScreenHeight(Window window) {
        return CreateScreenInput.screenHeight();
    }
}
