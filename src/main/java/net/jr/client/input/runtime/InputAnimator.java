package net.jr.client.input.runtime;

import java.util.HashMap;
import java.util.Map;

final class InputAnimator {
    private static final Map<String, State> STATES = new HashMap<>();
    private InputAnimator() {}

    static Builder value(String key) { return new Builder(key); }

    static final class Builder {
        private final String key;
        private float from, to;
        private int duration;
        private float easeIn = 1, easeOut = 1;
        Builder(String key) { this.key = key; }
        Builder fromTo(float from, float to) { this.from = from; this.to = to; return this; }
        Builder time(int duration) { this.duration = duration; return this; }
        Builder ease(float in, float out) { easeIn = in; easeOut = out; return this; }
        float getFloat() {
            long now = System.currentTimeMillis();
            State state = STATES.get(key);
            if (state == null || state.from != from || state.to != to || state.duration != duration) {
                state = new State(from, to, duration, easeIn, easeOut, now);
                STATES.put(key, state);
            }
            if (duration <= 0) return to;
            float progress = Math.clamp((now - state.start) / (float) duration, 0, 1);
            float eased = progress < .5f
                ? .5f * (float) Math.pow(progress * 2, easeIn)
                : .5f + .5f * (1 - (float) Math.pow(2 - progress * 2, easeOut));
            return from + (to - from) * eased;
        }
    }
    private record State(float from, float to, int duration, float easeIn, float easeOut, long start) {}
}
