package net.jr.mixin.runtime;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.jr.client.runtime.render.target.ViewportRenderTargets;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @ModifyExpressionValue(method = {"render", "doEntityOutline"}, at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;entityOutlineTarget:Lcom/mojang/blaze3d/pipeline/RenderTarget;"))
    private RenderTarget javaReforged$resolveActiveOutlineTarget(RenderTarget original) {
        return ViewportRenderTargets.activeOutlineOr(original);
    }
}
