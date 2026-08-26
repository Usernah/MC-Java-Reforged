package net.jr.client.sound.action;

import net.jr.client.input.InputApi;
import net.jr.client.input.runtime.MappedActionProcessor;
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
            MappedActionProcessor.suppressNextScreenTransitionSound();
            InputActionSounds.playClick();
        }
    }
}
