package net.jr.mixin.controlhints;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.jr.client.ui.hint.ControlHintPipeline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiControlHintsMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void javareforged$renderControlHints(
        DeltaTracker deltaTracker,
        boolean shouldRenderLevel,
        boolean resourcesLoaded,
        CallbackInfo ci
    ) {

    }
}
