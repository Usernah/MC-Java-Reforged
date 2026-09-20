package net.jr.experimental_features.gamepad_keyboard;

import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.util.StringUtil;

/** Common editing surface implemented by vanilla and Java Reforged text boxes. */
public interface GamepadTextInput extends GuiEventListener {
    boolean gamepadKeyboardCanConsumeInput();

    String gamepadKeyboardValue();

    int gamepadKeyboardCursor();

    int gamepadKeyboardMaxLength();

    /**
     * Validates a prospective insertion without mutating the underlying editor.
     * Implementations with richer rules can override this policy per field.
     */
    default boolean gamepadKeyboardAcceptsInsertion(String value, int cursor, String insertion) {
        return insertion != null
            && !insertion.isEmpty()
            && StringUtil.filterText(insertion).equals(insertion);
    }

    void gamepadKeyboardApply(String value, int cursor);
}
