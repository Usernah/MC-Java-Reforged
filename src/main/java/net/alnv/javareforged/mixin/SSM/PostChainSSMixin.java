package net.alnv.javareforged.mixin.SSM;

import net.alnv.javareforged.ClientRuntime.runtime.ViewportPostChain;
import net.minecraft.client.renderer.PostChain;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PostChain.class)
public abstract class PostChainSSMixin {
    @Inject(method = "process", at = @At("HEAD"))
    private void splitTest$captureViewportMainTarget(float partialTicks, CallbackInfo ci) {
        ViewportPostChain.beforeProcess((PostChain)(Object)this);
    }

    @Inject(method = "process", at = @At("RETURN"))
    private void splitTest$drawViewportMainTarget(float partialTicks, CallbackInfo ci) {
        ViewportPostChain.afterProcess((PostChain)(Object)this);
    }
}
