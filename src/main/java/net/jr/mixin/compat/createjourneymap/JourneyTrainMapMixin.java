package net.jr.mixin.compat.createjourneymap;

import com.mojang.blaze3d.platform.Window;
import net.jr.client.compat.create.CreateScreenInput;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "com.simibubi.create.compat.trainmap.JourneyTrainMap", remap = false)
abstract class JourneyTrainMapMixin {
    @Redirect(method = "mouseClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MouseHandler;xpos()D"))
    private static double javaReforged$localMouseX(MouseHandler mouse) {
        return CreateScreenInput.localWindowX(mouse);
    }

    @Redirect(method = "mouseClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MouseHandler;ypos()D"))
    private static double javaReforged$localMouseY(MouseHandler mouse) {
        return CreateScreenInput.localWindowY(mouse);
    }

    @Redirect(method = {"mouseClick", "onRender"}, at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;getGuiScaledWidth()I"))
    private static int javaReforged$viewportGuiWidth(Window window) {
        return CreateScreenInput.guiWidth();
    }

    @Redirect(method = "mouseClick", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;getGuiScaledHeight()I"))
    private static int javaReforged$viewportGuiHeight(Window window) {
        return CreateScreenInput.guiHeight();
    }

    @Redirect(method = {"mouseClick", "onRender"}, at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;getScreenWidth()I"))
    private static int javaReforged$viewportScreenWidth(Window window) {
        return CreateScreenInput.screenWidth();
    }

    @Redirect(method = "mouseClick", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;getScreenHeight()I"))
    private static int javaReforged$viewportScreenHeight(Window window) {
        return CreateScreenInput.screenHeight();
    }
}
