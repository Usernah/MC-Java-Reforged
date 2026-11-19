package net.jr.mixin.compat.xaerominimap;

import com.mojang.blaze3d.platform.Window;
import net.jr.client.compat.CompatHudDimensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xaero.hud.gui.util.GuiUtils;

@Mixin(value = GuiUtils.class, remap = false)
abstract class GuiUtilsMixin {
    @Redirect(
        method = "getAutoUIScale",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/Window;getWidth()I"
        )
    )
    private static int javaReforged$viewportAutoScaleWidth(Window window) {
        return CompatHudDimensions.framebufferWidth(window);
    }

    @Redirect(
        method = "getAutoUIScale",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/Window;getHeight()I"
        )
    )
    private static int javaReforged$viewportAutoScaleHeight(Window window) {
        return CompatHudDimensions.framebufferHeight(window);
    }

    @Redirect(
        method = "getUIScale",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/Window;getGuiScale()I"
        )
    )
    private static int javaReforged$viewportSystemScale(Window window) {
        return CompatHudDimensions.rasterScale(window);
    }
}
