package net.jr.client.ui.presentation;

import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.viewport.ViewportLayout;

public final class WindowScalePolicy {
    private WindowScalePolicy() {
    }

    public static boolean useCompactMetrics() {
        if (UiPresentation.isPortable()) {
            return true;
        }
        if (!UiPresentation.isSplitScreen()) {
            return false;
        }
        ViewportLayout layout = ClientRuntime.INSTANCE.viewports().layout();
        return layout == ViewportLayout.TWO_HORIZONTAL || layout == ViewportLayout.FOUR_GRID;
    }
}
