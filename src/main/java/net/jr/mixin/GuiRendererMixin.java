package net.jr.mixin;

import net.jr.client.render.item.GuiItemFrameState;
import net.jr.client.render.item.GuiItemRenderPass;
import net.jr.client.ui.layout.render.UILayoutRenderQueue;
import net.minecraft.client.gui.render.GuiItemAtlas;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderer.class)
public abstract class GuiRendererMixin {
    @Unique
    private final GuiItemFrameState javaReforged$itemFrameState = new GuiItemFrameState();

    @Inject(method = "render", at = @At("HEAD"))
    private void javaReforged$renderLiveLayoutTargets(CallbackInfo ci) {
        UILayoutRenderQueue.renderQueued();
    }

    @Inject(method = "prepareItemElements", at = @At("HEAD"))
    private void javaReforged$prepareItemResolution(CallbackInfo ci) {
        GuiItemRenderPass.prepareFrame((GuiRenderer) (Object) this, this.javaReforged$itemFrameState);
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
        return GuiItemRenderPass.atlasSlotSize(vanillaSlotSize);
    }

    @Inject(method = "submitBlitFromItemAtlas", at = @At("HEAD"))
    private void javaReforged$readItemOpacity(
            GuiItemRenderState state,
            GuiItemAtlas.SlotView slot,
            CallbackInfo ci
    ) {
        GuiItemRenderPass.readOpacity(state, this.javaReforged$itemFrameState);
    }

    @ModifyConstant(method = "submitBlitFromItemAtlas", constant = @Constant(intValue = -1))
    private int javaReforged$applyItemOpacity(int original) {
        return this.javaReforged$itemFrameState.itemColor();
    }

    @Inject(method = "endFrame", at = @At("TAIL"))
    private void javaReforged$clearItemResolution(CallbackInfo ci) {
        GuiItemRenderPass.clearFrame();
    }
}
