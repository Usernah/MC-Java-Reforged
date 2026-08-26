package net.jr.client.input.sdl;

import com.sun.jna.ptr.ByteByReference;
import com.sun.jna.ptr.FloatByReference;
import dev.isxander.sdl3java.api.events.SDL_EventType;
import dev.isxander.sdl3java.api.gamepad.SDL_Gamepad;

final class SdlTouchpadState {
    private static final long TAP_COORDINATE_GRACE_NANOS = 50_000_000L;
    private static final int SIDE_NONE = 0;
    private static final int SIDE_LEFT = 1;
    private static final int SIDE_RIGHT = 2;

    private final ByteByReference polledFingerState = new ByteByReference();
    private final FloatByReference polledX = new FloatByReference();
    private final FloatByReference polledY = new FloatByReference();
    private final FloatByReference polledPressure = new FloatByReference();
    private float currentX;
    private float lastCoordinateX;
    private long lastCoordinateNanos;
    private long lastButtonReleaseNanos;
    private boolean fingerDown;
    private boolean coordinateValid;
    private boolean buttonDown;
    private int pressedSide = SIDE_NONE;

    void handleFingerEvent(int type, float x) {
        if (type == SDL_EventType.SDL_EVENT_GAMEPAD_TOUCHPAD_UP) {
            recordCoordinate(x);
            clearActiveFinger();
            return;
        }
        this.fingerDown = true;
        if (validCoordinate(x)) recordCoordinate(x);
        else this.coordinateValid = false;
    }

    void refreshFinger(SDL_Gamepad gamepad) {
        if (gamepad == null) {
            clearActiveFinger();
            return;
        }
        int touchpadCount = dev.isxander.sdl3java.api.gamepad.SdlGamepad.SDL_GetNumGamepadTouchpads(gamepad);
        int fingerCount = touchpadCount <= 0
            ? 0
            : dev.isxander.sdl3java.api.gamepad.SdlGamepad.SDL_GetNumGamepadTouchpadFingers(gamepad, 0);
        if (fingerCount <= 0) {
            clearActiveFinger();
            return;
        }
        boolean read = dev.isxander.sdl3java.api.gamepad.SdlGamepad.SDL_GetGamepadTouchpadFinger(
            gamepad, 0, 0, this.polledFingerState, this.polledX, this.polledY, this.polledPressure
        );
        if (!read) return;
        if (this.polledFingerState.getValue() == 0) {
            clearActiveFinger();
            return;
        }
        float x = this.polledX.getValue();
        this.fingerDown = true;
        if (validCoordinate(x)) recordCoordinate(x);
        else this.coordinateValid = false;
    }

    void syncButton(boolean down) {
        long now = System.nanoTime();
        if (down && !this.buttonDown) {
            this.pressedSide = resolvePressedSide(now);
        } else if (!down) {
            if (this.buttonDown) this.lastButtonReleaseNanos = now;
            this.pressedSide = SIDE_NONE;
        }
        this.buttonDown = down;
    }

    int pressedSide() {
        return this.pressedSide;
    }

    private int resolvePressedSide(long now) {
        if (this.fingerDown && this.coordinateValid) return sideFor(this.currentX);
        boolean belongsToCurrentPress = this.lastCoordinateNanos > this.lastButtonReleaseNanos;
        boolean recentEnough = now - this.lastCoordinateNanos <= TAP_COORDINATE_GRACE_NANOS;
        return belongsToCurrentPress && recentEnough ? sideFor(this.lastCoordinateX) : SIDE_NONE;
    }

    private void recordCoordinate(float x) {
        if (!validCoordinate(x)) {
            this.coordinateValid = false;
            return;
        }
        this.currentX = x;
        this.lastCoordinateX = x;
        this.lastCoordinateNanos = System.nanoTime();
        this.coordinateValid = true;
    }

    private void clearActiveFinger() {
        this.fingerDown = false;
        this.coordinateValid = false;
    }

    private static int sideFor(float x) {
        return x < 0.5F ? SIDE_LEFT : SIDE_RIGHT;
    }

    private static boolean validCoordinate(float coordinate) {
        return Float.isFinite(coordinate) && coordinate >= 0.0F && coordinate <= 1.0F;
    }
}
