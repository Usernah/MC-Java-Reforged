package net.jr.mixin.compat.xaerominimap;

import com.mojang.blaze3d.platform.Window;
import net.jr.api.client.ui.Viewport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xaero.hud.minimap.waypoint.render.WaypointMapRenderer;

@Mixin(value = WaypointMapRenderer.class, remap = false)
abstract class WaypointMapRendererMixin {
    @Redirect(
        method = "drawSetChange",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/Window;getGuiScaledWidth()I"
        )
    )
    private int javaReforged$viewportWidth(Window window) {
        return Viewport.width();
    }

    @Redirect(
        method = "drawSetChange",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/Window;getGuiScaledHeight()I"
        )
    )
    private int javaReforged$viewportHeight(Window window) {
        return Viewport.height();
    }
}
