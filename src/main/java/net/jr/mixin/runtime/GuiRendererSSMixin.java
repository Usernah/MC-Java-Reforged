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
public abstract class GuiRendererSSMixin {
    @Shadow @Final private CubeMap cubeMap;

    @Inject(method = "render", at = @At("HEAD"))
    private void splitTest$renderViewportPanoramas(CallbackInfo callback) {
        ViewportPanoramaRenderer.renderPending(this.cubeMap);
    }

    @Inject(method = "enableScissor", at = @At("HEAD"), cancellable = true)
    private void splitTest$useExactViewportEdges(ScreenRectangle rectangle, RenderPass renderPass, CallbackInfo callback) {
        WindowRenderState window = Minecraft.getInstance().gameRenderer.gameRenderState().windowRenderState;
        GuiScissorBounds.PixelRectangle pixels = GuiScissorBounds.resolve(rectangle, window);
        renderPass.enableScissor(
            pixels.left(),
            window.height - pixels.bottom(),
            Math.max(0, pixels.right() - pixels.left()),
            Math.max(0, pixels.bottom() - pixels.top())
        );
        callback.cancel();
    }
}
