package net.jr.mixin.controlhints;

import net.jr.client.input.runtime.GamepadInputProcessor;
import net.jr.client.runtime.context.SlotScope;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Screen.class)
public abstract class ScreenMouseCoordinateMixin {
    @ModifyVariable(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int javareforged$useVirtualScreenMouseX(int mouseX) {
        return SlotScope.idOrNull() != null ? mouseX : GamepadInputProcessor.resolveScreenMouseX(mouseX);
    }

    @ModifyVariable(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private int javareforged$useVirtualScreenMouseY(int mouseY) {
        return SlotScope.idOrNull() != null ? mouseY : GamepadInputProcessor.resolveScreenMouseY(mouseY);
    }
}

