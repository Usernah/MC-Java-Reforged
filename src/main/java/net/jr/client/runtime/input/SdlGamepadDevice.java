package net.jr.client.runtime.input;

import javax.annotation.Nullable;
import net.jr.api.client.input.ControllerGlyphSet;
import net.jr.api.client.input.GamepadAxis;
import net.jr.api.client.input.GamepadDigitalInput;
import net.jr.api.client.input.GamepadIdentity;
import net.jr.api.client.input.IGamepad;
import net.jr.api.client.input.IGamepadHaptics;
import net.jr.api.client.input.IGamepadInput;
import net.jr.api.client.input.IGamepadProfile;
import net.jr.client.input.gamepad.GamepadInput;
import net.jr.client.input.gamepad.GamepadProfileResolver;
import net.jr.client.input.sdl.SdlGamepad;

/** The SDL instance handle stays on this side of the input API. */
final class SdlGamepadDevice implements IGamepad, ReconnectableGamepad {
    private static final IGamepadHaptics NO_HAPTICS = new IGamepadHaptics() {
        @Override public boolean supported() { return false; }
        @Override public boolean rumble(float lowFrequency, float highFrequency, int durationMs) { return false; }
    };

    private final long deviceId;
    private final IGamepadInput input = new SdlInput();
    private final IGamepadProfile profile = new SdlProfile();

    SdlGamepadDevice(long deviceId) {
        this.deviceId = deviceId;
    }

    long deviceId() {
        return this.deviceId;
    }

    String serial() {
        return SdlGamepad.serial(this.deviceId);
    }

    String path() {
        return SdlGamepad.path(this.deviceId);
    }

    @Nullable
    @Override
    public PhysicalGamepadIdentity physicalIdentity() {
        GamepadIdentity identity = this.identity();
        return identity == null ? null
            : new PhysicalGamepadIdentity(identity.key(), this.serial(), this.path());
    }

    @Override
    public String displayName() {
        GamepadIdentity identity = this.identity();
        return identity == null ? "Gamepad" : identity.displayName();
    }

    @Override
    public boolean connected() {
        return SdlGamepad.isConnected(this.deviceId);
    }

    @Nullable
    @Override
    public GamepadIdentity identity() {
        return SdlGamepad.identity(this.deviceId);
    }

    @Override
    public IGamepadInput input() {
        return this.input;
    }

    @Override
    public IGamepadProfile profile() {
        return this.profile;
    }

    private final class SdlInput implements IGamepadInput {
        @Override
        public boolean pressed(GamepadDigitalInput input) {
            return SdlGamepad.input(deviceId, input, GamepadInput.DIGITAL_PRESS_THRESHOLD);
        }

        @Override
        public float axis(GamepadAxis axis, float deadzone) {
            return SdlGamepad.axis01(deviceId, axis, deadzone);
        }

        @Override
        public float rawAxis(GamepadAxis axis) {
            return SdlGamepad.rawAxis(deviceId, axis);
        }

        @Override
        public boolean calibrationInputActive() {
            return SdlGamepad.calibrationInputActive(deviceId);
        }
    }

    private final class SdlProfile implements IGamepadProfile {
        @Override
        public ControllerGlyphSet glyphs() {
            return GamepadProfileResolver.glyphs(identity());
        }

        @Override
        public IGamepadHaptics haptics() {
            return NO_HAPTICS;
        }
    }
}
