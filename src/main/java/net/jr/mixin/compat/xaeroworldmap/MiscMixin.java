package net.jr.mixin.compat.xaeroworldmap;

import com.mojang.blaze3d.platform.Window;
import net.jr.api.client.ui.Cursor;
import net.jr.client.compat.xaero.XaeroWorldMapViewport;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xaero.map.misc.Misc;

@Mixin(value = Misc.class, remap = false)
abstract class MiscMixin {
    @Redirect(
        method = "getMouseX",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/MouseHandler;xpos()D"
        )
    )
    private static double javaReforged$localWindowMouseX(MouseHandler mouse) {
        return Cursor.localWindowX();
    }

    @Redirect(
        method = "getMouseY",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/MouseHandler;ypos()D"
        )
    )
    private static double javaReforged$localWindowMouseY(MouseHandler mouse) {
        return Cursor.localWindowY();
    }

    @Redirect(
        method = "getMouseX",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/Window;getWidth()I"
        )
    )
    private static int javaReforged$localFramebufferWidth(Window window) {
        return XaeroWorldMapViewport.framebufferWidth(window);
    }

    @Redirect(
        method = "getMouseX",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/Window;getScreenWidth()I"
        )
    )
    private static int javaReforged$localWindowWidth(Window window) {
        return XaeroWorldMapViewport.screenWidth(window);
    }

    @Redirect(
        method = "getMouseY",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/Window;getHeight()I"
        )
    )
    private static int javaReforged$localFramebufferHeight(Window window) {
        return XaeroWorldMapViewport.framebufferHeight(window);
    }

    @Redirect(
        method = "getMouseY",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/Window;getScreenHeight()I"
        )
    )
    private static int javaReforged$localWindowHeight(Window window) {
        return XaeroWorldMapViewport.screenHeight(window);
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
