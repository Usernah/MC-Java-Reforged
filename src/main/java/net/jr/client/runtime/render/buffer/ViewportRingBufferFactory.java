package net.jr.client.runtime.render.buffer;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuFence;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Arrays;
import java.util.function.Supplier;
import net.jr.client.runtime.slot.LocalClientSlotRegistry;

public final class ViewportRingBufferFactory {
    private static final int VANILLA_BUFFER_COUNT = 3;
    private static final int VIEWPORT_BUFFER_COUNT = VANILLA_BUFFER_COUNT * LocalClientSlotRegistry.MAX_SLOTS;

    private ViewportRingBufferFactory() {
    }

    public static Expansion expand(
            Supplier<String> label,
            int usage,
            int size,
            GpuBuffer[] buffers,
            GpuFence[] fences
    ) {
        String baseLabel = label.get();
        if (!isViewportSpecific(baseLabel) || buffers.length >= VIEWPORT_BUFFER_COUNT) {
            return new Expansion(buffers, fences);
        }

        int previousCount = buffers.length;
        GpuBuffer[] expandedBuffers = Arrays.copyOf(buffers, VIEWPORT_BUFFER_COUNT);
        GpuFence[] expandedFences = Arrays.copyOf(fences, VIEWPORT_BUFFER_COUNT);
        GpuDevice device = RenderSystem.getDevice();
        for (int index = previousCount; index < VIEWPORT_BUFFER_COUNT; index++) {
            int bufferIndex = index;
            expandedBuffers[index] = device.createBuffer(() -> baseLabel + " #" + bufferIndex, usage, size);
        }
        return new Expansion(expandedBuffers, expandedFences);
    }

    private static boolean isViewportSpecific(String label) {
        return "Cloud UBO".equals(label)
                || "Cloud UTB".equals(label)
                || "Lightmap UBO".equals(label)
                || "Fog UBO".equals(label)
                || label.endsWith(" SamplerInfo");
    }

    public record Expansion(GpuBuffer[] buffers, GpuFence[] fences) {
    }
}
