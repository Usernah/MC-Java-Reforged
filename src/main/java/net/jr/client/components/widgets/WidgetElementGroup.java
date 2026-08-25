package net.jr.client.components.widgets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public final class WidgetElementGroup {

    private final List<JavaBaseWidget> widgets = new ArrayList<>();

    private JavaBaseWidget selected;
    private Consumer<JavaBaseWidget> onSelectionChange = ignored -> {};

    public void add(JavaBaseWidget widget) {
        if (widget == null || this.widgets.contains(widget)) {
            return;
        }

        this.widgets.add(widget);
    }

    public void remove(JavaBaseWidget widget) {
        if (!this.widgets.remove(widget)) {
            return;
        }

        if (this.selected == widget) {
            this.selected = null;
            this.onSelectionChange.accept(null);
        }
    }

    public void clear() {
        this.widgets.clear();

        if (this.selected != null) {
            this.selected = null;
            this.onSelectionChange.accept(null);
        }
    }

    public boolean contains(JavaBaseWidget widget) {
        return this.widgets.contains(widget);
    }

    public List<JavaBaseWidget> widgets() {
        return Collections.unmodifiableList(this.widgets);
    }

    public void select(JavaBaseWidget widget) {
        if (widget == null || !this.widgets.contains(widget)) {
            return;
        }

        if (this.selected == widget) {
            return;
        }

        this.selected = widget;
        this.onSelectionChange.accept(widget);
    }

    public void clearSelection() {
        if (this.selected == null) {
            return;
        }

        this.selected = null;
        this.onSelectionChange.accept(null);
    }

    public JavaBaseWidget selected() {
        return this.selected;
    }

    public void onSelectionChange(Consumer<JavaBaseWidget> action) {
        this.onSelectionChange = action == null ? ignored -> {} : action;
    }

    public void setActive(boolean active) {
        this.forEach(widget -> widget.setActive(active));
    }

    public void setVisible(boolean visible) {
        this.forEach(widget -> widget.setVisible(visible));
    }

    public void setAlpha(float alpha) {
        this.forEach(widget -> widget.setAlpha(alpha));
    }

    public void captureBaseState() {
        this.forEach(JavaBaseWidget::captureBaseState);
    }

    public void resetVisualState() {
        this.forEach(JavaBaseWidget::resetVisualState);
    }

    public void forEach(Consumer<JavaBaseWidget> action) {
        if (action == null) {
            return;
        }

        for (JavaBaseWidget widget : this.widgets) {
            action.accept(widget);
        }
    }
}