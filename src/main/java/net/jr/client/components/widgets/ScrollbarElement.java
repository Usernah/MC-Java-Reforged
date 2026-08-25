package net.jr.client.components.widgets;

import net.jr.client.components.interfaces.IWidgetRenderer;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.DoubleConsumer;

public class ScrollbarElement extends JavaBaseWidget {

    public enum Orientation {
        HORIZONTAL,
        VERTICAL
    }

    private Orientation orientation = Orientation.VERTICAL;

    private float trackOffsetX;
    private float trackOffsetY;
    private float trackLength;
    private float trackThickness = 2.0F;
    private boolean customTrack;

    private float handleSize = 10.0F;
    private double position;

    private boolean draggingHandle;
    private double dragOffset;

    private IWidgetRenderer renderer;
    private DoubleConsumer onPositionChange = ignored -> {};

    public ScrollbarElement(int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty());
        this.resetTrack();
    }

    public Orientation orientation() {
        return this.orientation;
    }

    public void setOrientation(Orientation orientation) {
        this.orientation = orientation == null
                ? Orientation.VERTICAL
                : orientation;

        if (!this.customTrack) {
            this.resetTrack();
        }
    }

    public void setTrack(
            float offsetX,
            float offsetY,
            float length,
            float thickness
    ) {
        this.customTrack = true;
        this.trackOffsetX = offsetX;
        this.trackOffsetY = offsetY;
        this.trackLength = Math.max(1.0F, length);
        this.trackThickness = Math.max(1.0F, thickness);
    }

    public void resetTrack() {
        this.customTrack = false;

        if (this.orientation == Orientation.VERTICAL) {
            this.trackThickness = 2.0F;
            this.trackOffsetX = (this.width() - this.trackThickness) / 2.0F;
            this.trackOffsetY = 0.0F;
            this.trackLength = this.height();
        } else {
            this.trackThickness = 2.0F;
            this.trackOffsetX = 0.0F;
            this.trackOffsetY = (this.height() - this.trackThickness) / 2.0F;
            this.trackLength = this.width();
        }
    }

    public float trackX() {
        return this.x() + this.trackOffsetX;
    }

    public float trackY() {
        return this.y() + this.trackOffsetY;
    }

    public float trackLength() {
        return this.trackLength;
    }

    public float trackThickness() {
        return this.trackThickness;
    }

    public float trackWidth() {
        return this.orientation == Orientation.VERTICAL
                ? this.trackThickness
                : this.trackLength;
    }

    public float trackHeight() {
        return this.orientation == Orientation.VERTICAL
                ? this.trackLength
                : this.trackThickness;
    }

    public void setHandleSize(float size) {
        this.handleSize = Math.max(1.0F, size);
    }

    public float handleSize() {
        return Math.min(this.handleSize, this.trackLength);
    }

    public float handleX() {
        if (this.orientation == Orientation.VERTICAL) {
            return this.x();
        }

        return this.trackX()
                + this.availableHandleTravel() * (float)this.position;
    }

    public float handleY() {
        if (this.orientation == Orientation.HORIZONTAL) {
            return this.y();
        }

        return this.trackY()
                + this.availableHandleTravel() * (float)this.position;
    }

    public float handleWidth() {
        return this.orientation == Orientation.VERTICAL
                ? this.width()
                : this.handleSize();
    }

    public float handleHeight() {
        return this.orientation == Orientation.VERTICAL
                ? this.handleSize()
                : this.height();
    }

    public double position() {
        return this.position;
    }

    public void setPosition(double position) {
        this.setPositionInternal(position, true);
    }

    void setPositionSilently(double position) {
        this.setPositionInternal(position, false);
    }

    private void setPositionInternal(double position, boolean notify) {
        double resolved = Mth.clamp(position, 0.0D, 1.0D);

        if (Double.compare(this.position, resolved) == 0) {
            return;
        }

        this.position = resolved;

        if (notify) {
            this.onPositionChange.accept(this.position);
        }
    }

    public void onPositionChange(DoubleConsumer action) {
        this.onPositionChange = action == null
                ? ignored -> {}
                : action;
    }

    public boolean isDraggingHandle() {
        return this.draggingHandle;
    }

    public void setRenderer(IWidgetRenderer renderer) {
        this.renderer = renderer;
    }

    public IWidgetRenderer renderer() {
        return this.renderer;
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

        if (this.renderer != null) {
            this.renderer.render(
                    guiGraphics,
                    this,
                    mouseX,
                    mouseY,
                    partialTick
            );
        }
    }

    @Override
    public boolean mouseClicked(
            MouseButtonEvent event,
            boolean doubleClick
    ) {
        if (!this.isActive()
                || !this.visible()
                || !this.isValidClickButton(event.buttonInfo())
                || !this.isMouseOver(event.x(), event.y())) {
            return false;
        }

        this.playDownSound(
                net.minecraft.client.Minecraft.getInstance().getSoundManager()
        );

        this.setPressed(true);
        this.draggingHandle = true;

        double pointerAxis = this.pointerAxis(event);

        if (this.isPointerInsideHandle(event.x(), event.y())) {
            this.dragOffset =
                    pointerAxis - this.handleAxisPosition();
        } else {
            this.dragOffset = this.handleSize() / 2.0D;
            this.updatePositionFromPointer(pointerAxis);
        }

        return true;
    }

    @Override
    public boolean mouseDragged(
            MouseButtonEvent event,
            double dragX,
            double dragY
    ) {
        if (!this.draggingHandle) {
            return false;
        }

        this.updatePositionFromPointer(this.pointerAxis(event));
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (!this.draggingHandle) {
            return false;
        }

        this.draggingHandle = false;
        this.dragOffset = 0.0D;

        boolean handled = super.mouseReleased(event);
        return handled || true;
    }

    @Override
    public void cancelInteraction() {
        super.cancelInteraction();
        this.draggingHandle = false;
        this.dragOffset = 0.0D;
    }

    private void updatePositionFromPointer(double pointerAxis) {
        double available = this.availableHandleTravel();

        if (available <= 0.0D) {
            this.setPosition(0.0D);
            return;
        }

        double trackStart =
                this.orientation == Orientation.VERTICAL
                        ? this.trackY()
                        : this.trackX();

        double value =
                (pointerAxis - this.dragOffset - trackStart)
                        / available;

        this.setPosition(value);
    }

    private float availableHandleTravel() {
        return Math.max(
                0.0F,
                this.trackLength - this.handleSize()
        );
    }

    private double pointerAxis(MouseButtonEvent event) {
        return this.orientation == Orientation.VERTICAL
                ? event.y()
                : event.x();
    }

    private double handleAxisPosition() {
        return this.orientation == Orientation.VERTICAL
                ? this.handleY()
                : this.handleX();
    }

    private boolean isPointerInsideHandle(double x, double y) {
        return x >= this.handleX()
                && y >= this.handleY()
                && x < this.handleX() + this.handleWidth()
                && y < this.handleY() + this.handleHeight();
    }

    @Override
    public boolean shouldTakeFocusAfterInteraction() {
        return false;
    }

    @Override
    public ComponentPath nextFocusPath(
            FocusNavigationEvent event
    ) {
        return null;
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(false);
    }

    public static final class Builder {

        private final ScrollbarElement instance;

        public Builder(int x, int y, int width, int height) {
            this.instance =
                    new ScrollbarElement(x, y, width, height);
        }

        public Builder withOrientation(
                Orientation orientation
        ) {
            this.instance.setOrientation(orientation);
            return this;
        }

        public Builder withTrack(
                float offsetX,
                float offsetY,
                float length,
                float thickness
        ) {
            this.instance.setTrack(
                    offsetX,
                    offsetY,
                    length,
                    thickness
            );
            return this;
        }

        public Builder withHandleSize(float size) {
            this.instance.setHandleSize(size);
            return this;
        }

        public Builder withPosition(double position) {
            this.instance.setPositionSilently(position);
            return this;
        }

        public Builder withRenderer(
                IWidgetRenderer renderer
        ) {
            this.instance.setRenderer(renderer);
            return this;
        }

        public Builder onPositionChange(
                DoubleConsumer action
        ) {
            this.instance.onPositionChange(action);
            return this;
        }

        public ScrollbarElement build() {
            return this.instance;
        }
    }
}
