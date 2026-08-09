package net.alnv.javareforged.mixin.SSM;

import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.PostChain;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelRenderer.class)
public interface LevelRendererSSAccessor {
    @Accessor("level")
    @Nullable
    ClientLevel splitTest$getLevel();

    @Accessor("level")
    void splitTest$setLevel(@Nullable ClientLevel level);

    @Accessor("sectionRenderDispatcher")
    @Nullable
    SectionRenderDispatcher splitTest$getSectionRenderDispatcher();

    @Accessor("transparencyChain")
    @Nullable
    PostChain splitTest$getTransparencyChain();

    @Accessor("transparencyChain")
    void splitTest$setTransparencyChain(@Nullable PostChain transparencyChain);

    @Accessor("translucentTarget")
    @Nullable
    RenderTarget splitTest$getTranslucentTarget();

    @Accessor("translucentTarget")
    void splitTest$setTranslucentTarget(@Nullable RenderTarget translucentTarget);

    @Accessor("itemEntityTarget")
    @Nullable
    RenderTarget splitTest$getItemEntityTarget();

    @Accessor("itemEntityTarget")
    void splitTest$setItemEntityTarget(@Nullable RenderTarget itemEntityTarget);

    @Accessor("particlesTarget")
    @Nullable
    RenderTarget splitTest$getParticlesTarget();

    @Accessor("particlesTarget")
    void splitTest$setParticlesTarget(@Nullable RenderTarget particlesTarget);

    @Accessor("weatherTarget")
    @Nullable
    RenderTarget splitTest$getWeatherTarget();

    @Accessor("weatherTarget")
    void splitTest$setWeatherTarget(@Nullable RenderTarget weatherTarget);

    @Accessor("cloudsTarget")
    @Nullable
    RenderTarget splitTest$getCloudsTarget();

    @Accessor("cloudsTarget")
    void splitTest$setCloudsTarget(@Nullable RenderTarget cloudsTarget);
}
