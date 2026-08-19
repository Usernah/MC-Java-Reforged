package net.jr.client.runtime.viewport;

import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.slot.LocalClientSlot;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.WindowRenderState;

/** Keeps viewport-edge GUI scissors aligned with the exact framebuffer partition. */
public final class GuiScissorBounds {
    private GuiScissorBounds() {
    }

    public static PixelRectangle resolve(ScreenRectangle rectangle, WindowRenderState window) {
        int scale = window.guiScale;
        int left = rectangle.left() * scale;
        int top = rectangle.top() * scale;
        int right = Math.min(rectangle.right() * scale, window.width);
        int bottom = Math.min(rectangle.bottom() * scale, window.height);

        if (!ClientRuntime.INSTANCE.viewports().hasWindowMetrics()) {
            return new PixelRectangle(left, top, right, bottom);
        }

        for (LocalClientSlot slot : ClientRuntime.INSTANCE.viewports().drawableSlots()) {
            ViewportArea viewport = ClientRuntime.INSTANCE.viewports().viewport(slot.id());
            int guiLeft = (int)Math.floor(viewport.x() / (float)scale);
            int guiTop = (int)Math.floor(viewport.y() / (float)scale);
            int guiRight = guiLeft + (int)Math.floor(viewport.width() / (float)scale);
            int guiBottom = guiTop + (int)Math.floor(viewport.height() / (float)scale);

            if (rectangle.left() == guiLeft) {
                left = viewport.x();
            }
            if (rectangle.top() == guiTop) {
                top = viewport.y();
            }
            if (rectangle.right() == guiRight) {
                right = viewport.x() + viewport.width();
            }
            if (rectangle.bottom() == guiBottom) {
                bottom = viewport.y() + viewport.height();
            }
        }
        return new PixelRectangle(left, top, right, bottom);
    }

    public record PixelRectangle(int left, int top, int right, int bottom) {
    }
}
