package net.jr.api.client.input;

public interface IGamepadHaptics {
    boolean supported();


    boolean rumble(float lowFrequency, float highFrequency, int durationMs);
}
