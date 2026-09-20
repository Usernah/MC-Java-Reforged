package net.jr.mixin.compat.xaeroworldmap;

import com.mojang.blaze3d.platform.Window;
import net.jr.client.compat.xaero.XaeroWorldMapViewport;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.misc.Misc;

@Mixin(value = Misc.class, remap = false)
abstract class MiscMixin {
    @Inject(method = "getMouseX", at = @At("HEAD"), cancellable = true)
    private static void javaReforged$localMouseX(
        Minecraft minecraft,
        boolean directWindowCoordinates,
        CallbackInfoReturnable<Double> cir
    ) {
        Double mouseX = XaeroWorldMapViewport.mousePixelXOrNull(minecraft);
        if (mouseX != null) {
            cir.setReturnValue(mouseX);
        }
    }

    @Inject(method = "getMouseY", at = @At("HEAD"), cancellable = true)
    private static void javaReforged$localMouseY(
        Minecraft minecraft,
        boolean directWindowCoordinates,
        CallbackInfoReturnable<Double> cir
    ) {
        Double mouseY = XaeroWorldMapViewport.mousePixelYOrNull(minecraft);
        if (mouseY != null) {
            cir.setReturnValue(mouseY);
        }
    }

    @Redirect(
        method = "minecraftOrtho",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;getWidth()I")
    )
    private static int javaReforged$viewportFramebufferWidth(Window window) {
        return XaeroWorldMapViewport.framebufferWidth(window);
    }

    @Redirect(
        method = "minecraftOrtho",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;getHeight()I")
    )
    private static int javaReforged$viewportFramebufferHeight(Window window) {
        return XaeroWorldMapViewport.framebufferHeight(window);
    }

    @Redirect(
        method = "minecraftOrtho",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;getScreenWidth()I")
    )
    private static int javaReforged$viewportScreenWidth(Window window) {
        return XaeroWorldMapViewport.screenWidth(window);
    }

    @Redirect(
        method = "minecraftOrtho",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;getScreenHeight()I")
    )
    private static int javaReforged$viewportScreenHeight(Window window) {
        return XaeroWorldMapViewport.screenHeight(window);
    }

    @Redirect(
        method = "minecraftOrtho",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;getGuiScale()I")
    )
    private static int javaReforged$viewportGuiScale(Window window) {
        return XaeroWorldMapViewport.guiScale(window);
    }
}
