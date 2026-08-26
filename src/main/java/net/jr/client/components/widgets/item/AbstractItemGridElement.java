package net.jr.client.components.widgets.item;

import net.jr.client.components.navigation.UiAction;
import net.jr.client.components.widgets.JavaBaseWidget;
import net.jr.client.components.widgets.ScrollPanelElement;
import net.jr.client.ui.cursor.CursorMagnetTarget;
import net.jr.client.ui.navigation.UiInputModeController;
import net.minecraft.client.gui.components.events.GuiEventListener;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractItemGridElement<T> extends ScrollPanelElement {
    private final int visibleRows;
    private final int columns;
    private int cellWidth;
    private int cellHeight;
    private final List<T> entries = new ArrayList<>();

    protected AbstractItemGridElement(int x, int y, int width, int height, int visibleRows, int columns) {
        super(x, y, width, height);
        this.visibleRows = Math.max(1, visibleRows);
        this.columns = Math.max(1, columns);
        this.cellWidth = Math.max(1, width / this.columns);
        this.cellHeight = Math.max(1, height / this.visibleRows);
        this.setOrientation(Orientation.VERTICAL);
        this.setColumns(this.columns);
        this.setPadding(0.0F);
        this.setSpacing(0.0F);
    }

    public final void setCellSize(int width, int height) {
        this.cellWidth = Math.max(1, width);
        this.cellHeight = Math.max(1, height);
        this.rebuildCells();
    }

    public final void setEntries(List<T> values) {
        this.replaceEntries(values, -1);
    }

    protected final void setEntriesAndFocus(List<T> values, int focusIndex) {
        this.replaceEntries(values, focusIndex);
    }

    private void replaceEntries(List<T> values, int focusIndex) {
        this.entries.clear();
        if (values != null) this.entries.addAll(values);
        this.rebuildCells(focusIndex);
    }

    public final List<T> entries() { return List.copyOf(this.entries); }
    public final int visibleRows() { return this.visibleRows; }
    public final int gridColumns() { return this.columns; }
    public final int cellWidth() { return this.cellWidth; }
    public final int cellHeight() { return this.cellHeight; }

    public final CursorMagnetTarget cursorMagnetTargetAt(double mouseX, double mouseY) {
        JavaBaseWidget cell = this.hoveredCellAt(mouseX, mouseY);
        return cell == null ? null : new CursorMagnetTarget(
                cell.getX() + cell.getWidth() / 2.0D,
                cell.getY() + cell.getHeight() / 2.0D
        );
    }

    protected final JavaBaseWidget hoveredCellAt(double mouseX, double mouseY) {
        this.prepareInputLayout();
        if (!this.visible() || !this.active || !this.isMouseOver(mouseX, mouseY)) {
            return null;
        }

        for (JavaBaseWidget cell : this.childWidgets()) {
            if (cell.visible() && cell.isActive() && cell.isMouseOver(mouseX, mouseY)) {
                return cell;
            }
        }
        return null;
    }

    protected final JavaBaseWidget previewedCellAt(
            double mouseX,
            double mouseY,
            boolean includeFocused
    ) {
        JavaBaseWidget hovered = this.hoveredCellAt(mouseX, mouseY);
        if (hovered != null) {
            return hovered;
        }
        if (includeFocused && this.getFocused() instanceof JavaBaseWidget focused
                && this.childWidgets().contains(focused)
                && focused.visible()
                && focused.isActive()) {
            return focused;
        }
        return null;
    }

    protected abstract JavaBaseWidget createCell(T entry, int index, int width, int height);

    public final boolean focusFirstEntry() {
        return this.focusEntry(this.firstFocusableEntryIndex());
    }

    public final boolean focusLastEntry() {
        return this.focusEntry(this.lastFocusableEntryIndex());
    }

    public final void clearEntryFocus() {
        this.setFocused((GuiEventListener) null);
    }

    @Override
    public boolean handleUiAction(UiAction action) {
        if (!this.active || !this.visible()) {
            return false;
        }

        List<JavaBaseWidget> cells = this.childWidgets();
        if (cells.isEmpty()) {
            return false;
        }

        GuiEventListener focused = this.getFocused();
        int currentIndex = cells.indexOf(focused);
        if (currentIndex < 0) {
            return action.isDirectional() && this.focusFirstEntry();
        }

        if (action == UiAction.ACCEPT || action == UiAction.ALTERNATE) {
            return cells.get(currentIndex).handleUiAction(action);
        }
        if (!action.isDirectional()) {
            return false;
        }

        return this.focusEntry(this.nextFocusableEntryIndex(currentIndex, action));
    }

    public final int focusedEntryIndex() {
        return this.childWidgets().indexOf(this.getFocused());
    }

    public final boolean focusEntry(int index) {
        if (index < 0 || index >= this.childWidgets().size()) return false;
        JavaBaseWidget cell = this.childWidgets().get(index);
        if (!cell.isActive() || !cell.visible()) return false;
        this.setFocused(cell);

        if (UiInputModeController.isFocusNavigationActive()) {
            this.ensureChildVisible(cell);
        }
        return true;
    }

    private int nextFocusableEntryIndex(int currentIndex, UiAction action) {
        List<JavaBaseWidget> cells = this.childWidgets();
        int currentColumn = currentIndex % this.columns;
        int currentRow = currentIndex / this.columns;

        return switch (action) {
            case MOVE_LEFT -> this.nextFocusableInRow(cells, currentRow, currentColumn - 1, -1);
            case MOVE_RIGHT -> this.nextFocusableInRow(cells, currentRow, currentColumn + 1, 1);
            case MOVE_UP -> this.closestFocusableInAdjacentRow(cells, currentRow - 1, -1, currentColumn);
            case MOVE_DOWN -> this.closestFocusableInAdjacentRow(cells, currentRow + 1, 1, currentColumn);
            default -> -1;
        };
    }

    private int nextFocusableInRow(
            List<JavaBaseWidget> cells,
            int row,
            int column,
            int step
    ) {
        int rowStart = row * this.columns;
        int rowEnd = Math.min(rowStart + this.columns, cells.size());
        for (int currentColumn = column;
             currentColumn >= 0 && rowStart + currentColumn < rowEnd;
             currentColumn += step) {
            int index = rowStart + currentColumn;
            if (this.isFocusableCell(cells.get(index))) {
                return index;
            }
        }
        return -1;
    }

    private int closestFocusableInAdjacentRow(
            List<JavaBaseWidget> cells,
            int row,
            int rowStep,
            int preferredColumn
    ) {
        int rowCount = (cells.size() + this.columns - 1) / this.columns;
        for (int candidateRow = row;
             candidateRow >= 0 && candidateRow < rowCount;
             candidateRow += rowStep) {
            int rowStart = candidateRow * this.columns;
            int rowEnd = Math.min(rowStart + this.columns, cells.size());
            int closestIndex = -1;
            int closestDistance = Integer.MAX_VALUE;
            for (int index = rowStart; index < rowEnd; index++) {
                if (!this.isFocusableCell(cells.get(index))) {
                    continue;
                }
                int distance = Math.abs(index - rowStart - preferredColumn);
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestIndex = index;
                }
            }
            if (closestIndex >= 0) {
                return closestIndex;
            }
        }
        return -1;
    }

    private int firstFocusableEntryIndex() {
        List<JavaBaseWidget> cells = this.childWidgets();
        for (int index = 0; index < cells.size(); index++) {
            if (this.isFocusableCell(cells.get(index))) {
                return index;
            }
        }
        return -1;
    }

    private int lastFocusableEntryIndex() {
        List<JavaBaseWidget> cells = this.childWidgets();
        for (int index = cells.size() - 1; index >= 0; index--) {
            if (this.isFocusableCell(cells.get(index))) {
                return index;
            }
        }
        return -1;
    }

    private boolean isFocusableCell(JavaBaseWidget cell) {
        return cell.isActive() && cell.visible();
    }

    private void rebuildCells() {
        this.rebuildCells(-1);
    }

    private void rebuildCells(int requestedFocusIndex) {
        double previousScrollX = this.targetScrollX();
        double previousScrollY = this.targetScrollY();
        int previousFocusIndex = this.focusedEntryIndex();

        this.clearChildren();
        for (int i = 0; i < this.entries.size(); i++) {
            this.addChild(this.createCell(this.entries.get(i), i, this.cellWidth, this.cellHeight));
        }

        this.scrollToImmediately(previousScrollX, previousScrollY);
        int restoredFocusIndex = requestedFocusIndex >= 0
                ? requestedFocusIndex
                : previousFocusIndex;
        if (restoredFocusIndex >= 0 && restoredFocusIndex < this.childWidgets().size()) {
            this.focusEntry(restoredFocusIndex);
        }
    }
}
