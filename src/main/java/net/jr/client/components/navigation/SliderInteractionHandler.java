package net.jr.client.components.navigation;

import net.jr.client.sound.action.WidgetInputSounds;
import net.jr.mixin.accessors.AbstractSliderButtonAccessor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;

public final class SliderInteractionHandler {
    private SliderInteractionHandler() {
    }

    public static void capturePointerValue(AbstractSliderButton slider, SliderInteractionState state) {
        state.setValueBeforePointerUpdate(accessor(slider).javaReforged$getValue());
    }

    public static void reportPointerClick(AbstractSliderButton slider, SliderInteractionState state) {
        WidgetInputSounds.reportPointerClickFeedback(
                valueChanged(slider, state) ? UiActionFeedback.CHAR : UiActionFeedback.NONE
        );
    }

    public static void reportPointerDrag(AbstractSliderButton slider, SliderInteractionState state) {
        if (valueChanged(slider, state)) {
            WidgetInputSounds.playPointerDragFeedback(UiActionFeedback.CHAR);
        }
    }

    public static boolean handleUiAction(
            AbstractSliderButton slider,
            SliderInteractionState state,
            UiAction action
    ) {
        state.setLastFeedback(UiActionFeedback.NONE);
        if (!slider.isActive()) {
            return false;
        }

        AbstractSliderButtonAccessor accessor = accessor(slider);
        if (action == UiAction.ACCEPT) {
            accessor.javaReforged$setCanChangeValue(!accessor.javaReforged$getCanChangeValue());
            state.setLastFeedback(UiActionFeedback.CLICK);
            return true;
        }
        if (!accessor.javaReforged$getCanChangeValue()) {
            return false;
        }
        if (action == UiAction.CANCEL || action == UiAction.BACK) {
            accessor.javaReforged$setCanChangeValue(false);
            state.setLastFeedback(UiActionFeedback.CLICK);
            return true;
        }
        if (action == UiAction.MOVE_LEFT) {
            adjustValue(slider, state, GLFW.GLFW_KEY_LEFT);
            return true;
        }
        if (action == UiAction.MOVE_RIGHT) {
            adjustValue(slider, state, GLFW.GLFW_KEY_RIGHT);
            return true;
        }
        if (action == UiAction.MOVE_UP || action == UiAction.MOVE_DOWN) {
            accessor.javaReforged$setCanChangeValue(false);
        }
        return false;
    }

    private static void adjustValue(AbstractSliderButton slider, SliderInteractionState state, int keyCode) {
        double previous = accessor(slider).javaReforged$getValue();
        slider.keyPressed(new KeyEvent(keyCode, 0, 0));
        state.setLastFeedback(Double.compare(previous, accessor(slider).javaReforged$getValue()) != 0
                ? UiActionFeedback.CHAR
                : UiActionFeedback.NONE);
    }

    private static boolean valueChanged(AbstractSliderButton slider, SliderInteractionState state) {
        return Double.compare(state.valueBeforePointerUpdate(), accessor(slider).javaReforged$getValue()) != 0;
    }

    private static AbstractSliderButtonAccessor accessor(AbstractSliderButton slider) {
        return (AbstractSliderButtonAccessor) slider;
    }
}
