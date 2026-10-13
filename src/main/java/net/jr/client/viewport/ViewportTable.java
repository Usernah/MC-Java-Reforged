package net.jr.client.viewport;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public final class ViewportTable implements Iterable<Viewport> {
    private final ViewportLayout layout;
    private final WindowMetrics window;
    private final List<Viewport> viewports;

    public ViewportTable(ViewportLayout layout, WindowMetrics window, int[] requestedGuiScales,
                         GuiScale guiScale, boolean unicode) {
        this.layout = Objects.requireNonNull(layout, "layout");
        this.window = Objects.requireNonNull(window, "window");
        Objects.requireNonNull(requestedGuiScales, "requestedGuiScales");
        Objects.requireNonNull(guiScale, "guiScale");
        layout.requireCount(requestedGuiScales.length);

        Viewport.Rect[] frame = split(layout, window.framebufferWidth(), window.framebufferHeight());
        Viewport.Rect[] pixels = split(layout, window.windowWidth(), window.windowHeight());
        Viewport[] result = new Viewport[requestedGuiScales.length];
        for (int index = 0; index < result.length; index++) {
            int scale = guiScale.resolve(frame[index].width(), frame[index].height(),
                    requestedGuiScales[index], unicode);
            result[index] = new Viewport(index, frame[index], pixels[index], scale);
        }
        this.viewports = List.of(result);
    }

    private static Viewport.Rect[] split(ViewportLayout layout, int width, int height) {
        if (((layout == ViewportLayout.TWO_VERTICAL || layout == ViewportLayout.GRID) && width < 2)
                || ((layout == ViewportLayout.TWO_HORIZONTAL || layout == ViewportLayout.GRID) && height < 2)) {
            throw new IllegalArgumentException("Window is too small for layout " + layout);
        }
        int left = width / 2;
        int top = height / 2;
        return switch (layout) {
            case SINGLE -> new Viewport.Rect[] { new Viewport.Rect(0, 0, width, height) };
            case TWO_VERTICAL -> new Viewport.Rect[] {
                    new Viewport.Rect(0, 0, left, height),
                    new Viewport.Rect(left, 0, width - left, height) };
            case TWO_HORIZONTAL -> new Viewport.Rect[] {
                    new Viewport.Rect(0, 0, width, top),
                    new Viewport.Rect(0, top, width, height - top) };
            case GRID -> new Viewport.Rect[] {
                    new Viewport.Rect(0, 0, left, top),
                    new Viewport.Rect(left, 0, width - left, top),
                    new Viewport.Rect(0, top, left, height - top),
                    new Viewport.Rect(left, top, width - left, height - top) };
        };
    }

    public ViewportLayout layout() { return this.layout; }
    public WindowMetrics window() { return this.window; }
    public int size() { return this.viewports.size(); }
    public Viewport get(int index) { return this.viewports.get(index); }
    public List<Viewport> asList() { return this.viewports; }

    @Override
    public Iterator<Viewport> iterator() { return this.viewports.iterator(); }
}
