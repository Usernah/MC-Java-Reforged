package net.jr.mixin.runtime;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.jr.client.runtime.viewport.ViewportPanoramaRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.Panorama;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Panorama.class)
public abstract class PanoramaMixin {
    @WrapMethod(method = "extractRenderState")
    private void javaReforged$extractViewportPanorama(
        GuiGraphicsExtractor graphics,
        int width,
        int height,
        Operation<Void> original
    ) {
        ViewportPanoramaRenderer.captureForActiveViewport(() -> original.call(graphics, width, height));
    }
}
