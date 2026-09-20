package net.jr.mixin.compat.create;

import com.mojang.blaze3d.platform.Window;
import net.jr.client.compat.create.CreateScreenInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen", remap = false)
abstract class StockKeeperRequestScreenMixin {
    @Redirect(method = "init", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;getGuiScaledHeight()I"))
    private int javaReforged$viewportHeight(Window window) {
        return CreateScreenInput.guiHeight();
    }
}
