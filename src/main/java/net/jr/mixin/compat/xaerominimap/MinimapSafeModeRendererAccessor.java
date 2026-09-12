package net.jr.mixin.compat.xaerominimap;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import xaero.common.graphics.MinimapTexture;
import xaero.common.minimap.render.MinimapSafeModeRenderer;

@Mixin(value = MinimapSafeModeRenderer.class, remap = false)
public interface MinimapSafeModeRendererAccessor {
    @Accessor("data")
    int[] javaReforged$getData();

    @Accessor("data")
    void javaReforged$setData(int[] data);

    @Accessor("drawYState")
    byte javaReforged$getDrawYState();

    @Accessor("drawYState")
    void javaReforged$setDrawYState(byte drawYState);

    @Accessor("mapTexture")
    MinimapTexture javaReforged$getMapTexture();

    @Accessor("mapTexture")
    void javaReforged$setMapTexture(MinimapTexture mapTexture);
}
