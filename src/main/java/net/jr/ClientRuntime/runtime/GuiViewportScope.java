package net.jr.ClientRuntime.runtime;

import java.util.ArrayDeque;
import java.util.Deque;
import net.jr.ClientRuntime.viewport.ViewportArea;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Maps one client's logical GUI into its rectangle in the shared GUI render state. */
public final class GuiViewportScope implements AutoCloseable {
    private static final ThreadLocal<Deque<CoordinateMapping>> COORDINATE_MAPPINGS =
        ThreadLocal.withInitial(ArrayDeque::new);

    private final GuiGraphicsExtractor graphics;
    private final ViewportPass.Scope viewportScope;
    private final ScreenScale.Scope screenScaleScope;
    private final CoordinateMapping coordinateMapping;
    private boolean closed;

    private GuiViewportScope(GuiGraphicsExtractor graphics, ViewportArea viewport) {
        this.graphics = graphics;
        this.viewportScope = ViewportPass.enterGui(viewport);
        this.screenScaleScope = ScreenScale.enter(viewport);

        Minecraft minecraft = Minecraft.getInstance();
        float destinationX = viewport.windowX()
            * minecraft.getWindow().getGuiScaledWidth()
            / (float)minecraft.getWindow().getScreenWidth();
        float destinationY = viewport.windowY()
            * minecraft.getWindow().getGuiScaledHeight()
            / (float)minecraft.getWindow().getScreenHeight();
        float destinationWidth = viewport.windowWidth()
            * minecraft.getWindow().getGuiScaledWidth()
            / (float)minecraft.getWindow().getScreenWidth();
        float destinationHeight = viewport.windowHeight()
            * minecraft.getWindow().getGuiScaledHeight()
            / (float)minecraft.getWindow().getScreenHeight();
        int logicalWidth = ScreenScale.logicalWidth(viewport);
        int logicalHeight = ScreenScale.logicalHeight(viewport);
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

    public static GuiViewportScope enter(GuiGraphicsExtractor graphics) {
        return new GuiViewportScope(graphics, Client.viewport());
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
