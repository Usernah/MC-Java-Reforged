package net.jr.client.components.widgets;

import net.jr.client.components.elements.VisualElementsInterface;
import net.jr.client.components.elements.VisualState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;


public final class GridLayout implements VisualElementsInterface {
    private final VisualState visualState;
    private final List<VisualElementsInterface> children = new ArrayList<>();

    private int columns = 1;
    private int horizontalGap;
    private int verticalGap;
    private GridDirection direction = GridDirection.horizontal(
        GridDirection.VerticalAlignment.TOP
    );

    public GridLayout(float x, float y) {
        this.visualState = new VisualState(x, y, 0.0F, 0.0F);
    }

    @Override
    public VisualState visualState() {
        return this.visualState;
    }

    public void addChild(VisualElementsInterface child) {
        if (child != null) {
            child.setLayoutParent(this);
            this.children.add(child);
            this.layoutChildren();
        }
    }

    public void removeChild(VisualElementsInterface child) {
        if (this.children.remove(child)) {
            child.setLayoutParent(null);
            this.layoutChildren();
        }
    }

    public void clearChildren() {
        this.children.forEach(child -> child.setLayoutParent(null));
        this.children.clear();
        this.setSize(0.0F, 0.0F);
    }

    public List<VisualElementsInterface> children() {
        return Collections.unmodifiableList(this.children);
    }

    @Override
    public Object dslValue(String property) {
        return switch (property) {
            case "size", "count" -> this.children.size();
            case "visible_size", "visible_count" -> this.children.stream()
                .filter(VisualElementsInterface::visible)
                .count();
            case "columns" -> this.columns;
            case "horizontal_gap" -> this.horizontalGap;
            case "vertical_gap" -> this.verticalGap;
            default -> VisualElementsInterface.super.dslValue(property);
        };
    }

    public int columns() {
        return this.columns;
    }

    public void setColumns(int columns) {
        if (columns <= 0) {
            throw new IllegalArgumentException("Grid columns must be greater than zero");
        }
        this.columns = columns;
        this.layoutChildren();
    }

    public int horizontalGap() {
        return this.horizontalGap;
    }

    public void setHorizontalGap(int gap) {
        if (gap < 0) {
            throw new IllegalArgumentException("Grid horizontal gap cannot be negative");
        }
        this.horizontalGap = gap;
        this.layoutChildren();
    }

    public int verticalGap() {
        return this.verticalGap;
    }

    public void setVerticalGap(int gap) {
        if (gap < 0) {
            throw new IllegalArgumentException("Grid vertical gap cannot be negative");
        }
        this.verticalGap = gap;
        this.layoutChildren();
    }

    public GridDirection direction() {
        return this.direction;
    }

    public void setDirection(GridDirection direction) {
        this.direction = Objects.requireNonNull(direction, "direction");
        this.layoutChildren();
    }

    public void layoutChildren() {
        List<VisualElementsInterface> visibleChildren = this.children.stream()
            .filter(VisualElementsInterface::visible)
            .toList();
        if (visibleChildren.isEmpty()) {
            this.setSize(0.0F, 0.0F);
            return;
        }

        GridMetrics metrics = this.measure(visibleChildren);
        this.setSize(metrics.width(), metrics.height());
        for (int index = 0; index < visibleChildren.size(); index++) {
            VisualElementsInterface child = visibleChildren.get(index);
            Cell cell = metrics.cells().get(index);
            float childX = metrics.columnX()[cell.column()];
            float childY = metrics.rowY()[cell.row()];
            if (this.direction.axis() == GridDirection.Axis.HORIZONTAL) {
                childY += this.direction.crossOffset(
                    metrics.rowHeights()[cell.row()], child.height()
                );
            } else {
                childX += this.direction.crossOffset(
                    metrics.columnWidths()[cell.column()], child.width()
                );
            }
            child.setPosition(childX, childY);
        }
    }

    private GridMetrics measure(List<VisualElementsInterface> visibleChildren) {
        int usedColumns = Math.min(this.columns, visibleChildren.size());
        int rows = divideRoundUp(visibleChildren.size(), usedColumns);
        if (this.direction.axis() == GridDirection.Axis.VERTICAL) {
            usedColumns = divideRoundUp(visibleChildren.size(), rows);
        }

        float[] columnWidths = new float[usedColumns];
        float[] rowHeights = new float[rows];
        List<Cell> cells = new ArrayList<>(visibleChildren.size());
        for (int index = 0; index < visibleChildren.size(); index++) {
            Cell cell = this.cell(index, rows, usedColumns);
            VisualElementsInterface child = visibleChildren.get(index);
            cells.add(cell);
            columnWidths[cell.column()] = Math.max(columnWidths[cell.column()], child.width());
            rowHeights[cell.row()] = Math.max(rowHeights[cell.row()], child.height());
        }

        float[] columnX = offsets(columnWidths, this.horizontalGap);
        float[] rowY = offsets(rowHeights, this.verticalGap);
        float width = total(columnWidths, this.horizontalGap);
        float height = total(rowHeights, this.verticalGap);
        return new GridMetrics(
            columnWidths, rowHeights, columnX, rowY, List.copyOf(cells), width, height
        );
    }

    private Cell cell(int index, int rows, int usedColumns) {
        if (this.direction.axis() == GridDirection.Axis.HORIZONTAL) {
            return new Cell(index / usedColumns, index % usedColumns);
        }
        return new Cell(index % rows, index / rows);
    }

    private static int divideRoundUp(int value, int divisor) {
        return (value + divisor - 1) / divisor;
    }

    private static float[] offsets(float[] sizes, int gap) {
        float[] offsets = new float[sizes.length];
        for (int index = 1; index < sizes.length; index++) {
            offsets[index] = offsets[index - 1] + sizes[index - 1] + gap;
        }
        return offsets;
    }

    private static float total(float[] sizes, int gap) {
        float total = 0.0F;
        for (float size : sizes) {
            total += size;
        }
        return total + Math.max(0, sizes.length - 1) * gap;
    }

    private record Cell(int row, int column) {
    }

    private record GridMetrics(
        float[] columnWidths,
        float[] rowHeights,
        float[] columnX,
        float[] rowY,
        List<Cell> cells,
        float width,
        float height
    ) {
    }
}
