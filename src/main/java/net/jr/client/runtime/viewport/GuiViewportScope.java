package net.jr.client.runtime.viewport;

import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class GuiViewportScope implements AutoCloseable {
    private static final ThreadLocal<Deque<CoordinateMapping>> COORDINATE_MAPPINGS =
        ThreadLocal.withInitial(ArrayDeque::new);

    private final GuiGraphicsExtractor graphics;
    private final ViewportRenderScope.Scope viewportScope;
    private final ViewportGuiScale.Scope screenScaleScope;
    private final CoordinateMapping coordinateMapping;
    private boolean closed;

    private GuiViewportScope(GuiGraphicsExtractor graphics, ViewportArea viewport) {
        this.graphics = graphics;
        this.viewportScope = ViewportRenderScope.enterGui(viewport);
        this.screenScaleScope = ViewportGuiScale.enter(viewport);

        float sharedGuiScale = (float)Minecraft.getInstance().getWindow().getGuiScale();
        float destinationX = viewport.x() / sharedGuiScale;
        float destinationY = viewport.y() / sharedGuiScale;
        float destinationRight = (viewport.x() + viewport.width()) / sharedGuiScale;
        float destinationBottom = (viewport.y() + viewport.height()) / sharedGuiScale;
        float destinationWidth = destinationRight - destinationX;
        float destinationHeight = destinationBottom - destinationY;
        int logicalWidth = ViewportGuiScale.logicalWidth(viewport);
        int logicalHeight = ViewportGuiScale.logicalHeight(viewport);
        this.coordinateMapping = new CoordinateMapping(
            destinationX,
            destinationY,
            destinationWidth / logicalWidth,
            destinationHeight / logicalHeight
        );
        COORDINATE_MAPPINGS.get().push(this.coordinateMapping);

        graphics.pose().pushMatrix();
        graphics.pose().translate(destinationX, destinationY);
        graphics.pose().scale(destinationWidth / logicalWidth, destinationHeight / logicalHeight);
        graphics.enableScissor(0, 0, logicalWidth, logicalHeight);
    }

    public static GuiViewportScope enter(
        GuiGraphicsExtractor graphics,
        ViewportArea viewport
    ) {
        return new GuiViewportScope(graphics, viewport);
    }

    public static int mapPictureInPictureX(int x) {
        CoordinateMapping mapping = currentMappingOrNull();
        return mapping == null ? x : Math.round(mapping.x() + x * mapping.scaleX());
    }

    public static int mapPictureInPictureY(int y) {
        CoordinateMapping mapping = currentMappingOrNull();
        return mapping == null ? y : Math.round(mapping.y() + y * mapping.scaleY());
    }

    public static float mapPictureInPictureScale(float scale) {
        CoordinateMapping mapping = currentMappingOrNull();
        return mapping == null ? scale : scale * Math.min(mapping.scaleX(), mapping.scaleY());
    }

    public static int mapScissorTestX(int x) {
        CoordinateMapping mapping = currentMappingOrNull();
        return mapping == null ? x : Math.round(mapping.x() + x * mapping.scaleX());
    }

    public static int mapScissorTestY(int y) {
        CoordinateMapping mapping = currentMappingOrNull();
        return mapping == null ? y : Math.round(mapping.y() + y * mapping.scaleY());
    }

    private static CoordinateMapping currentMappingOrNull() {
        Deque<CoordinateMapping> mappings = COORDINATE_MAPPINGS.get();
        return mappings.isEmpty() ? null : mappings.peek();
    }

    @Override
    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        Deque<CoordinateMapping> mappings = COORDINATE_MAPPINGS.get();
        if (mappings.isEmpty() || mappings.pop() != this.coordinateMapping) {
            throw new IllegalStateException("GUI viewport coordinate scope is unbalanced");
        }
        if (mappings.isEmpty()) {
            COORDINATE_MAPPINGS.remove();
        }
        this.graphics.disableScissor();
        this.graphics.pose().popMatrix();
        this.screenScaleScope.close();
        this.viewportScope.close();
    }

    private record CoordinateMapping(float x, float y, float scaleX, float scaleY) {
    }
}
