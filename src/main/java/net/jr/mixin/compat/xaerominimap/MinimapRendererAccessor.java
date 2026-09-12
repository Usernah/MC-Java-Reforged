package net.jr.mixin.compat.xaerominimap;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import xaero.common.minimap.render.MinimapRenderer;

@Mixin(value = MinimapRenderer.class, remap = false)
public interface MinimapRendererAccessor {
    @Accessor("lastMinimapSize")
    int javaReforged$getLastMinimapSize();

    @Accessor("lastMinimapSize")
    void javaReforged$setLastMinimapSize(int size);

    @Accessor("zoom")
    double javaReforged$getZoom();

    @Accessor("zoom")
    void javaReforged$setZoom(double zoom);
}
