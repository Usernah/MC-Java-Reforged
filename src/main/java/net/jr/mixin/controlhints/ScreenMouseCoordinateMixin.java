package net.jr.mixin.controlhints;

import net.jr.client.input.runtime.GamepadInputProcessor;
import net.jr.client.runtime.ui.LocalScreenManager;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Screen.class)
public abstract class ScreenMouseCoordinateMixin {
    @ModifyVariable(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int javareforged$useVirtualScreenMouseX(int mouseX) {
        if (LocalScreenManager.slotUiPassOwnsScreens()) {
            return mouseX;
        }
        return GamepadInputProcessor.resolveScreenMouseX(mouseX);
    }

    @ModifyVariable(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private int javareforged$useVirtualScreenMouseY(int mouseY) {
        if (LocalScreenManager.slotUiPassOwnsScreens()) {
            return mouseY;
        }
        return GamepadInputProcessor.resolveScreenMouseY(mouseY);
    }
}

