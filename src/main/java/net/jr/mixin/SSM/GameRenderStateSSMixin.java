package net.jr.mixin.SSM;

import net.jr.ClientRuntime.bridge.GameRenderStateSSAccessor;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.renderer.state.LightmapRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(GameRenderState.class)
public abstract class GameRenderStateSSMixin implements GameRenderStateSSAccessor {
    @Shadow @Final @Mutable
    public LevelRenderState levelRenderState;

    @Shadow @Final @Mutable
    public LightmapRenderState lightmapRenderState;

    @Override
    public LevelRenderState splitTest$getLevelRenderState() {
        return this.levelRenderState;
    }

    @Override
    public void splitTest$setLevelRenderState(LevelRenderState state) {
        this.levelRenderState = state;
    }

    @Override
    public LightmapRenderState splitTest$getLightmapRenderState() {
        return this.lightmapRenderState;
    }

    @Override
    public void splitTest$setLightmapRenderState(LightmapRenderState state) {
        this.lightmapRenderState = state;
    }
}
