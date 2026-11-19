package net.jr.mixin.compat.jade;

import com.mojang.blaze3d.platform.Window;
import net.jr.client.compat.CompatHudDimensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import snownee.jade.impl.ui.BoxElementImpl;

@Mixin(value = BoxElementImpl.class, remap = false)
abstract class BoxElementImplMixin {
    @Redirect(
        method = "updateExpectedRect",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/Window;getGuiScaledWidth()I"
        )
    )
    private int javaReforged$viewportWidth(Window window) {
        return CompatHudDimensions.width(window);
    }

    @Redirect(
        method = "updateExpectedRect",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/Window;getGuiScaledHeight()I"
        )
    )
    private int javaReforged$viewportHeight(Window window) {
        return CompatHudDimensions.height(window);
    }
}
