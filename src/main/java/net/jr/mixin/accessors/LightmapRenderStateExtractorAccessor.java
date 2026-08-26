package net.jr.mixin.accessors;

import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LightmapRenderStateExtractor.class)
public interface LightmapRenderStateExtractorAccessor {
    @Accessor("needsUpdate")
    void javaReforged$setNeedsUpdate(boolean needsUpdate);
}
