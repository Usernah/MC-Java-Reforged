package net.jr.mixin;

import net.jr.client.render.ItemRenderOpacity;
import net.minecraft.client.gui.render.GuiItemAtlas;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderer.class)
public abstract class GuiItemOpacityRendererMixin {
    @Unique
    private int javaReforged$itemColor = -1;

    @Inject(method = "submitBlitFromItemAtlas", at = @At("HEAD"))
    private void javaReforged$readItemOpacity(
            GuiItemRenderState state,
            GuiItemAtlas.SlotView slot,
            CallbackInfo callback
    ) {
        this.javaReforged$itemColor = ItemRenderOpacity.consumeColor(state);
    }

    @ModifyConstant(method = "submitBlitFromItemAtlas", constant = @Constant(intValue = -1))
    private int javaReforged$applyItemOpacity(int original) {
        return this.javaReforged$itemColor;
    }
}
