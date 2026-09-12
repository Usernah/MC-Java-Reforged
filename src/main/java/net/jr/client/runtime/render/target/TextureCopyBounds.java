package net.jr.client.runtime.render.target;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import javax.annotation.Nullable;

public final class TextureCopyBounds {
    private static final ThreadLocal<Boolean> COPYING_REGION = ThreadLocal.withInitial(() -> false);

    private TextureCopyBounds() {
    }

    public static void copyRegion(GpuTexture source, GpuTexture destination, int destX, int destY,
                                  int sourceX, int sourceY, int width, int height) {
        boolean previous = COPYING_REGION.get();
        COPYING_REGION.set(true);
        try {
            RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(
                source, destination, 0, destX, destY, sourceX, sourceY, width, height
            );
        } finally {
            if (previous) COPYING_REGION.set(true);
            else COPYING_REGION.remove();
        }
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
        if (!ViewportRenderTargets.isPresenting() && !COPYING_REGION.get()) {
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
