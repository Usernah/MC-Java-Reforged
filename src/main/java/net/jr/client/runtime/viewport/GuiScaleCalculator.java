package net.jr.client.runtime.viewport;

import net.jr.client.runtime.ClientRuntime;
import net.jr.client.ui.presentation.UiPresentation;

public final class GuiScaleCalculator {
    private static final int STANDARD_BASE_WIDTH = 320;
    private static final int STANDARD_BASE_HEIGHT = 240;
    private static final int PORTABLE_BASE_WIDTH = 280;
    private static final int PORTABLE_BASE_HEIGHT = 200;

    private GuiScaleCalculator() {
    }

    public static int resolve(int width, int height, int requestedScale, boolean enforceUnicode) {
        int scale = 1;
        boolean smallMetrics = usesSmallMetrics();
        int baseWidth = smallMetrics ? PORTABLE_BASE_WIDTH : STANDARD_BASE_WIDTH;
        int baseHeight = smallMetrics ? PORTABLE_BASE_HEIGHT : STANDARD_BASE_HEIGHT;

        while (scale != requestedScale
            && scale < width
            && scale < height
            && width / (scale + 1) >= baseWidth
            && height / (scale + 1) >= baseHeight) {
            scale++;
        }
        if (enforceUnicode && scale % 2 != 0) {
            scale++;
        }
        return scale;
    }

    private static boolean usesSmallMetrics() {
        if (UiPresentation.isPortable()) {
            return true;
        }
        if (!UiPresentation.isSplitScreen()) {
            return false;
        }
        ViewportLayout layout = ClientRuntime.INSTANCE.slots().layout();
        return layout == ViewportLayout.TWO_HORIZONTAL || layout == ViewportLayout.FOUR_GRID;
    }

    public static int maximumForSlot(int slotId, boolean enforceUnicode) {
        if (!ClientRuntime.INSTANCE.hasWindowMetrics()) {
            return 1;
        }
        var slots = ClientRuntime.INSTANCE.slots();
        var slot = slots.slot(slotId);
        if (slot.hasViewport()) {
            var viewport = slot.viewport();
            return resolve(viewport.glWidth(), viewport.glHeight(), 0, enforceUnicode);
        }
        var metrics = ClientRuntime.INSTANCE.windowMetrics();
        return resolve(metrics.framebufferWidth(), metrics.framebufferHeight(), 0, enforceUnicode);
    }
}
