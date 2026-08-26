package net.jr.mixin.runtime;

import com.mojang.blaze3d.systems.RenderPass;
import net.jr.client.runtime.viewport.GuiScissorBounds;
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
public abstract class GuiRendererMixin {
    @Shadow @Final private CubeMap cubeMap;

    @Inject(method = "render", at = @At("HEAD"))
    private void javaReforged$renderViewportPanoramas(CallbackInfo callback) {
        ViewportPanoramaRenderer.renderPending(this.cubeMap);
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

        GuiScissorBounds.PixelRectangle bounds = GuiScissorBounds.resolvePrecise(rectangle, window);
        if (bounds == null) {
            return;
        }

        renderPass.enableScissor(
                bounds.left(),
                window.height - bounds.bottom(),
                bounds.right() - bounds.left(),
                bounds.bottom() - bounds.top()
        );

        callback.cancel();
    }
}
