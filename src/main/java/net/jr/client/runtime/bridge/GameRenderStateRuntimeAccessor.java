package net.jr.client.runtime.bridge;

import net.minecraft.client.renderer.state.LightmapRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;

public interface GameRenderStateRuntimeAccessor {
    LevelRenderState splitTest$getLevelRenderState();

    void splitTest$setLevelRenderState(LevelRenderState state);

    LightmapRenderState splitTest$getLightmapRenderState();

    void splitTest$setLightmapRenderState(LightmapRenderState state);
}
