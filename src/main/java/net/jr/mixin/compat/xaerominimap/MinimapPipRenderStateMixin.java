package net.jr.mixin.compat.xaerominimap;

import net.jr.api.client.local.Client;
import net.jr.client.compat.xaero.XaeroMinimapPipState;
import net.jr.client.runtime.viewport.GuiViewportScope;
import net.jr.client.runtime.viewport.ViewportArea;
import net.jr.client.runtime.viewport.ViewportRenderScope;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.hud.minimap.render.MinimapPipRenderState;

@Mixin(value = MinimapPipRenderState.class, remap = false)
abstract class MinimapPipRenderStateMixin implements XaeroMinimapPipState {
    @Shadow
    private ScreenRectangle rectangle;

    @Unique
    private int javaReforged$slotId = -1;
    @Unique
    private boolean javaReforged$mapped;
    @Unique
    private int javaReforged$x0;
    @Unique
    private int javaReforged$y0;
    @Unique
    private int javaReforged$x1;
    @Unique
    private int javaReforged$y1;
    @Unique
    private int javaReforged$renderWidth;
    @Unique
    private int javaReforged$renderHeight;
    @Unique
    private float javaReforged$scale = 1.0F;

    @Inject(
        method = "update(IIIIDFIIFLxaero/lib/client/graphics/XaeroBufferProvider;)Lxaero/hud/minimap/render/MinimapPipRenderState;",
        at = @At("HEAD")
    )
    private void javaReforged$beginUpdate(CallbackInfoReturnable<MinimapPipRenderState> callback) {
        this.javaReforged$mapped = false;
    }

    @Inject(
        method = "update(IIIIDFIIFLxaero/lib/client/graphics/XaeroBufferProvider;)Lxaero/hud/minimap/render/MinimapPipRenderState;",
        at = @At("RETURN")
    )
    private void javaReforged$captureViewport(CallbackInfoReturnable<MinimapPipRenderState> callback) {
        MinimapPipRenderState state = (MinimapPipRenderState)(Object)this;
        ViewportArea viewport = ViewportRenderScope.activeViewportOrNull();
        if (viewport == null || Client.currentOrNull() == null) {
            this.javaReforged$slotId = -1;
            return;
        }

        int x0 = state.x0();
        int y0 = state.y0();
        int x1 = state.x1();
        int y1 = state.y1();
        GuiViewportScope.PictureInPictureBounds bounds = GuiViewportScope.mapPictureInPicture(
            x0,
            y0,
            x1,
            y1,
            1.0F
        );
        this.javaReforged$slotId = Client.slotId();
        this.javaReforged$renderWidth = x1 - x0;
        this.javaReforged$renderHeight = y1 - y0;
        this.javaReforged$x0 = bounds.x0();
        this.javaReforged$y0 = bounds.y0();
        this.javaReforged$x1 = bounds.x1();
        this.javaReforged$y1 = bounds.y1();
        this.javaReforged$scale = bounds.scale();
        this.rectangle = new ScreenRectangle(
            this.javaReforged$x0,
            this.javaReforged$y0,
            this.javaReforged$x1 - this.javaReforged$x0,
            this.javaReforged$y1 - this.javaReforged$y0
        );
        this.javaReforged$mapped = true;
    }

    @Inject(method = "x0", at = @At("HEAD"), cancellable = true)
    private void javaReforged$mappedX0(CallbackInfoReturnable<Integer> callback) {
        if (this.javaReforged$mapped) {
            callback.setReturnValue(this.javaReforged$x0);
        }
    }

    @Inject(method = "y0", at = @At("HEAD"), cancellable = true)
    private void javaReforged$mappedY0(CallbackInfoReturnable<Integer> callback) {
        if (this.javaReforged$mapped) {
            callback.setReturnValue(this.javaReforged$y0);
        }
    }

    @Inject(method = "x1", at = @At("HEAD"), cancellable = true)
    private void javaReforged$mappedX1(CallbackInfoReturnable<Integer> callback) {
        if (this.javaReforged$mapped) {
            callback.setReturnValue(this.javaReforged$x1);
        }
    }

    @Inject(method = "y1", at = @At("HEAD"), cancellable = true)
    private void javaReforged$mappedY1(CallbackInfoReturnable<Integer> callback) {
        if (this.javaReforged$mapped) {
            callback.setReturnValue(this.javaReforged$y1);
        }
    }

    @Inject(method = "scale", at = @At("HEAD"), cancellable = true)
    private void javaReforged$mappedScale(CallbackInfoReturnable<Float> callback) {
        if (this.javaReforged$mapped) {
            callback.setReturnValue(this.javaReforged$scale);
        }
    }

    @Override
    public int javaReforged$slotId() {
        return this.javaReforged$slotId;
    }

    @Override
    public int javaReforged$renderWidth() {
        return this.javaReforged$renderWidth;
    }

    @Override
    public int javaReforged$renderHeight() {
        return this.javaReforged$renderHeight;
    }
}
