package net.jr.client.runtime.viewport;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.joml.Vector2f;
import net.jr.client.ui.render.composition.GuiCompositionScope;

public final class GuiScissorHandler {
    private GuiScissorHandler() {
    }

    public static void enabled(
            GuiGraphicsExtractor graphics,
            GuiScissorState state,
            double left,
            double top,
            double right,
            double bottom
    ) {
        if (usesPreciseScissor()) {
            pushPrecise(graphics, state, left, top, right, bottom);
        }
    }

    public static void disabled(GuiScissorState state) {
        if (usesPreciseScissor()) {
            state.popPreciseScissor();
        }
    }

    public static void enablePrecise(
            GuiGraphicsExtractor graphics,
            GuiScissorState state,
            double left,
            double top,
            double right,
            double bottom
    ) {
        graphics.enableScissor(
                (int) Math.floor(left),
                (int) Math.floor(top),
                (int) Math.ceil(right),
                (int) Math.ceil(bottom)
        );
        if (!usesPreciseScissor()) {
            return;
        }
        state.popPreciseScissor();
        pushPrecise(graphics, state, left, top, right, bottom);
    }

    private static void pushPrecise(
            GuiGraphicsExtractor graphics,
            GuiScissorState state,
            double left,
            double top,
            double right,
            double bottom
    ) {
        PreciseGuiScissor previous = state.currentPreciseScissor();
        GuiCompositionScope.Extraction composition =
            GuiCompositionScope.activeExtractionOrNull();
        if (composition != null) {
            Vector2f p0 = graphics.pose().transformPosition((float)left, (float)top, new Vector2f());
            Vector2f p1 = graphics.pose().transformPosition((float)right, (float)bottom, new Vector2f());
            double rasterScale = composition.rasterScale();
            PreciseGuiScissor precise = new PreciseGuiScissor(
                Math.min(p0.x, p1.x) * rasterScale,
                Math.min(p0.y, p1.y) * rasterScale,
                Math.max(p0.x, p1.x) * rasterScale,
                Math.max(p0.y, p1.y) * rasterScale
            );
            if (previous != null) {
                precise = precise.intersection(previous);
            }
            state.pushPreciseScissor(precise);
            ScreenRectangle vanillaScissor = graphics.peekScissorStack();
            if (vanillaScissor != null) {
                ((PreciseScissorAccess)(Object)vanillaScissor)
                    .javaReforged$setPreciseScissor(precise);
            }
            return;
        }
        ViewportGuiScale.Context viewportContext = ViewportGuiScale.activeOrNull();
        PreciseGuiScissor precise;
        if (previous == null
                && viewportContext != null
                && left == 0.0D
                && top == 0.0D
                && right == viewportContext.logicalWidth()
                && bottom == viewportContext.logicalHeight()) {
            var viewport = viewportContext.viewport();
            precise = new PreciseGuiScissor(
                    viewport.x(),
                    viewport.y(),
                    viewport.x() + viewport.width(),
                    viewport.y() + viewport.height()
            );
        } else {
            Vector2f p0 = graphics.pose().transformPosition((float) left, (float) top, new Vector2f());
            Vector2f p1 = graphics.pose().transformPosition((float) right, (float) bottom, new Vector2f());
            double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
            precise = new PreciseGuiScissor(
                    Math.min(p0.x, p1.x) * guiScale,
                    Math.min(p0.y, p1.y) * guiScale,
                    Math.max(p0.x, p1.x) * guiScale,
                    Math.max(p0.y, p1.y) * guiScale
            );
            if (previous != null) {
                precise = precise.intersection(previous);
            }
        }

        state.pushPreciseScissor(precise);
        ScreenRectangle vanillaScissor = graphics.peekScissorStack();
        if (vanillaScissor != null) {
            ((PreciseScissorAccess) (Object) vanillaScissor).javaReforged$setPreciseScissor(precise);
        }
    }

    private static boolean usesPreciseScissor() {
        return ViewportGuiScale.isActive() || GuiCompositionScope.isExtracting();
    }
}
