package net.jr.client.components.navigation;

import net.jr.mixin.accessors.AbstractSliderButtonAccessor;
import net.jr.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import org.lwjgl.glfw.GLFW;

public final class SliderInteractionHandler {
    private SliderInteractionHandler() {
    }

    public static void capturePointerValue(AbstractSliderButton slider, SliderInteractionState state) {
        state.setValueBeforePointerUpdate(accessor(slider).javaReforged$getValue());
    }

    public static void reportPointerClick(AbstractSliderButton slider, SliderInteractionState state) {
        if (valueChanged(slider, state)) {
            playCharacterSound(Minecraft.getInstance().getSoundManager());
        }
    }

    public static void reportPointerDrag(AbstractSliderButton slider, SliderInteractionState state) {
        if (valueChanged(slider, state)) {
            playCharacterSound(Minecraft.getInstance().getSoundManager());
        }
    }

    public static boolean handleUiAction(
            AbstractSliderButton slider,
            SliderInteractionState state,
            UiAction action
    ) {
        if (!slider.isActive()) {
            return false;
        }

        AbstractSliderButtonAccessor accessor = accessor(slider);
        if (action == UiAction.ACCEPT) {
            accessor.javaReforged$setCanChangeValue(!accessor.javaReforged$getCanChangeValue());
            playClickSound(Minecraft.getInstance().getSoundManager());
            return true;
        }
        if (!accessor.javaReforged$getCanChangeValue()) {
            return false;
        }
        if (action == UiAction.CANCEL || action == UiAction.BACK) {
            accessor.javaReforged$setCanChangeValue(false);
            playClickSound(Minecraft.getInstance().getSoundManager());
            return true;
        }
        if (action == UiAction.MOVE_LEFT) {
            if (adjustValue(slider, GLFW.GLFW_KEY_LEFT)) {
                playCharacterSound(Minecraft.getInstance().getSoundManager());
            }
            return true;
        }
        if (action == UiAction.MOVE_RIGHT) {
            if (adjustValue(slider, GLFW.GLFW_KEY_RIGHT)) {
                playCharacterSound(Minecraft.getInstance().getSoundManager());
            }
            return true;
        }
        if (action == UiAction.MOVE_UP || action == UiAction.MOVE_DOWN) {
            accessor.javaReforged$setCanChangeValue(false);
        }
        return false;
    }

    private static void playClickSound(SoundManager soundManager) {
        soundManager.play(SimpleSoundInstance.forUI(ModSounds.UI_CLICK.get(), 1.0F, 1.0F));
    }

    private static void playCharacterSound(SoundManager soundManager) {
        soundManager.play(SimpleSoundInstance.forUI(ModSounds.UI_CHAR.get(), 1.0F, 1.0F));
    }

    private static boolean adjustValue(AbstractSliderButton slider, int keyCode) {
        double previous = accessor(slider).javaReforged$getValue();
        slider.keyPressed(new KeyEvent(keyCode, 0, 0));
        return Double.compare(previous, accessor(slider).javaReforged$getValue()) != 0;
    }

    private static boolean valueChanged(AbstractSliderButton slider, SliderInteractionState state) {
        return Double.compare(state.valueBeforePointerUpdate(), accessor(slider).javaReforged$getValue()) != 0;
    }

    private static AbstractSliderButtonAccessor accessor(AbstractSliderButton slider) {
        return (AbstractSliderButtonAccessor) slider;
    }
}
