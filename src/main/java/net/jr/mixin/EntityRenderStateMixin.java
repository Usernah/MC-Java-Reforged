package net.jr.mixin;

import net.jr.client.render.nametag.LegacyNameTagRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
public class EntityRenderStateMixin implements LegacyNameTagRenderState {
    @Unique
    private int javaReforged$playerColor = -1;
    @Unique
    private boolean javaReforged$player;

    @Override
    public void javaReforged$setPlayer(boolean player) {
        this.javaReforged$player = player;
    }

    @Override
    public boolean javaReforged$isPlayer() {
        return this.javaReforged$player;
    }

    @Override
    public void javaReforged$setPlayerColor(int color) {
        this.javaReforged$playerColor = color;
    }

    @Override
    public int javaReforged$getPlayerColor() {
        return this.javaReforged$playerColor;
    }
}
