package net.jr.mixin.compat.xaerominimap;

import net.jr.client.compat.xaero.XaeroMinimapPipRenderers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.minimap.MinimapProcessor;
import xaero.hud.minimap.render.MinimapPipRenderState;

@Mixin(value = MinimapProcessor.class, remap = false)
abstract class MinimapProcessorMixin {
    @Shadow
    public abstract MinimapPipRenderState getRenderState();

    @Shadow
    public abstract MinimapPipRenderState.Enlarged getEnlargedRenderState();

    @Inject(method = "cleanup", at = @At("HEAD"))
    private void javaReforged$releaseRenderTargets(CallbackInfo callback) {
        XaeroMinimapPipRenderers.release(this.getRenderState());
        XaeroMinimapPipRenderers.release(this.getEnlargedRenderState());
    }
}
