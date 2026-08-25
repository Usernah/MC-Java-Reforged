package net.jr.mixin;

import net.jr.client.render.ItemRenderResolution;
import net.minecraft.client.gui.render.GuiItemAtlas;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderer.class)
public abstract class GuiItemResolutionRendererMixin {
    @Shadow @Final private GuiRenderState renderState;

    @Unique
    private int javaReforged$largestAtlasItemSize = 16;

    @Invoker("invalidateItemAtlas")
    abstract void javaReforged$invalidateItemAtlas();

    @Inject(method = "prepareItemElements", at = @At("HEAD"))
    private void javaReforged$prepareItemResolution(CallbackInfo ci) {
        ItemRenderResolution.prepareFrame(this.renderState);
        int requestedSize = ItemRenderResolution.preparedItemSize();
        if (requestedSize > this.javaReforged$largestAtlasItemSize) {
            this.javaReforged$invalidateItemAtlas();
            this.javaReforged$largestAtlasItemSize = requestedSize;
        }
    }

    @ModifyArg(
            method = "prepareItemElements",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/render/GuiRenderer;prepareItemAtlas(Ljava/util/Set;I)Lnet/minecraft/client/gui/render/GuiItemAtlas;"
            ),
            index = 1
    )
    private int javaReforged$resizeItemAtlas(int vanillaSlotSize) {
        return ItemRenderResolution.atlasSlotSize(vanillaSlotSize);
    }

    @Inject(method = "endFrame", at = @At("TAIL"))
    private void javaReforged$clearItemResolution(CallbackInfo ci) {
        ItemRenderResolution.clearFrame();
    }
}
