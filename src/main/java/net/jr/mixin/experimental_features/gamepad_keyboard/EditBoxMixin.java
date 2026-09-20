package net.jr.mixin.experimental_features.gamepad_keyboard;

import net.jr.experimental_features.gamepad_keyboard.GamepadTextInput;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EditBox.class)
public abstract class EditBoxMixin implements GamepadTextInput {
    @Shadow public abstract boolean canConsumeInput();
    @Shadow public abstract String getValue();
    @Shadow public abstract int getCursorPosition();
    @Shadow public abstract void setValue(String value);
    @Shadow public abstract void setCursorPosition(int cursor);

    @Accessor("maxLength")
    public abstract int javaReforged$getMaxLength();

    @Override
    public boolean gamepadKeyboardCanConsumeInput() {
        return this.canConsumeInput();
    }

    @Override
    public String gamepadKeyboardValue() {
        return this.getValue();
    }

    @Override
    public int gamepadKeyboardCursor() {
        return this.getCursorPosition();
    }

    @Override
    public int gamepadKeyboardMaxLength() {
        return this.javaReforged$getMaxLength();
    }

    @Override
    public void gamepadKeyboardApply(String value, int cursor) {
        this.setValue(value);
        this.setCursorPosition(cursor);
    }
}
