package net.jr.client.components.navigation;

public interface UiActionHandler {
    boolean handleUiAction(UiAction action);

    default UiActionFeedback uiActionFeedback(UiAction action) {
        return UiActionFeedback.CLICK;
    }

    default boolean prefersDirectFocusAction() {
        return false;
    }
}
