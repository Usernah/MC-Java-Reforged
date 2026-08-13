package net.jr.client.ui.navigation;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.InputType;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/** Mutable UI-navigation state owned by one local player slot. */
public final class UiNavigationState {
    private boolean focusNavigationActive;
    private InputType lastInputType = InputType.NONE;
    private Screen observedScreen;
    private boolean hasObservedRawMousePosition;
    private double lastObservedRawMouseX;
    private double lastObservedRawMouseY;
    private final Map<AbstractContainerScreen<?>, Integer> containerSlotFocus = new WeakHashMap<>();

    public boolean focusNavigationActive() {
        return this.focusNavigationActive;
    }

    public void setFocusNavigationActive(boolean focusNavigationActive) {
        this.focusNavigationActive = focusNavigationActive;
    }

    public InputType lastInputType() {
        return this.lastInputType;
    }

    public void setLastInputType(InputType lastInputType) {
        this.lastInputType = lastInputType;
    }

    public Screen observedScreen() {
        return this.observedScreen;
    }

    public void setObservedScreen(Screen observedScreen) {
        this.observedScreen = observedScreen;
    }

    public boolean hasObservedRawMousePosition() {
        return this.hasObservedRawMousePosition;
    }

    public void setHasObservedRawMousePosition(boolean hasObservedRawMousePosition) {
        this.hasObservedRawMousePosition = hasObservedRawMousePosition;
    }

    public double lastObservedRawMouseX() {
        return this.lastObservedRawMouseX;
    }

    public void setLastObservedRawMouseX(double lastObservedRawMouseX) {
        this.lastObservedRawMouseX = lastObservedRawMouseX;
    }

    public double lastObservedRawMouseY() {
        return this.lastObservedRawMouseY;
    }

    public void setLastObservedRawMouseY(double lastObservedRawMouseY) {
        this.lastObservedRawMouseY = lastObservedRawMouseY;
    }

    public Map<AbstractContainerScreen<?>, Integer> containerSlotFocus() {
        return this.containerSlotFocus;
    }

    public void clear() {
        this.focusNavigationActive = false;
        this.lastInputType = InputType.NONE;
        this.observedScreen = null;
        this.hasObservedRawMousePosition = false;
        this.lastObservedRawMouseX = 0.0D;
        this.lastObservedRawMouseY = 0.0D;
        this.containerSlotFocus.clear();
    }
}
