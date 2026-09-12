package net.jr.mixin.compat.xaerominimap;

import com.mojang.blaze3d.platform.Window;
import net.jr.client.compat.xaero.XaeroViewport;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.viewport.ViewportArea;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import xaero.hud.minimap.config.util.MinimapConfigClientUtils;

@Mixin(value = MinimapConfigClientUtils.class, remap = false)
abstract class MinimapConfigClientUtilsMixin {
    @ModifyArg(
        method = "getAutoMinimapSize",
        at = @At(
            value = "INVOKE",
            target = "Ljava/lang/Math;max(II)I"
        ),
        index = 0
    )
    private static int javaReforged$removeSplitScreenMinimum(int minimum) {
        return ClientRuntime.INSTANCE.viewports().presentedCount() > 1 ? 1 : minimum;
    }

    @Redirect(
        method = "getAutoMinimapSize",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/Window;getWidth()I"
        )
    )
    private static int javaReforged$viewportWidth(Window window) {
        ViewportArea viewport = XaeroViewport.currentOrNull();
        return viewport == null ? window.getWidth() : viewport.width();
    }

    @Redirect(
        method = "getAutoMinimapSize",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/Window;getHeight()I"
        )
    )
    private static int javaReforged$viewportHeight(Window window) {
        ViewportArea viewport = XaeroViewport.currentOrNull();
        return viewport == null ? window.getHeight() : viewport.height();
    }

    @Redirect(
        method = "addAutoMCScaleValueToComponent",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/Window;getGuiScale()I"
        )
    )
    private static int javaReforged$viewportScale(Window window) {
        ViewportArea viewport = XaeroViewport.currentOrNull();
        return viewport == null ? window.getGuiScale() : viewport.guiScale();
    }
}
