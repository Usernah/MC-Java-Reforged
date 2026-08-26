package net.jr.client.sound.action;

import net.jr.client.input.InputApi;
import net.jr.client.runtime.input.LocalInputActionHandler;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;

public final class WidgetSoundRouter {
    private WidgetSoundRouter() {
    }

    public static void playDownSound(AbstractWidget widget) {
        if (widget instanceof AbstractSliderButton) {
            return;
        }
        if (!InputApi.isGamepadMode()) {
            LocalInputActionHandler.suppressNextScreenTransitionSound();
            InputActionSounds.playClick();
        }
    }
}
