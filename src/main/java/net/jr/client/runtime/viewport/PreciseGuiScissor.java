package net.jr.client.runtime.viewport;

public record PreciseGuiScissor(
        double left,
        double top,
        double right,
        double bottom
) {
    public PreciseGuiScissor intersection(PreciseGuiScissor other) {
        double newLeft = Math.max(this.left, other.left);
        double newTop = Math.max(this.top, other.top);
        double newRight = Math.min(this.right, other.right);
        double newBottom = Math.min(this.bottom, other.bottom);

        return new PreciseGuiScissor(
                newLeft,
                newTop,
                Math.max(newLeft, newRight),
                Math.max(newTop, newBottom)
        );
    }
}