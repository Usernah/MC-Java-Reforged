package net.jr.mixin.compat.xaerominimap;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.platform.Window;
import net.jr.client.compat.xaero.XaeroMinimapRuntimeState;
import net.jr.client.compat.xaero.XaeroViewport;
import net.jr.client.runtime.viewport.ViewportArea;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xaero.hud.Hud;
import xaero.hud.render.HudRenderer;

@Mixin(value = HudRenderer.class, remap = false)
abstract class HudRendererMixin {
    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/Window;getGuiScaledWidth()I"
        )
    )
    private int javaReforged$viewportWidth(Window window) {
        ViewportArea viewport = XaeroViewport.currentOrNull();
        return viewport == null ? window.getGuiScaledWidth() : viewport.logicalGuiWidth();
    }

    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/Window;getGuiScaledHeight()I"
        )
    )
    private int javaReforged$viewportHeight(Window window) {
        ViewportArea viewport = XaeroViewport.currentOrNull();
        return viewport == null ? window.getGuiScaledHeight() : viewport.logicalGuiHeight();
    }

    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/Window;getGuiScale()I"
        )
    )
    private int javaReforged$viewportScale(Window window) {
        ViewportArea viewport = XaeroViewport.currentOrNull();
        return viewport == null ? window.getGuiScale() : viewport.guiScale();
    }

    @WrapMethod(method = "render")
    private void javaReforged$renderCurrentClient(
        Hud hud,
        GuiGraphicsExtractor graphics,
        float partialTicks,
        Operation<Void> original
    ) {
        XaeroMinimapRuntimeState.run(() -> original.call(hud, graphics, partialTicks));
    }
}
