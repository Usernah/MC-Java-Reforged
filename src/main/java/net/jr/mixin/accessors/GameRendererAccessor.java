package net.jr.mixin.accessors;

import com.mojang.blaze3d.resource.CrossFrameResourcePool;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.GlobalSettingsUniform;
import net.minecraft.client.renderer.Lightmap;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GameRenderer.class)
public interface GameRendererAccessor {
    @Accessor("lightmap")
    Lightmap javaReforged$getLightmap();

    @Accessor("lightmapRenderStateExtractor")
    LightmapRenderStateExtractor javaReforged$getLightmapExtractor();

    @Accessor("globalSettingsUniform")
    GlobalSettingsUniform javaReforged$getGlobalSettingsUniform();

    @Accessor("effectActive")
    boolean javaReforged$isPostEffectActive();

    @Accessor("resourcePool")
    CrossFrameResourcePool javaReforged$getResourcePool();

    @Invoker("extractCamera")
    void javaReforged$extractCamera(DeltaTracker deltaTracker, float worldPartialTicks, float cameraEntityPartialTicks);
}
