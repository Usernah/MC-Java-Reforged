package net.jr.client.runtime.render.pass;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import java.nio.ByteOrder;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import net.jr.client.render.pipeline.ModRenderPipelines;
import net.jr.client.runtime.slot.ClientSlots;
import net.jr.Java_reforged;
import net.minecraft.client.renderer.MappableRingBuffer;


public final class LegacyGammaPass {
    private static final int CONFIG_SIZE = 16;
    private static final MappableRingBuffer[] CONFIG_BUFFERS =
        new MappableRingBuffer[ClientSlots.MAX_SLOTS];
    private static boolean pipelineFailureReported;

    private LegacyGammaPass() {
    }

    public static boolean process(int slotId, RenderTarget input, RenderTarget output, double strength) {
        RenderSystem.assertOnRenderThread();
        ClientSlots.requireSlotId(slotId);
        float safeStrength = (float)Math.clamp(strength, 0.0D, 1.0D);
        MappableRingBuffer uniforms = configBuffer(slotId);
        GpuBuffer buffer = uniforms.currentBuffer();
        try (GpuBufferSlice.MappedView mapped = buffer.map(false, true)) {
            Std140Builder.intoBuffer(mapped.data().order(ByteOrder.nativeOrder()))
                .putFloat(safeStrength);
        }

        try {
            try (RenderPass renderPass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(
                    () -> "Java Reforged legacy gamma",
                    Objects.requireNonNull(output.getColorTextureView()),
                    Optional.empty(),
                    null,
                    OptionalDouble.empty()
                )) {
                renderPass.setPipeline(ModRenderPipelines.LEGACY_GAMMA);
                RenderSystem.bindDefaultUniforms(renderPass);
                renderPass.setUniform("LegacyGammaConfig", buffer.slice(0L, CONFIG_SIZE));
                renderPass.bindTexture(
                    "InSampler",
                    Objects.requireNonNull(input.getColorTextureView()),
                    RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST)
                );
                renderPass.draw(3, 1, 0, 0);
            }
            pipelineFailureReported = false;
            uniforms.rotate();
            return true;
        } catch (IllegalStateException exception) {
            if (!pipelineFailureReported) {
                Java_reforged.LOGGER.error("Legacy gamma pass was skipped because its render pipeline is unavailable", exception);
                pipelineFailureReported = true;
            }
            uniforms.rotate();
            return false;
        }
    }

    public static void close() {
        RenderSystem.assertOnRenderThread();
        for (int slotId = 0; slotId < CONFIG_BUFFERS.length; slotId++) {
            MappableRingBuffer buffer = CONFIG_BUFFERS[slotId];
            if (buffer != null) {
                buffer.close();
                CONFIG_BUFFERS[slotId] = null;
            }
        }
        pipelineFailureReported = false;
    }

    private static MappableRingBuffer configBuffer(int slotId) {
        MappableRingBuffer buffer = CONFIG_BUFFERS[slotId];
        if (buffer == null) {
            int bufferSlot = slotId;
            buffer = new MappableRingBuffer(
                () -> "Java Reforged legacy gamma config / slot " + bufferSlot,
                GpuBuffer.USAGE_MAP_WRITE | GpuBuffer.USAGE_UNIFORM,
                CONFIG_SIZE
            );
            CONFIG_BUFFERS[slotId] = buffer;
        }
        return buffer;
    }
}
