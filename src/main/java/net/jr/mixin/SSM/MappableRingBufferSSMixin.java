package net.jr.mixin.SSM;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuFence;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Arrays;
import java.util.function.Supplier;
import net.jr.ClientRuntime.slot.PlayerSlots;
import net.minecraft.client.renderer.MappableRingBuffer;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps viewport-specific mapped uniforms alive across the shared GPU submit. */
@Mixin(MappableRingBuffer.class)
public abstract class MappableRingBufferSSMixin {
    @Unique
    private static final int VANILLA_BUFFER_COUNT = 3;
    @Unique
    private static final int SPLIT_VIEWPORT_BUFFER_COUNT = VANILLA_BUFFER_COUNT * PlayerSlots.MAX_SLOTS;

    @Shadow
    @Final
    @Mutable
    private GpuBuffer[] buffers;

    @Shadow
    @Final
    @Mutable
    private @Nullable GpuFence[] fences;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void splitTest$expandViewportRings(Supplier<String> label, int usage, int size, CallbackInfo callback) {
        String baseLabel = label.get();
        if (!splitTest$isViewportSpecific(baseLabel)) {
            return;
        }

        int vanillaCount = this.buffers.length;
        if (vanillaCount >= SPLIT_VIEWPORT_BUFFER_COUNT) {
            return;
        }

        GpuBuffer[] expandedBuffers = Arrays.copyOf(this.buffers, SPLIT_VIEWPORT_BUFFER_COUNT);
        GpuFence[] expandedFences = Arrays.copyOf(this.fences, SPLIT_VIEWPORT_BUFFER_COUNT);
        GpuDevice device = RenderSystem.getDevice();
        for (int index = vanillaCount; index < SPLIT_VIEWPORT_BUFFER_COUNT; index++) {
            int bufferIndex = index;
            expandedBuffers[index] = device.createBuffer(() -> baseLabel + " #" + bufferIndex, usage, size);
        }

        this.buffers = expandedBuffers;
        this.fences = expandedFences;
    }

    @ModifyConstant(method = {"rotate", "close"}, constant = @Constant(intValue = 3))
    private int splitTest$useActualRingCapacity(int vanillaCount) {
        return this.buffers.length;
    }

    @Unique
    private static boolean splitTest$isViewportSpecific(String label) {
        return "Cloud UBO".equals(label)
            || "Cloud UTB".equals(label)
            || "Lightmap UBO".equals(label)
            || "Fog UBO".equals(label)
            || label.endsWith(" SamplerInfo");
    }
}
