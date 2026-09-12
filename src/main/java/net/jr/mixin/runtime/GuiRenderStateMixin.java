package net.jr.mixin.runtime;

import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.bridge.ViewportBlurAccess;
import net.jr.client.runtime.render.state.ViewportBlurState;
import net.jr.client.runtime.viewport.ViewportArea;
import net.jr.client.runtime.viewport.ViewportRenderScope;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderState.class)
public abstract class GuiRenderStateMixin implements ViewportBlurAccess {
    @Shadow private int firstStratumAfterBlur;
    @Unique private final ViewportBlurState javaReforged$blur = new ViewportBlurState();

    @Override
    public ViewportBlurState javaReforged$viewportBlurState() {
        return this.javaReforged$blur;
    }

    @Inject(method = "blurBeforeThisStratum", at = @At("HEAD"), cancellable = true)
    private void javaReforged$collectViewportBlur(CallbackInfo ci) {
        if (ClientRuntime.INSTANCE.viewports().presentedCount() <= 1) {
            return;
        }
        ViewportArea viewport = ViewportRenderScope.activeViewportOrNull();
        if (viewport == null) {
            this.javaReforged$blur.requestGlobal();
        } else {
            this.javaReforged$blur.request(new ViewportBlurState.Region(
                viewport.id(), viewport.glX(), viewport.glY(), viewport.width(), viewport.height()
            ));
        }
        // Keep the first GUI stratum boundary; later players add regions only.
        // All HUDs are extracted before screens; requesting menus stay sharp.
        if (this.firstStratumAfterBlur != Integer.MAX_VALUE) {
            ci.cancel();
        }
    }

    @Inject(method = "reset", at = @At("HEAD"))
    private void javaReforged$resetViewportBlur(CallbackInfo ci) {
        this.javaReforged$blur.reset();
    }
}
