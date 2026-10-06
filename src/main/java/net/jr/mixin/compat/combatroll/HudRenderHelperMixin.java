package net.jr.mixin.compat.combatroll;

import com.mojang.blaze3d.platform.Window;
import net.combat_roll.client.gui.HudRenderHelper;
import net.jr.api.client.ui.Viewport;
import net.jr.client.runtime.viewport.ViewportHudLayout;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = HudRenderHelper.class, remap = false)
abstract class HudRenderHelperMixin {
    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/Window;getGuiScaledWidth()I"
        )
    )
    private static int javaReforged$viewportWidth(Window window) {
        return Viewport.current().logicalGuiWidth();
    }

    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/Window;getGuiScaledHeight()I"
        )
    )
    private static int javaReforged$viewportHeight(Window window) {
        return Viewport.current().logicalGuiHeight();
    }

    @ModifyArg(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/combat_roll/client/gui/HudElement$Origin;getPoint(II)Lnet/minecraft/world/phys/Vec2;"
        ),
        index = 1
    )
    private static int javaReforged$bottomAnchor(int viewportHeight) {
        return ViewportHudLayout.bottomAnchorY(viewportHeight);
    }
}
