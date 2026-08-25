package net.jr.mixin.runtime;

import com.mojang.blaze3d.systems.RenderPass;
import net.jr.client.runtime.viewport.GuiScissorBounds;
import net.jr.client.runtime.viewport.PreciseGuiScissor;
import net.jr.client.runtime.viewport.PreciseScissorAccess;
import net.jr.client.runtime.viewport.ViewportPanoramaRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.CubeMap;
import net.minecraft.client.renderer.state.WindowRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderer.class)
public abstract class GuiRendererSSMixin {
    @Shadow @Final private CubeMap cubeMap;

    @Inject(method = "render", at = @At("HEAD"))
    private void splitTest$renderViewportPanoramas(CallbackInfo callback) {
        ViewportPanoramaRenderer.renderPending(this.cubeMap);
    }

    @Inject(
            method = "enableScissor",
            at = @At("HEAD"),
            cancellable = true
    )
    private void splitTest$usePreciseViewportScissor(
            ScreenRectangle rectangle,
            RenderPass renderPass,
            CallbackInfo callback
    ) {
        PreciseScissorAccess access =
                (PreciseScissorAccess)(Object)rectangle;

        PreciseGuiScissor precise =
                access.javaReforged$preciseScissor();

        if (precise == null) {
            return;
        }

        WindowRenderState window =
                Minecraft.getInstance()
                        .gameRenderer
                        .gameRenderState()
                        .windowRenderState;

        int left = (int)Math.floor(precise.left());
        int top = (int)Math.floor(precise.top());
        int right = (int)Math.ceil(precise.right());
        int bottom = (int)Math.ceil(precise.bottom());

        left = Math.max(0, Math.min(left, window.width));
        top = Math.max(0, Math.min(top, window.height));
        right = Math.max(0, Math.min(right, window.width));
        bottom = Math.max(0, Math.min(bottom, window.height));

        int width = right - left;
        int height = bottom - top;

        if (width <= 0 || height <= 0) {
            return;
        }

        renderPass.enableScissor(
                left,
                window.height - bottom,
                width,
                height
        );

        callback.cancel();
    }
}
