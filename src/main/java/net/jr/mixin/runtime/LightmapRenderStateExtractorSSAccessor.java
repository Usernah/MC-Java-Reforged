package net.jr.mixin.runtime;

import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LightmapRenderStateExtractor.class)
public interface LightmapRenderStateExtractorSSAccessor {
    @Accessor("needsUpdate")
    void splitTest$setNeedsUpdate(boolean needsUpdate);
}
