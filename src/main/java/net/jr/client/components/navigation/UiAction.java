package net.jr.client.components.navigation;

public enum UiAction {
    MOVE_UP,
    MOVE_DOWN,
    MOVE_LEFT,
    MOVE_RIGHT,
    ACCEPT,
    CANCEL,
    BACK,
    PAUSE;

    public boolean isDirectional() {
        return this == MOVE_UP || this == MOVE_DOWN || this == MOVE_LEFT || this == MOVE_RIGHT;
    }

    public boolean isVertical() {
        return this == MOVE_UP || this == MOVE_DOWN;
    }

    public boolean isHorizontal() {
        return this == MOVE_LEFT || this == MOVE_RIGHT;
    }
}
