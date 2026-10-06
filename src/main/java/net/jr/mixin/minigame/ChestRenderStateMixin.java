package net.jr.mixin.minigame;

import net.jr.experimental_features.minigame.ChestOutlineRenderstate;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ChestRenderState.class)
public class ChestRenderStateMixin implements ChestOutlineRenderstate {
    @Unique
    private int javaReforged$outlineColor;

    @Override
    public int javaReforged$outlineColor() {
        return this.javaReforged$outlineColor;
    }

    @Override
    public void javaReforged$setOutlineColor(int color) {
        this.javaReforged$outlineColor = color;
    }
}
