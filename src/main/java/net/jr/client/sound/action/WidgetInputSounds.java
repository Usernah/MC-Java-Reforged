package net.jr.client.sound.action;

import java.util.Optional;
import net.jr.client.components.navigation.UiActionFeedback;
import net.jr.client.input.InputApi;

public final class WidgetInputSounds {
    private static final ThreadLocal<UiActionFeedback> POINTER_CLICK_FEEDBACK = new ThreadLocal<>();

    private WidgetInputSounds() {
    }

    public static void clearPointerClickFeedback() {
        POINTER_CLICK_FEEDBACK.remove();
    }

    public static void reportPointerClickFeedback(UiActionFeedback feedback) {
        UiActionFeedback resolved = feedback == null ? UiActionFeedback.NONE : feedback;
        if (InputApi.isGamepadMode()) {
            POINTER_CLICK_FEEDBACK.set(resolved);
            return;
        }
        InputActionSounds.play(resolved);
    }

    public static Optional<UiActionFeedback> consumePointerClickFeedback() {
        UiActionFeedback feedback = POINTER_CLICK_FEEDBACK.get();
        POINTER_CLICK_FEEDBACK.remove();
        return Optional.ofNullable(feedback);
    }

    public static void playPointerDragFeedback(UiActionFeedback feedback) {
        InputActionSounds.play(feedback);
    }
}
