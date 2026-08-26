package net.jr.client.runtime.render.buffer;

import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.fog.FogRenderer;

public final class ViewportUniformRotation {
    private ViewportUniformRotation() {
    }

    public static void rotate(MappableRingBuffer buffer) {
        buffer.rotate();
    }

    public static void rotateWorldFog(FogRenderer.FogMode mode, MappableRingBuffer buffer) {
        if (mode == FogRenderer.FogMode.WORLD) {
            buffer.rotate();
        }
    }

    public static boolean replacesEndFrameRotation() {
        return true;
    }
}
