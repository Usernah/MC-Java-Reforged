package net.jr.client.ui.presentation;

public final class WindowScalePolicy {
    private WindowScalePolicy() {
    }

    public static boolean useCompactMetrics() {
        return UiPresentation.isPortable();
    }
}
