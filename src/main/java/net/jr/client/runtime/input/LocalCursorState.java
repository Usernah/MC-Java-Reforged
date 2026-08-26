package net.jr.client.runtime.input;

import net.minecraft.client.gui.screens.Screen;

final class LocalCursorState {
    double virtualCursorX;
    double virtualCursorY;
    Screen lastScreen;
    CursorInputSource cursorInputSource = CursorInputSource.MOUSE;
    boolean hasObservedRawMousePosition;
    double lastObservedRawMouseX;
    double lastObservedRawMouseY;
    boolean focusCursorAnimationActive;
    double focusCursorAnimationStartX;
    double focusCursorAnimationStartY;
    double focusCursorAnimationTargetX;
    double focusCursorAnimationTargetY;
}

enum CursorInputSource {
    MOUSE,
    JOYSTICK,
    MAGNET,
    FOCUS_SLOT
}
