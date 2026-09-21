package net.jr.client.input.sdl;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.jr.Java_reforged;
import net.jr.api.client.input.GamepadIdentity;
import net.jr.client.input.gamepad.RawGamepadInput;
import net.jr.client.input.gamepad.RawGamepadInputPress;

final class SdlRawInputTracker {
    private static final float AXIS_DIGITAL_THRESHOLD = 0.55F;
    private static final boolean LOG_RAW_INPUT = Boolean.getBoolean("split.sdlRawInputLog");
    private static final ConcurrentLinkedQueue<RawGamepadInputPress> PRESSES = new ConcurrentLinkedQueue<>();
    private static final Map<Long, SdlGamepadSnapshot> BASELINES = new LinkedHashMap<>();

    private SdlRawInputTracker() {
    }

    static void clearPresses() {
        PRESSES.clear();
    }

    static RawGamepadInputPress pollPress() {
        return PRESSES.poll();
    }

    static void publish(Map<Long, SdlGamepadSnapshot> snapshots) {
        for (Map.Entry<Long, SdlGamepadSnapshot> entry : snapshots.entrySet()) {
            queueEdges(entry.getKey(), entry.getValue());
        }
        BASELINES.keySet().removeIf(id -> !snapshots.containsKey(id));
    }

    static void remove(long deviceId) {
        BASELINES.remove(deviceId);
    }

    static void clear() {
        BASELINES.clear();
        PRESSES.clear();
    }

    private static void queueEdges(long deviceId, SdlGamepadSnapshot next) {
        SdlGamepadSnapshot previous = BASELINES.getOrDefault(deviceId, SdlGamepadSnapshot.disconnected());
        if (!next.connected() || next.identity() == null) {
            BASELINES.put(deviceId, next);
            return;
        }
        if (!previous.connected()
            || previous.identity() == null
            || !previous.identity().key().equals(next.identity().key())) {
            BASELINES.put(deviceId, next);
            return;
        }

        GamepadIdentity identity = next.identity();
        int gamepadButtonCount = Math.min(previous.buttons().length, next.buttons().length);
        for (int button = 0; button < gamepadButtonCount; button++) {
            if (next.buttons()[button] && !previous.buttons()[button]) {
                record(deviceId, identity, RawGamepadInput.gamepadButton(button));
            }
        }
        int rawButtonCount = Math.min(previous.rawButtons().length, next.rawButtons().length);
        for (int button = 0; button < rawButtonCount; button++) {
            if (next.rawButtons()[button] && !previous.rawButtons()[button]) {
                record(deviceId, identity, RawGamepadInput.button(button));
            }
        }
        int rawHatCount = Math.min(previous.rawHats().length, next.rawHats().length);
        for (int hat = 0; hat < rawHatCount; hat++) {
            int previousValue = previous.rawHats()[hat];
            int nextValue = next.rawHats()[hat];
            if (nextValue != 0 && nextValue != previousValue) {
                record(deviceId, identity, RawGamepadInput.hat(hat, nextValue));
            }
        }
        int axisCount = Math.min(previous.axes().length, next.axes().length);
        for (int axis = 0; axis < axisCount; axis++) {
            int previousDirection = axisDirection(previous, axis);
            int nextDirection = axisDirection(next, axis);
            if (nextDirection != 0 && nextDirection != previousDirection) {
                record(deviceId, identity, RawGamepadInput.axis(axis, nextDirection));
            }
        }
        BASELINES.put(deviceId, next);
    }

    private static int axisDirection(SdlGamepadSnapshot snapshot, int axis) {
        float value = snapshot.axis(axis);
        if (value >= AXIS_DIGITAL_THRESHOLD) return 1;
        if (value <= -AXIS_DIGITAL_THRESHOLD) return -1;
        return 0;
    }

    private static void record(long deviceId, GamepadIdentity identity, RawGamepadInput input) {
        if (identity == null || input == null) return;
        PRESSES.offer(new RawGamepadInputPress(deviceId, identity, input, System.currentTimeMillis()));
        if (LOG_RAW_INPUT) {
            Java_reforged.LOGGER.info(
                "SDL raw input capturado: {} desde {}.", input.displayName(), identity.displayName()
            );
        }
    }
}
