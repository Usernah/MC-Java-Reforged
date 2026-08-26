package net.jr.client.runtime.render.target;

import javax.annotation.Nullable;

public final class TextureCopyBounds {
    private TextureCopyBounds() {
    }

    @Nullable
    public static Corners presentingCorners(
            int sourceX,
            int sourceY,
            int destinationX,
            int destinationY,
            int width,
            int height
    ) {
        if (!ViewportRenderTargets.isPresenting()) {
            return null;
        }
        return new Corners(
                sourceX + width,
                sourceY + height,
                destinationX + width,
                destinationY + height
        );
    }

    public record Corners(int sourceRight, int sourceBottom, int destinationRight, int destinationBottom) {
    }
}
