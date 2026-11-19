package net.jr.client.components.navigation;

public interface UiActionHandler {
    boolean handleUiAction(UiAction action);

    default boolean prefersDirectFocusAction() {
        return false;
    }

    default boolean participatesInFocusNavigation() {
        return true;
    }
}
