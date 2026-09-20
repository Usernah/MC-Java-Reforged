package net.jr.mixin.compat.createflywheel;

import com.mojang.blaze3d.platform.Window;
import net.jr.client.runtime.viewport.ViewportRenderScope;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "dev.engine_room.create_flywheel.backend.engine.uniform.FrameUniforms", remap = false)
abstract class FrameUniformsMixin {
    @Redirect(method = "update", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;getWidth()I"))
    private static int javaReforged$viewportWidth(Window window) {
        return ViewportRenderScope.shaderWidth(window);
    }

    @Redirect(method = "update", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;getHeight()I"))
    private static int javaReforged$viewportHeight(Window window) {
        return ViewportRenderScope.shaderHeight(window);
    }
}
