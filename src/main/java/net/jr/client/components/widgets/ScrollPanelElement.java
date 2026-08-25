package net.jr.client.components.widgets;

import net.jr.client.components.interfaces.IWidgetRenderer;
import net.jr.client.render.GuiGraphicsExtractorBridge;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScrollPanelElement
        extends JavaBaseWidget
        implements ContainerEventHandler {

    public enum Orientation {
        VERTICAL,
        HORIZONTAL
    }

    private final List<JavaBaseWidget> children =
            new ArrayList<>();

    private Orientation orientation =
            Orientation.VERTICAL;

    private float spacing;
    private float padding;
    private int columns = 1;

    private float scissorPadding;

    private double scrollX;
    private double scrollY;
    private double targetScrollX;
    private double targetScrollY;

    private double totalContentWidth;
    private double totalContentHeight;

    private final ScrollbarElement scrollbar;

    private IWidgetRenderer renderer;

    private GuiEventListener focused;
    private boolean dragging;

    private boolean layoutDirty = true;

    private float lastLayoutX = Float.NaN;
    private float lastLayoutY = Float.NaN;
    private float lastLayoutWidth = Float.NaN;
    private float lastLayoutHeight = Float.NaN;

    private boolean autoPlaceScrollbar = true;
    private float minimumScrollbarHandleSize = 8.0F;
    private Float fixedScrollbarHandleSize;

    public ScrollPanelElement(
            int x,
            int y,
            int width,
            int height
    ) {
        super(x, y, width, height, Component.empty());

        this.scrollbar =
                new ScrollbarElement(
                        x + width - 8,
                        y,
                        8,
                        height
                );

        this.scrollbar.setOrientation(
                ScrollbarElement.Orientation.VERTICAL
        );

        this.scrollbar.onPositionChange(
                this::applyScrollbarPosition
        );

        this.placeDefaultScrollbar();
    }

    public void addChild(JavaBaseWidget child) {
        if (child == null || this.children.contains(child)) {
            return;
        }

        this.children.add(child);
        this.markLayoutDirty();
    }

    public void removeChild(JavaBaseWidget child) {
        if (this.children.remove(child)) {
            if (this.focused == child) {
                this.setFocused((GuiEventListener)null);
            }

            this.markLayoutDirty();
        }
    }

    public void removeLastChild() {
        if (this.children.isEmpty()) {
            return;
        }

        this.removeChild(
                this.children.getLast()
        );
    }

    public void clearChildren() {
        if (this.focused != null) {
            this.focused.setFocused(false);
        }
        this.children.clear();
        this.focused = null;

        this.scrollX = 0.0D;
        this.scrollY = 0.0D;
        this.targetScrollX = 0.0D;
        this.targetScrollY = 0.0D;

        this.markLayoutDirty();
    }

    public List<JavaBaseWidget> childWidgets() {
        return Collections.unmodifiableList(this.children);
    }

    public boolean hasFocusableChild() {
        if (!this.active || !this.visible()) {
            return false;
        }

        for (JavaBaseWidget child : this.children) {
            if (this.isFocusableChild(child)) {
                return true;
            }
        }
        return false;
    }

    public Orientation orientation() {
        return this.orientation;
    }

    public void setOrientation(Orientation orientation) {
        this.orientation =
                orientation == null
                        ? Orientation.VERTICAL
                        : orientation;

        this.scrollbar.setOrientation(
                this.orientation == Orientation.VERTICAL
                        ? ScrollbarElement.Orientation.VERTICAL
                        : ScrollbarElement.Orientation.HORIZONTAL
        );

        this.markLayoutDirty();

        if (this.autoPlaceScrollbar) {
            this.placeDefaultScrollbar();
        }
    }

    public float spacing() {
        return this.spacing;
    }

    public void setSpacing(float spacing) {
        this.spacing = Math.max(0.0F, spacing);
        this.markLayoutDirty();
    }

    public float padding() {
        return this.padding;
    }

    public void setPadding(float padding) {
        this.padding = Math.max(0.0F, padding);
        this.markLayoutDirty();
    }

    public int columns() {
        return this.columns;
    }

    public void setColumns(int columns) {
        this.columns = Math.max(1, columns);
        this.markLayoutDirty();
    }

    public float scissorPadding() {
        return this.scissorPadding;
    }

    public void setScissorPadding(float padding) {
        this.scissorPadding =
                Math.max(0.0F, padding);
    }

    public ScrollbarElement scrollbar() {
        return this.scrollbar;
    }

    public void setAutoPlaceScrollbar(boolean auto) {
        this.autoPlaceScrollbar = auto;

        if (auto) {
            this.placeDefaultScrollbar();
        }
    }

    public void setMinimumScrollbarHandleSize(float size) {
        this.minimumScrollbarHandleSize = Math.max(1.0F, size);
    }

    public float minimumScrollbarHandleSize() {
        return this.minimumScrollbarHandleSize;
    }

    public void setFixedScrollbarHandleSize(float size) {
        this.fixedScrollbarHandleSize = Math.max(1.0F, size);
    }

    public void clearFixedScrollbarHandleSize() {
        this.fixedScrollbarHandleSize = null;
    }

    public boolean hasScrollableOverflow() {
        this.ensureLayoutCurrent();
        return this.orientation == Orientation.VERTICAL
                ? this.maxScrollY() > 0.0D
                : this.maxScrollX() > 0.0D;
    }

    public void setRenderer(IWidgetRenderer renderer) {
        this.renderer = renderer;
    }

    public double scrollX() {
        return this.scrollX;
    }

    public double scrollY() {
        return this.scrollY;
    }

    public double targetScrollX() {
        return this.targetScrollX;
    }

    public double targetScrollY() {
        return this.targetScrollY;
    }

    public double totalContentWidth() {
        this.ensureLayoutCurrent();
        return this.totalContentWidth;
    }

    public double totalContentHeight() {
        this.ensureLayoutCurrent();
        return this.totalContentHeight;
    }

    public void scrollTo(double x, double y) {
        this.ensureLayoutCurrent();

        this.targetScrollX =
                Mth.clamp(
                        x,
                        0.0D,
                        this.maxScrollX()
                );

        this.targetScrollY =
                Mth.clamp(
                        y,
                        0.0D,
                        this.maxScrollY()
                );
    }

    public void scrollToImmediately(double x, double y) {
        this.ensureLayoutCurrent();

        this.targetScrollX =
                Mth.clamp(
                        x,
                        0.0D,
                        this.maxScrollX()
                );

        this.targetScrollY =
                Mth.clamp(
                        y,
                        0.0D,
                        this.maxScrollY()
                );

        this.scrollX = this.targetScrollX;
        this.scrollY = this.targetScrollY;
        this.applyScrollToChildren();
        this.syncScrollbarFromScroll();
    }

    public void scrollBy(double x, double y) {
        this.scrollTo(
                this.targetScrollX + x,
                this.targetScrollY + y
        );
    }

    public void ensureChildVisible(
            JavaBaseWidget child
    ) {
        this.scrollChildIntoView(child);
    }

    private void scrollChildIntoView(
            JavaBaseWidget child
    ) {
        if (!this.children.contains(child)) {
            return;
        }

        this.ensureLayoutCurrent();

        double localLeft =
                child.baseX() - this.x();

        double localTop =
                child.baseY() - this.y();

        double localRight =
                localLeft + child.width();

        double localBottom =
                localTop + child.height();

        double newX = this.targetScrollX;
        double newY = this.targetScrollY;

        if (localLeft < newX) {
            newX = localLeft;
        } else if (localRight > newX + this.width()) {
            newX = localRight - this.width();
        }

        if (localTop < newY) {
            newY = localTop;
        } else if (localBottom > newY + this.height()) {
            newY = localBottom - this.height();
        }

        this.scrollTo(newX, newY);
    }

    public boolean hasActiveScrollTransition() {
        return Math.abs(this.scrollX - this.targetScrollX) >= 0.05D
                || Math.abs(this.scrollY - this.targetScrollY) >= 0.05D;
    }

    @Override
    protected void extractWidgetRenderState(
            GuiGraphicsExtractor guiGraphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        if (!this.visible()) {
            return;
        }

        this.ensureLayoutCurrent();
        this.updateSmoothScroll();
        this.applyScrollToChildren();

        if (this.renderer != null) {
            this.renderer.render(
                    guiGraphics,
                    this,
                    mouseX,
                    mouseY,
                    partialTick
            );
        }

        ((GuiGraphicsExtractorBridge)(Object)guiGraphics)
                .javaReforged$enablePreciseScissor(
                        this.x() - this.scissorPadding,
                        this.y() - this.scissorPadding,
                        this.x() + this.width() + this.scissorPadding,
                        this.y() + this.height() + this.scissorPadding
                );

        for (JavaBaseWidget child : this.children) {
            if (this.isChildVisibleInsidePanel(child)) {
                child.extractRenderState(
                        guiGraphics,
                        mouseX,
                        mouseY,
                        partialTick
                );
            }
        }

        guiGraphics.disableScissor();

        this.syncScrollbarFromScroll();

        this.scrollbar.extractRenderState(
                guiGraphics,
                mouseX,
                mouseY,
                partialTick
        );
    }

    private void ensureLayoutCurrent() {
        boolean boundsChanged =
                Float.compare(this.lastLayoutX, this.x()) != 0
                        || Float.compare(this.lastLayoutY, this.y()) != 0
                        || Float.compare(
                        this.lastLayoutWidth,
                        this.width()
                ) != 0
                        || Float.compare(
                        this.lastLayoutHeight,
                        this.height()
                ) != 0;

        if (boundsChanged) {
            this.layoutDirty = true;

            if (this.autoPlaceScrollbar) {
                this.placeDefaultScrollbar();
            }
        }

        if (this.layoutDirty) {
            this.recalculateLayout();
        }
    }

    protected final void prepareInputLayout() {
        this.ensureLayoutCurrent();
        this.applyScrollToChildren();
    }

    private void recalculateLayout() {
        float localX = this.padding;
        float localY = this.padding;

        float maxRight = this.padding;
        float maxBottom = this.padding;

        int column = 0;
        float rowHeight = 0.0F;

        for (JavaBaseWidget child : this.children) {
            child.setBasePosition(
                    this.x() + localX,
                    this.y() + localY
            );

            maxRight =
                    Math.max(
                            maxRight,
                            localX + child.width()
                    );

            maxBottom =
                    Math.max(
                            maxBottom,
                            localY + child.height()
                    );

            if (this.orientation == Orientation.VERTICAL) {
                rowHeight =
                        Math.max(
                                rowHeight,
                                child.height()
                        );

                localX +=
                        child.width() + this.spacing;

                column++;

                if (column >= this.columns) {
                    column = 0;
                    localX = this.padding;

                    localY +=
                            rowHeight + this.spacing;

                    rowHeight = 0.0F;
                }
            } else {
                localX +=
                        child.width() + this.spacing;
            }
        }

        this.totalContentWidth =
                maxRight + this.padding;

        this.totalContentHeight =
                maxBottom + this.padding;

        this.targetScrollX =
                Mth.clamp(
                        this.targetScrollX,
                        0.0D,
                        this.maxScrollX()
                );

        this.targetScrollY =
                Mth.clamp(
                        this.targetScrollY,
                        0.0D,
                        this.maxScrollY()
                );

        this.scrollX =
                Mth.clamp(
                        this.scrollX,
                        0.0D,
                        this.maxScrollX()
                );

        this.scrollY =
                Mth.clamp(
                        this.scrollY,
                        0.0D,
                        this.maxScrollY()
                );

        this.lastLayoutX = this.x();
        this.lastLayoutY = this.y();
        this.lastLayoutWidth = this.width();
        this.lastLayoutHeight = this.height();

        this.layoutDirty = false;

        this.applyScrollToChildren();
    }

    private void updateSmoothScroll() {
        this.scrollX =
                Mth.lerp(
                        0.2D,
                        this.scrollX,
                        this.targetScrollX
                );

        this.scrollY =
                Mth.lerp(
                        0.2D,
                        this.scrollY,
                        this.targetScrollY
                );

        if (Math.abs(
                this.scrollX - this.targetScrollX
        ) < 0.05D) {
            this.scrollX = this.targetScrollX;
        }

        if (Math.abs(
                this.scrollY - this.targetScrollY
        ) < 0.05D) {
            this.scrollY = this.targetScrollY;
        }
    }

    private void applyScrollToChildren() {
        for (JavaBaseWidget child : this.children) {
            child.setPosition(
                    child.baseX() - (float)this.scrollX,
                    child.baseY() - (float)this.scrollY
            );
        }
    }

    private boolean isChildVisibleInsidePanel(
            JavaBaseWidget child
    ) {
        return child.x() + child.width() > this.x()
                && child.y() + child.height() > this.y()
                && child.x() < this.x() + this.width()
                && child.y() < this.y() + this.height();
    }

    private double maxScrollX() {
        return Math.max(
                0.0D,
                this.totalContentWidth - this.width()
        );
    }

    private double maxScrollY() {
        return Math.max(
                0.0D,
                this.totalContentHeight - this.height()
        );
    }

    private void applyScrollbarPosition(
            double position
    ) {
        this.ensureLayoutCurrent();

        if (this.orientation == Orientation.VERTICAL) {
            this.targetScrollY =
                    position * this.maxScrollY();
        } else {
            this.targetScrollX =
                    position * this.maxScrollX();
        }
    }

    private void syncScrollbarFromScroll() {
        double maxScroll;
        double current;
        double visibleAmount;

        if (this.orientation == Orientation.VERTICAL) {
            maxScroll = this.maxScrollY();
            current = this.scrollY;

            visibleAmount =
                    this.totalContentHeight <= 0.0D
                            ? 1.0D
                            : this.height()
                            / this.totalContentHeight;
        } else {
            maxScroll = this.maxScrollX();
            current = this.scrollX;

            visibleAmount =
                    this.totalContentWidth <= 0.0D
                            ? 1.0D
                            : this.width()
                            / this.totalContentWidth;
        }

        visibleAmount =
                Mth.clamp(
                        visibleAmount,
                        0.0D,
                        1.0D
                );

        float handleSize;
        if (this.fixedScrollbarHandleSize != null) {
            handleSize = Math.min(
                    this.fixedScrollbarHandleSize,
                    this.scrollbar.trackLength()
            );
        } else {
            handleSize =
                    (float)(
                            this.scrollbar.trackLength()
                                    * visibleAmount
                    );

            handleSize =
                    Math.max(
                            this.minimumScrollbarHandleSize,
                            Math.min(
                                    handleSize,
                                    this.scrollbar.trackLength()
                            )
                    );
        }

        this.scrollbar.setHandleSize(handleSize);

        this.scrollbar.setPositionSilently(
                maxScroll <= 0.0D
                        ? 0.0D
                        : current / maxScroll
        );
    }

    private void placeDefaultScrollbar() {
        if (this.orientation == Orientation.VERTICAL) {
            this.scrollbar.setBounds(
                    this.x() + this.width() - 8.0F,
                    this.y(),
                    8.0F,
                    this.height()
            );
        } else {
            this.scrollbar.setBounds(
                    this.x(),
                    this.y() + this.height() - 8.0F,
                    this.width(),
                    8.0F
            );
        }

        this.scrollbar.resetTrack();
    }

    private void markLayoutDirty() {
        this.layoutDirty = true;
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double scrollX,
            double scrollY
    ) {
        this.prepareInputLayout();

        if (!this.visible()
                || !this.active
                || !this.isMouseOver(mouseX, mouseY)) {
            return false;
        }

        if (this.orientation == Orientation.VERTICAL) {
            this.scrollBy(
                    0.0D,
                    -scrollY * 20.0D
            );
        } else {
            double amount =
                    scrollX != 0.0D
                            ? scrollX
                            : scrollY;

            this.scrollBy(
                    -amount * 20.0D,
                    0.0D
            );
        }

        return true;
    }

    @Override
    public boolean mouseClicked(
            MouseButtonEvent event,
            boolean doubleClick
    ) {
        this.prepareInputLayout();

        if (this.scrollbar.mouseClicked(
                event,
                doubleClick
        )) {
            return true;
        }

        if (!this.isMouseOver(event.x(), event.y())) {
            return false;
        }

        for (int i = this.children.size() - 1;
             i >= 0;
             i--) {

            JavaBaseWidget child =
                    this.children.get(i);

            if (child.mouseClicked(
                    event,
                    doubleClick
            )) {

                if (this.children.contains(child)) {
                    this.setFocused(child);
                }
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (this.scrollbar.isDraggingHandle()) {
            return this.scrollbar.mouseReleased(event);
        }

        GuiEventListener focused = this.getFocused();
        if (focused != null) {
            return focused.mouseReleased(event);
        }

        return false;
    }

    @Override
    public boolean mouseDragged(
            MouseButtonEvent event,
            double dragX,
            double dragY
    ) {
        if (this.scrollbar.isDraggingHandle()) {
            return this.scrollbar.mouseDragged(
                    event,
                    dragX,
                    dragY
            );
        }

        GuiEventListener focused = this.getFocused();
        if (focused != null) {
            return focused.mouseDragged(
                    event,
                    dragX,
                    dragY
            );
        }

        return false;
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return Collections.unmodifiableList(
                this.children
        );
    }

    @Override
    public GuiEventListener getFocused() {
        this.clearInvalidFocusedChild();
        return this.focused;
    }

    @Override
    public void setFocused(
            GuiEventListener focused
    ) {
        if (focused != null && !this.isFocusableChild(focused)) {
            return;
        }

        if (this.focused == focused) {
            return;
        }

        if (this.focused != null) {
            this.focused.setFocused(false);
        }

        this.focused = focused;

        if (this.focused != null) {
            this.focused.setFocused(true);

            if (this.focused instanceof JavaBaseWidget widget) {
                this.ensureChildVisible(widget);
            }
        }
    }

    @Override
    public void setFocused(boolean focused) {
        this.clearInvalidFocusedChild();

        if (focused && !this.hasFocusableChild()) {
            super.setFocused(false);
            return;
        }

        super.setFocused(focused);

        if (this.focused != null) {
            this.focused.setFocused(focused);

            if (focused && this.focused instanceof JavaBaseWidget widget) {
                this.ensureChildVisible(widget);
            }
        }
    }

    @Override
    public boolean isDragging() {
        return this.dragging;
    }

    @Override
    public void setDragging(boolean dragging) {
        this.dragging = dragging;
    }

    @Override
    public ComponentPath nextFocusPath(
            FocusNavigationEvent event
    ) {
        this.clearInvalidFocusedChild();

        if (!this.hasFocusableChild()) {
            return null;
        }

        if (event instanceof FocusNavigationEvent.InitialFocus) {
            ComponentPath initialChildPath = this.initialFocusableChildPath();
            return initialChildPath == null
                    ? null
                    : ComponentPath.path(this, initialChildPath);
        }

        return ContainerEventHandler.super
                .nextFocusPath(event);
    }

    private boolean isFocusableChild(GuiEventListener child) {
        if (!(child instanceof JavaBaseWidget widget)
                || !this.children.contains(widget)
                || !widget.isActive()
                || !widget.visible()) {
            return false;
        }

        return widget.isFocused()
                || widget.nextFocusPath(new FocusNavigationEvent.InitialFocus()) != null;
    }

    private void clearInvalidFocusedChild() {
        if (this.focused != null && !this.isFocusableChild(this.focused)) {
            this.setFocused((GuiEventListener)null);
        }
    }

    private ComponentPath initialFocusableChildPath() {
        for (JavaBaseWidget child : this.children) {
            if (!this.isFocusableChild(child)) {
                continue;
            }

            ComponentPath childPath = child.nextFocusPath(new FocusNavigationEvent.InitialFocus());
            return childPath != null
                    ? childPath
                    : ComponentPath.leaf(child);
        }
        return null;
    }

    public static final class Builder {

        private final ScrollPanelElement instance;

        public Builder(
                int x,
                int y,
                int width,
                int height
        ) {
            this.instance =
                    new ScrollPanelElement(
                            x,
                            y,
                            width,
                            height
                    );
        }

        public Builder withOrientation(
                Orientation orientation
        ) {
            this.instance.setOrientation(orientation);
            return this;
        }

        public Builder withSpacing(float spacing) {
            this.instance.setSpacing(spacing);
            return this;
        }

        public Builder withPadding(float padding) {
            this.instance.setPadding(padding);
            return this;
        }

        public Builder withColumns(int columns) {
            this.instance.setColumns(columns);
            return this;
        }

        public Builder withScissorPadding(
                float padding
        ) {
            this.instance.setScissorPadding(padding);
            return this;
        }

        public Builder withRenderer(
                IWidgetRenderer renderer
        ) {
            this.instance.setRenderer(renderer);
            return this;
        }

        public ScrollPanelElement build() {
            return this.instance;
        }
    }
}
