package net.jr.client.runtime.viewport;

import javax.annotation.Nullable;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.context.SlotExecution;

public final class ViewportProjection {
    private ViewportProjection() {
    }

    @Nullable
    public static Size activeSizeOrNull() {
        Integer slotId = SlotExecution.currentSlotIdOrNull();
        if (slotId == null || !ClientRuntime.INSTANCE.viewports().hasViewport(slotId)) {
            return null;
        }
        ViewportArea viewport = ClientRuntime.INSTANCE.viewports().viewport(slotId);
        return new Size(viewport.width(), viewport.height(), viewport.aspectRatio());
    }

    @Nullable
    public static Size activePanoramaSizeOrNull() {
        if (!ViewportPanoramaRenderer.isRendering() || !ViewportRenderScope.hasActiveViewport()) {
            return null;
        }
        ViewportArea viewport = ViewportRenderScope.activeViewport();
        return new Size(viewport.width(), viewport.height(), viewport.aspectRatio());
    }

    public record Size(int width, int height, float aspectRatio) {
    }
}
