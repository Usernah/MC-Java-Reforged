package net.jr.client.runtime.viewport;

import javax.annotation.Nullable;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.slot.Slots;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.WindowRenderState;

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

        for (Slots slot : ClientRuntime.INSTANCE.viewports().drawableSlots()) {
            ViewportArea viewport =
                    ClientRuntime.INSTANCE.viewports().viewport(slot.id());

            int guiLeft = (int)Math.floor(
                    viewport.x() / (float)scale
            );

            int guiTop = (int)Math.floor(
                    viewport.y() / (float)scale
            );

            int guiRight = (int)Math.floor(
                    (viewport.x() + viewport.width()) / (float)scale
            );

            int guiBottom = (int)Math.floor(
                    (viewport.y() + viewport.height()) / (float)scale
            );

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

    @Nullable
    public static PixelRectangle resolvePrecise(ScreenRectangle rectangle, WindowRenderState window) {
        PreciseGuiScissor precise = ((PreciseScissorAccess) (Object) rectangle).javaReforged$preciseScissor();
        if (precise == null) {
            return null;
        }

        int left = clamp((int) Math.floor(precise.left()), window.width);
        int top = clamp((int) Math.floor(precise.top()), window.height);
        int right = clamp((int) Math.ceil(precise.right()), window.width);
        int bottom = clamp((int) Math.ceil(precise.bottom()), window.height);
        return right > left && bottom > top ? new PixelRectangle(left, top, right, bottom) : null;
    }

    private static int clamp(int value, int maximum) {
        return Math.max(0, Math.min(value, maximum));
    }

    public record PixelRectangle(int left, int top, int right, int bottom) {
    }
}
