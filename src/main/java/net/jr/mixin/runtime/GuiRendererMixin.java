package net.jr.mixin.runtime;

import com.mojang.blaze3d.systems.RenderPass;
import net.jr.client.runtime.bridge.ViewportBlurAccess;
import net.jr.client.runtime.render.pass.ViewportBlurRenderer;
import net.jr.client.runtime.viewport.GuiScissorBounds;
import net.jr.client.runtime.viewport.ViewportPanoramaRenderer;
import net.jr.client.ui.render.composition.GuiCompositionScope;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.CubeMap;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.WindowRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderer.class)
public abstract class GuiRendererMixin {
    @Shadow @Final private CubeMap cubeMap;
    @Shadow @Final private GuiRenderState renderState;
    @Unique private final ViewportBlurRenderer javaReforged$blurRenderer = new ViewportBlurRenderer();

    @Inject(method = "render", at = @At("HEAD"))
    private void javaReforged$renderViewportPanoramas(CallbackInfo callback) {
        this.javaReforged$blurRenderer.prepareFrame(((ViewportBlurAccess)this.renderState).javaReforged$viewportBlurState());
        if (GuiCompositionScope.activeRenderingOrNull() == null) {
            ViewportPanoramaRenderer.renderPending(this.cubeMap);
        }
    }

    @Redirect(method = "draw", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;processBlurEffect()V"))
    private void javaReforged$blurRequestedViewports(GameRenderer renderer) {
        this.javaReforged$blurRenderer.render(renderer, ((ViewportBlurAccess)this.renderState).javaReforged$viewportBlurState());
    }

    @Inject(method = "close", at = @At("TAIL"))
    private void javaReforged$closeBlurTargets(CallbackInfo ci) {
        this.javaReforged$blurRenderer.close();
    }

    @Inject(
            method = "enableScissor",
            at = @At("HEAD"),
            cancellable = true
    )
    private void javaReforged$usePreciseViewportScissor(
            ScreenRectangle rectangle,
            RenderPass renderPass,
            CallbackInfo callback
    ) {
        WindowRenderState window =
                Minecraft.getInstance()
                        .gameRenderer
                        .gameRenderState()
                        .windowRenderState;

        GuiCompositionScope.Rendering composition =
                GuiCompositionScope.activeRenderingOrNull();
        int targetWidth = composition == null
                ? window.width
                : composition.framebufferWidth();
        int targetHeight = composition == null
                ? window.height
                : composition.framebufferHeight();

        GuiScissorBounds.PixelRectangle bounds =
                GuiScissorBounds.resolvePrecise(rectangle, targetWidth, targetHeight);
        if (bounds == null) {
            return;
        }

        renderPass.enableScissor(
                bounds.left(),
                targetHeight - bounds.bottom(),
                bounds.right() - bounds.left(),
                bounds.bottom() - bounds.top()
        );

        callback.cancel();
    }
}
