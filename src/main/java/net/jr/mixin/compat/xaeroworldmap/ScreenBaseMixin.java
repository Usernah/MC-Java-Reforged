package net.jr.mixin.compat.xaeroworldmap;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.gui.ConfirmScreenBase;
import xaero.map.gui.GuiMap;

@Mixin(value = ConfirmScreenBase.class, remap = false)
public abstract class ScreenBaseMixin {

    /*
     * GuiMap already renders its own fullscreen map/background.
     *
     * Letting vanilla Screen.extractBackground() run as well creates
     * a global screen background in Java Reforged's shared GUI frame.
     *
     * Suppress ONLY the vanilla background for GuiMap.
     * Widgets and ScreenBase.extractRenderState() still run normally.
     */
    @Inject(
            method = "extractBackground",
            at = @At("HEAD"),
            cancellable = true
    )
    private void javaReforged$skipGlobalGuiMapBackground(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick,
            CallbackInfo ci
    ) {
        if ((Object)this instanceof GuiMap) {
            ci.cancel();
        }
    }
}