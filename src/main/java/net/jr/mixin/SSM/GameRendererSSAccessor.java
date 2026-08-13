package net.jr.mixin.SSM;

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
public interface GameRendererSSAccessor {
    @Accessor("lightmap")
    Lightmap splitTest$getLightmap();

    @Accessor("lightmapRenderStateExtractor")
    LightmapRenderStateExtractor splitTest$getLightmapExtractor();

    @Accessor("globalSettingsUniform")
    GlobalSettingsUniform splitTest$getGlobalSettingsUniform();

    @Accessor("effectActive")
    boolean splitTest$isPostEffectActive();

    @Accessor("resourcePool")
    CrossFrameResourcePool splitTest$getResourcePool();

    @Invoker("extractCamera")
    void splitTest$extractCamera(DeltaTracker deltaTracker, float worldPartialTicks, float cameraEntityPartialTicks);
}
