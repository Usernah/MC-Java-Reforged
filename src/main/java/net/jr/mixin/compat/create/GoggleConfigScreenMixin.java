package net.jr.mixin.compat.create;

import com.mojang.blaze3d.platform.Window;
import net.jr.client.compat.create.CreateScreenInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "com.simibubi.create.content.equipment.goggles.GoggleConfigScreen", remap = false)
abstract class GoggleConfigScreenMixin {
    @Redirect(method = "init", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;getGuiScaledWidth()I"))
    private int javaReforged$viewportWidth(Window window) {
        return CreateScreenInput.guiWidth();
    }

    @Redirect(method = "init", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;getGuiScaledHeight()I"))
    private int javaReforged$viewportHeight(Window window) {
        return CreateScreenInput.guiHeight();
    }
}
