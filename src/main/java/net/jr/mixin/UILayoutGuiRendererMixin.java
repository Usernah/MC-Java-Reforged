package net.jr.mixin;

import net.jr.client.ui.layout.render.UILayoutRenderQueue;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderer.class)
public abstract class UILayoutGuiRendererMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void javareforged$renderLiveLayoutTargets(CallbackInfo callbackInfo) {
        UILayoutRenderQueue.renderQueued();
    }
}
