package net.jr.mixin.compat.createcatnip;

import com.mojang.blaze3d.platform.Window;
import net.jr.client.compat.create.CreateScreenInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.createmod.catnip.impl.client.placement.PlacementClient", remap = false)
abstract class PlacementClientMixin {
    @Redirect(method = "projectToPlayerView", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;getGuiScaledHeight()I"))
    private static int javaReforged$viewportHeight(Window window) {
        return CreateScreenInput.guiHeight();
    }
}
