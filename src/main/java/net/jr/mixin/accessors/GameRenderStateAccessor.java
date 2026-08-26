package net.jr.mixin.accessors;

import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.renderer.state.LightmapRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GameRenderState.class)
public interface GameRenderStateAccessor {
    @Accessor("levelRenderState")
    LevelRenderState javaReforged$getLevelRenderState();

    @Mutable
    @Accessor("levelRenderState")
    void javaReforged$setLevelRenderState(LevelRenderState state);

    @Accessor("lightmapRenderState")
    LightmapRenderState javaReforged$getLightmapRenderState();

    @Mutable
    @Accessor("lightmapRenderState")
    void javaReforged$setLightmapRenderState(LightmapRenderState state);
}
