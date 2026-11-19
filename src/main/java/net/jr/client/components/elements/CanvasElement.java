package net.jr.client.components.elements;

/**
 * A non-rendering visual element that provides layout bounds to its DSL children.
 */
public final class CanvasElement implements VisualElementsInterface {
    private final VisualState visualState = new VisualState(0.0F, 0.0F, 0.0F, 0.0F);

    @Override
    public VisualState visualState() {
        return this.visualState;
    }
}
