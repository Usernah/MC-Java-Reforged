package net.jr.client.runtime.viewport;

import net.jr.api.client.ui.Viewport;
import net.jr.client.ui.presentation.UiPresentation;

public final class GuiScaleCalculator {
    private static final int STANDARD_BASE_WIDTH = 320;
    private static final int STANDARD_BASE_HEIGHT = 240;

    private GuiScaleCalculator() {
    }

    public static int resolve(int width, int height, int requestedScale, boolean enforceUnicode) {
        int maximum = resolveStandard(
            width,
            height,
            0,
            enforceUnicode
        );
        if (!UiPresentation.isSmall() && maximum > 1) {
            maximum--;
        }
        return requestedScale == 0
            ? maximum
            : Math.clamp(requestedScale, 1, maximum);
    }

    public static int resolveStandard(
        int width,
        int height,
        int requestedScale,
        boolean enforceUnicode
    ) {
        return resolve(
            width,
            height,
            requestedScale,
            enforceUnicode,
            STANDARD_BASE_WIDTH,
            STANDARD_BASE_HEIGHT
        );
    }

    private static int resolve(
        int width,
        int height,
        int requestedScale,
        boolean enforceUnicode,
        int baseWidth,
        int baseHeight
    ) {
        int scale = 1;

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

    public static int maximumForSlot(int slotId, boolean enforceUnicode) {
        var viewport = Viewport.getOrNull(slotId);
        if (viewport != null) {
            return resolve(viewport.glWidth(), viewport.glHeight(), 0, enforceUnicode);
        }
        var metrics = Viewport.windowOrNull();
        if (metrics == null) {
            return 1;
        }
        return resolve(metrics.framebufferWidth(), metrics.framebufferHeight(), 0, enforceUnicode);
    }
}
