package net.jr.client.ui.presentation;


public enum UiFramebufferScale {
    FULL_HD(0.25F),
    HD(0.50F),
    SMALL(0.50F);

    private final float value;

    UiFramebufferScale(float value) {
        if (!Float.isFinite(value) || value <= 0.0F) {
            throw new IllegalArgumentException("Framebuffer scale must be positive and finite");
        }
        this.value = value;
    }

    public float value() {
        return this.value;
    }

    public static UiFramebufferScale forResolution(UiResolution resolution) {
        return switch (resolution) {
            case FULL_HD -> FULL_HD;
            case HD -> HD;
        };
    }
}
