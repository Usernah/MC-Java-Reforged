package net.jr.client.components.elements;

import net.jr.api.client.ui.Viewport;

public interface VisualElementsInterface {
    VisualState visualState();

    default float x() {
        return this.visualState().x();
    }

    default float baseX() {
        return this.visualState().baseX();
    }

    default void setX(float x) {
        this.visualState().setX(x);
    }

    default void setBaseX(float x) {
        this.visualState().setBaseX(x);
    }

    default float y() {
        return this.visualState().y();
    }

    default float baseY() {
        return this.visualState().baseY();
    }

    default void setY(float y) {
        this.visualState().setY(y);
    }

    default void setBaseY(float y) {
        this.visualState().setBaseY(y);
    }

    default float width() {
        return this.visualState().width();
    }

    default float baseWidth() {
        return this.visualState().baseWidth();
    }

    default void setWidth(float width) {
        this.visualState().setWidth(width);
    }

    default void setBaseWidth(float width) {
        this.visualState().setBaseWidth(width);
    }

    default float height() {
        return this.visualState().height();
    }

    default float baseHeight() {
        return this.visualState().baseHeight();
    }

    default void setHeight(float height) {
        this.visualState().setHeight(height);
    }

    default void setBaseHeight(float height) {
        this.visualState().setBaseHeight(height);
    }

    default float rotation() {
        return this.visualState().rotation();
    }

    default float baseRotation() {
        return this.visualState().baseRotation();
    }

    default void setRotation(float rotation) {
        this.visualState().setRotation(rotation);
    }

    default void setBaseRotation(float rotation) {
        this.visualState().setBaseRotation(rotation);
    }

    default float alpha() {
        return this.visualState().alpha();
    }

    default float baseAlpha() {
        return this.visualState().baseAlpha();
    }

    default void setAlpha(float alpha) {
        this.visualState().setAlpha(alpha);
    }

    default void setBaseAlpha(float alpha) {
        this.visualState().setBaseAlpha(alpha);
    }

    default boolean visible() {
        return this.visualState().visible();
    }

    default boolean baseVisible() {
        return this.visualState().baseVisible();
    }

    default void setVisible(boolean visible) {
        this.visualState().setVisible(visible);
    }

    default void setBaseVisible(boolean visible) {
        this.visualState().setBaseVisible(visible);
    }

    default VisualAnchor anchor() {
        return this.visualState().anchor();
    }

    default void setAnchor(VisualAnchor anchor) {
        this.visualState().setAnchor(anchor);
    }

    default VisualAnchor baseAnchor() {
        return this.visualState().baseAnchor();
    }

    default void setBaseAnchor(VisualAnchor anchor) {
        this.visualState().setBaseAnchor(anchor);
    }

    default VisualElementsInterface layoutParent() {
        return this.visualState().layoutParent();
    }

    default void setLayoutParent(VisualElementsInterface parent) {
        if (parent == this) {
            throw new IllegalArgumentException("A visual element cannot be its own layout parent");
        }
        this.visualState().setLayoutParent(parent);
    }

    default float resolvedX() {
        VisualAnchor anchor = this.anchor();
        VisualElementsInterface parent = this.layoutParent();
        if (!anchor.isExplicit()) {
            return parent == null ? this.x() : parent.resolvedX() + this.x();
        }
        return parent == null
            ? anchor.resolveX(this.x(), this.width(), 0.0F, Viewport.current().uiWidth())
            : anchor.resolveX(this.x(), this.width(), parent.resolvedX(), parent.width());
    }

    default float resolvedY() {
        VisualAnchor anchor = this.anchor();
        VisualElementsInterface parent = this.layoutParent();
        if (!anchor.isExplicit()) {
            return parent == null ? this.y() : parent.resolvedY() + this.y();
        }
        return parent == null
            ? anchor.resolveY(this.y(), this.height(), 0.0F, Viewport.current().uiHeight())
            : anchor.resolveY(this.y(), this.height(), parent.resolvedY(), parent.height());
    }

    default void setPosition(float x, float y) {
        this.visualState().setPosition(x, y);
    }

    default void setBasePosition(float x, float y) {
        this.visualState().setBasePosition(x, y);
    }

    default void setSize(float width, float height) {
        this.visualState().setSize(width, height);
    }

    default void setBaseSize(float width, float height) {
        this.visualState().setBaseSize(width, height);
    }

    default void setBounds(float x, float y, float width, float height) {
        this.visualState().setBounds(x, y, width, height);
    }

    default void setBaseBounds(float x, float y, float width, float height) {
        this.visualState().setBaseBounds(x, y, width, height);
    }

    default void resetVisualState() {
        this.visualState().resetVisualState();
    }

    default void captureBaseState() {
        this.visualState().captureBaseState();
    }


    default Object dslValue(String property) {
        return switch (property) {
            case "x" -> this.x();
            case "y" -> this.y();
            case "w", "width" -> this.width();
            case "h", "height" -> this.height();
            case "resolved_x" -> this.resolvedX();
            case "resolved_y" -> this.resolvedY();
            case "rotation" -> this.rotation();
            case "alpha" -> this.alpha();
            case "visible" -> this.visible();
            default -> throw new IllegalArgumentException(
                "Visual element does not expose '" + property + "'"
            );
        };
    }
}
