package net.jr.api.client.input;

public interface IGamepadHaptics {
    boolean supported();

    /** Returns false when the device cannot perform this effect. */
    boolean rumble(float lowFrequency, float highFrequency, int durationMs);
}
