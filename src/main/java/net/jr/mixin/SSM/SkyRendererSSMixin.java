package net.jr.mixin.SSM;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.jr.ClientRuntime.runtime.SlotRenderTargets;
import net.minecraft.client.renderer.SkyRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Makes the one shared SkyRenderer draw into the complete frame graph of the active slot. */
@Mixin(SkyRenderer.class)
public abstract class SkyRendererSSMixin {
    @ModifyExpressionValue(
        method = {
            "renderSkyDisc",
            "renderDarkDisc",
            "renderSun",
            "renderMoon",
            "renderStars",
            "renderSunriseAndSunset",
            "renderEndSky",
            "renderEndFlash"
        },
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/SkyRenderer;renderTarget:Lcom/mojang/blaze3d/pipeline/RenderTarget;")
    )
    private RenderTarget splitTest$resolveActiveTarget(RenderTarget original) {
        return SlotRenderTargets.activeMainOr(original);
    }
}
