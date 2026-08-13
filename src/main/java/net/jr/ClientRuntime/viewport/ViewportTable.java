package net.jr.ClientRuntime.viewport;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public final class ViewportTable implements Iterable<ViewportArea> {
    private final ViewportLayout layout;
    private final WindowMetrics metrics;
    private final ViewportArea[] viewports;
    private static final boolean FORCE_GRID_GUI_SCALE = false;
    private static final int GRID_GUI_SCALE = 2;

    public ViewportTable(ViewportLayout layout, WindowMetrics metrics, int[] requestedGuiScales, boolean enforceUnicode) {
        this.layout = layout;
        this.metrics = metrics;
        this.viewports = build(layout, metrics, requestedGuiScales.clone(), enforceUnicode);
    }

    private static ViewportArea[] build(ViewportLayout layout, WindowMetrics metrics, int[] requestedGuiScales, boolean enforceUnicode) {
        return switch (layout) {
            case SINGLE -> new ViewportArea[] {
                area(layout, 0, metrics, rect(0, 0, metrics.framebufferWidth(), metrics.framebufferHeight()), rect(0, 0, metrics.windowWidth(), metrics.windowHeight()), rect(0, 0, metrics.guiWidth(), metrics.guiHeight()), requestedGuiScales[0], enforceUnicode)
            };
            case TWO_VERTICAL -> buildTwoVertical(layout, metrics, requestedGuiScales, enforceUnicode);
            case TWO_HORIZONTAL -> buildTwoHorizontal(layout, metrics, requestedGuiScales, enforceUnicode);
            case FOUR_GRID -> buildFourGrid(layout, metrics, requestedGuiScales, enforceUnicode);
        };
    }

    private static ViewportArea[] buildTwoVertical(ViewportLayout layout, WindowMetrics metrics, int[] requestedGuiScales, boolean enforceUnicode) {
        Rect[] framebuffer = splitVertical(metrics.framebufferWidth(), metrics.framebufferHeight());
        Rect[] window = splitVertical(metrics.windowWidth(), metrics.windowHeight());
        Rect[] gui = splitVertical(metrics.guiWidth(), metrics.guiHeight());
        return areas(layout, metrics, framebuffer, window, gui, requestedGuiScales, enforceUnicode);
    }

    private static ViewportArea[] buildTwoHorizontal(ViewportLayout layout, WindowMetrics metrics, int[] requestedGuiScales, boolean enforceUnicode) {
        Rect[] framebuffer = splitHorizontal(metrics.framebufferWidth(), metrics.framebufferHeight());
        Rect[] window = splitHorizontal(metrics.windowWidth(), metrics.windowHeight());
        Rect[] gui = splitHorizontal(metrics.guiWidth(), metrics.guiHeight());
        return areas(layout, metrics, framebuffer, window, gui, requestedGuiScales, enforceUnicode);
    }

    private static ViewportArea[] buildFourGrid(ViewportLayout layout, WindowMetrics metrics, int[] requestedGuiScales, boolean enforceUnicode) {
        Rect[] framebuffer = splitGrid(metrics.framebufferWidth(), metrics.framebufferHeight());
        Rect[] window = splitGrid(metrics.windowWidth(), metrics.windowHeight());
        Rect[] gui = splitGrid(metrics.guiWidth(), metrics.guiHeight());
        return areas(layout, metrics, framebuffer, window, gui, requestedGuiScales, enforceUnicode);
    }

    private static ViewportArea[] areas(ViewportLayout layout, WindowMetrics metrics, Rect[] framebuffer, Rect[] window, Rect[] gui, int[] requestedGuiScales, boolean enforceUnicode) {
        ViewportArea[] areas = new ViewportArea[layout.viewportCount()];
        for (int id = 0; id < areas.length; id++) {
            areas[id] = area(layout, id, metrics, framebuffer[id], window[id], gui[id], requestedGuiScales[id], enforceUnicode);
        }
        return areas;
    }

    private static ViewportArea area(ViewportLayout layout, int id, WindowMetrics metrics, Rect framebuffer, Rect window, Rect gui, int requestedScale, boolean enforceUnicode) {
        int guiScale = layout == ViewportLayout.FOUR_GRID && FORCE_GRID_GUI_SCALE
                ? GRID_GUI_SCALE
                : calculateGuiScale(
                framebuffer.width(),
                framebuffer.height(),
                requestedScale,
                enforceUnicode
        );

        double effectiveGuiScale = guiScale;
        int guiWidth = (int)Math.ceil(framebuffer.width() / effectiveGuiScale);
        int guiHeight = (int)Math.ceil(framebuffer.height() / effectiveGuiScale);
        return new ViewportArea(
            layout,
            id,
            framebuffer.x(),
            framebuffer.y(),
            framebuffer.width(),
            framebuffer.height(),
            metrics.framebufferHeight(),
            window.x(),
            window.y(),
            window.width(),
            window.height(),
            gui.x(),
            gui.y(),
                guiWidth,
                guiHeight,
                guiScale,
                effectiveGuiScale
        );
    }

    private static int calculateGuiScale(
            int width,
            int height,
            int requestedScale,
            boolean enforceUnicode
    ) {
        return GuiScaleCalculator.resolve(width, height, requestedScale, enforceUnicode);
    }

    private static Rect[] splitVertical(int width, int height) {
        int leftWidth = width / 2;
        return new Rect[] {
            rect(0, 0, leftWidth, height),
            rect(leftWidth, 0, width - leftWidth, height)
        };
    }

    private static Rect[] splitHorizontal(int width, int height) {
        int topHeight = height / 2;
        return new Rect[] {
            rect(0, 0, width, topHeight),
            rect(0, topHeight, width, height - topHeight)
        };
    }

    private static Rect[] splitGrid(int width, int height) {
        int leftWidth = width / 2;
        int topHeight = height / 2;
        int rightWidth = width - leftWidth;
        int bottomHeight = height - topHeight;
        return new Rect[] {
            rect(0, 0, leftWidth, topHeight),
            rect(leftWidth, 0, rightWidth, topHeight),
            rect(0, topHeight, leftWidth, bottomHeight),
            rect(leftWidth, topHeight, rightWidth, bottomHeight)
        };
    }

    private static Rect rect(int x, int y, int width, int height) {
        return new Rect(x, y, width, height);
    }

    public ViewportArea viewport(int id) {
        this.layout.requireViewport(id);
        return this.viewports[id];
    }

    public ViewportLayout layout() {
        return this.layout;
    }

    public WindowMetrics metrics() {
        return this.metrics;
    }

    public int size() {
        return this.viewports.length;
    }

    public List<ViewportArea> asList() {
        return List.copyOf(Arrays.asList(this.viewports));
    }

    @Override
    public Iterator<ViewportArea> iterator() {
        return this.asList().iterator();
    }

    private record Rect(int x, int y, int width, int height) {
    }
}
