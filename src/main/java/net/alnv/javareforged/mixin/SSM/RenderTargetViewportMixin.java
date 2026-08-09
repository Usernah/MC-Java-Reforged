package net.alnv.javareforged.mixin.SSM;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.alnv.javareforged.ClientRuntime.runtime.ViewportPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderTarget.class)
public abstract class RenderTargetViewportMixin {
    @Inject(method = "bindWrite", at = @At("RETURN"))
    private void splitTest$restoreViewportAfterBind(boolean setViewport, CallbackInfo ci) {
        ViewportPass.applyActiveViewport((RenderTarget)(Object)this);
    }

    @Inject(method = "clear", at = @At("HEAD"))
    private void splitTest$beginViewportClear(boolean clearError, CallbackInfo ci) {
        ViewportPass.beginClear((RenderTarget)(Object)this);
    }

    @Inject(method = "clear", at = @At("RETURN"))
    private void splitTest$endViewportClear(boolean clearError, CallbackInfo ci) {
        ViewportPass.endClear();
    }

    @Inject(method = "copyDepthFrom", at = @At("HEAD"), cancellable = true)
    private void splitTest$copyDepthFromActiveViewport(RenderTarget otherTarget, CallbackInfo ci) {
        if (ViewportPass.copyDepthFrom((RenderTarget)(Object)this, otherTarget)) {
            ci.cancel();
        }
    }
}
