package net.jr.ClientRuntime.runtime;

import net.jr.ClientRuntime.slot.PlayerSlot;
import net.jr.ClientRuntime.viewport.ViewportArea;
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

        if (!LocalPlayers.INSTANCE.hasWindowMetrics()) {
            return new PixelRectangle(left, top, right, bottom);
        }

        for (PlayerSlot slot : LocalPlayers.INSTANCE.slots().visibleSlots()) {
            if (!slot.hasViewport()) {
                continue;
            }
            ViewportArea viewport = slot.viewport();
            // GuiGraphicsExtractor creates its ScreenRectangle by transforming
            // the origin and the size independently, flooring both values.
            // Reproduce that exact operation here. floor(origin + size) is not
            // equivalent when either value contains a fractional GUI pixel.
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
