package net.jr.client.runtime.viewport;


public final class HudScaleCalculator {
    public static final int MINIMUM_LEVEL = 1;
    public static final int MAXIMUM_LEVEL = 3;

    private HudScaleCalculator() {
    }

    public static int resolveLevel(int requestedLevel) {
        return Math.clamp(requestedLevel, MINIMUM_LEVEL, MAXIMUM_LEVEL);
    }

    public static float splitMultiplier(int requestedLevel) {
        return switch (resolveLevel(requestedLevel)) {
            case 2 -> 1.5F;
            case 3 -> 2.0F;
            default -> 1.0F;
        };
    }

    public static float singlePlayerMultiplier(int requestedLevel) {
        return switch (resolveLevel(requestedLevel)) {
            case 2 -> 2.5F;
            case 3 -> 3.0F;
            default -> 2.0F;
        };
    }

}
