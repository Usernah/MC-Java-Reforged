package net.jr.client.components.widgets;

import net.jr.client.components.elements.VisualElementsInterface;
import net.jr.client.components.elements.VisualState;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class GridLayout implements VisualElementsInterface {
    private static final int DEBUG_BACKGROUND_COLOR = 0xFF00FF00;

    public enum HorizontalAlignment {
        START,
        CENTER,
        END
    }

    private final VisualState visualState;
    private final List<VisualElementsInterface> children = new ArrayList<>();

    private int padding = 3;
    private int columns = 1;
    private int gap = 0;
    private HorizontalAlignment horizontalAlignment = HorizontalAlignment.START;
    private boolean debugBackground = true;

    public GridLayout(float x, float y, float width, float height) {
        this.visualState = new VisualState(x, y, width, height);
    }

    @Override
    public VisualState visualState() {
        return this.visualState;
    }

    public void addChild(VisualElementsInterface child) {
        if (child != null) {
            this.children.add(child);
        }
    }

    public void removeChild(VisualElementsInterface child) {
        this.children.remove(child);
    }

    public void clearChildren() {
        this.children.clear();
    }

    public List<VisualElementsInterface> children() {
        return Collections.unmodifiableList(this.children);
    }

    public int padding() {
        return this.padding;
    }

    public void setPadding(int padding) {
        this.padding = Math.max(0, padding);
    }

    public void setGridPadding(int padding) {
        this.setPadding(padding);
    }

    public int columns() {
        return this.columns;
    }

    public void setColumns(int columns) {
        this.columns = Math.max(1, columns);
    }

    public int gap() {
        return this.gap;
    }

    public void setGap(int gap) {
        this.gap = Math.max(0, gap);
    }

    public HorizontalAlignment horizontalAlignment() {
        return this.horizontalAlignment;
    }

    public void setHorizontalAlignment(HorizontalAlignment horizontalAlignment) {
        this.horizontalAlignment = horizontalAlignment == null ? HorizontalAlignment.START : horizontalAlignment;
    }

    public boolean debugBackground() {
        return this.debugBackground;
    }

    public void setDebugBackground(boolean debugBackground) {
        this.debugBackground = debugBackground;
    }

    public void layoutChildren() {
        if (this.children.isEmpty()) {
            return;
        }

        float cellWidth = this.maxChildWidth();
        float contentX = this.x() + this.padding;
        float contentY = this.y() + this.padding;
        float[] columnY = new float[this.columns];
        for (int column = 0; column < columnY.length; column++) {
            columnY[column] = contentY;
        }

        for (int index = 0; index < this.children.size(); index++) {
            VisualElementsInterface child = this.children.get(index);
            int column = index % this.columns;

            float cellX = contentX + column * (cellWidth + this.gap);
            float childX = this.alignedX(cellX, cellWidth, child.width());
            float childY = columnY[column];

            child.setPosition(childX, childY);
            columnY[column] += child.height() + this.gap;
        }
    }

    public void draw(GuiGraphicsExtractor guiGraphics) {
        this.layoutChildren();
        this.drawDebugBackground(guiGraphics);
    }

    public void drawDebugBackground(GuiGraphicsExtractor guiGraphics) {
        if (!this.visible() || !this.debugBackground || guiGraphics == null) {
            return;
        }

        int x = Math.round(this.x());
        int y = Math.round(this.y());
        int width = Math.round(this.width());
        int height = Math.round(this.height());
        guiGraphics.fill(x, y, x + width, y + height, DEBUG_BACKGROUND_COLOR);
    }

    private float maxChildWidth() {
        float maxWidth = 0.0F;
        for (VisualElementsInterface child : this.children) {
            if (child.visible()) {
                maxWidth = Math.max(maxWidth, child.width());
            }
        }
        return maxWidth;
    }

    private float alignedX(float cellX, float cellWidth, float childWidth) {
        return switch (this.horizontalAlignment) {
            case START -> cellX;
            case CENTER -> cellX + (cellWidth - childWidth) / 2.0F;
            case END -> cellX + cellWidth - childWidth;
        };
    }
}
