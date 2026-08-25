package net.jr.mixin;

import net.jr.client.render.ItemRenderOpacity;
import net.jr.client.render.ItemRenderResolution;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiItemOpacityExtractorMixin {
    @ModifyArg(
            method = "item(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;III)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/state/gui/GuiRenderState;addItem(Lnet/minecraft/client/renderer/state/gui/GuiItemRenderState;)V"
            ),
            index = 0
    )
    private GuiItemRenderState javaReforged$captureItemOpacity(GuiItemRenderState state) {
        return ItemRenderResolution.capture(ItemRenderOpacity.capture(state));
    }
}
