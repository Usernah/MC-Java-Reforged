package net.jr.mixin.uifocus;

import net.jr.client.components.navigation.UiAction;
import net.jr.client.components.navigation.UiActionFeedback;
import net.jr.client.components.navigation.UiActionHandler;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.lwjgl.glfw.GLFW;

@Mixin(AbstractSliderButton.class)
public abstract class AbstractSliderButtonUiActionMixin implements UiActionHandler {
    @Shadow protected boolean canChangeValue;
    @Shadow protected double value;

    @Unique
    private UiActionFeedback javareforged$lastUiActionFeedback = UiActionFeedback.NONE;

    @Override
    public boolean handleUiAction(UiAction action) {
        this.javareforged$lastUiActionFeedback = UiActionFeedback.NONE;
        if (!this.slider().isActive()) {
            return false;
        }

        if (action == UiAction.ACCEPT) {
            this.canChangeValue = !this.canChangeValue;
            this.javareforged$lastUiActionFeedback = UiActionFeedback.CLICK;
            return true;
        }

        if (!this.canChangeValue) {
            return false;
        }

        if (action == UiAction.CANCEL || action == UiAction.BACK) {
            this.canChangeValue = false;
            this.javareforged$lastUiActionFeedback = UiActionFeedback.CLICK;
            return true;
        }

        if (action == UiAction.MOVE_LEFT) {
            this.adjustValue(GLFW.GLFW_KEY_LEFT);
            return true;
        }

        if (action == UiAction.MOVE_RIGHT) {
            this.adjustValue(GLFW.GLFW_KEY_RIGHT);
            return true;
        }

        if (action == UiAction.MOVE_UP || action == UiAction.MOVE_DOWN) {
            this.canChangeValue = false;
        }

        return false;
    }

    @Override
    public UiActionFeedback uiActionFeedback(UiAction action) {
        return this.javareforged$lastUiActionFeedback;
    }

    @Override
    public boolean prefersDirectFocusAction() {
        return true;
    }

    private void adjustValue(int keyCode) {
        double previous = this.value;
        this.slider().keyPressed(new KeyEvent(keyCode, 0, 0));
        this.javareforged$lastUiActionFeedback = Double.compare(previous, this.value) != 0
                ? UiActionFeedback.CHAR
                : UiActionFeedback.NONE;
    }

    private AbstractSliderButton slider() {
        return (AbstractSliderButton) (Object) this;
    }
}
