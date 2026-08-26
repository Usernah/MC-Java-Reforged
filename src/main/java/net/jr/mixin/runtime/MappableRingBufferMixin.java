package net.jr.mixin.runtime;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuFence;
import java.util.function.Supplier;
import net.jr.client.runtime.render.buffer.ViewportRingBufferFactory;
import net.minecraft.client.renderer.MappableRingBuffer;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MappableRingBuffer.class)
public abstract class MappableRingBufferMixin {
    @Shadow
    @Final
    @Mutable
    private GpuBuffer[] buffers;

    @Shadow
    @Final
    @Mutable
    private @Nullable GpuFence[] fences;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void javaReforged$expandViewportRings(Supplier<String> label, int usage, int size, CallbackInfo callback) {
        ViewportRingBufferFactory.Expansion expansion = ViewportRingBufferFactory.expand(
                label,
                usage,
                size,
                this.buffers,
                this.fences
        );
        this.buffers = expansion.buffers();
        this.fences = expansion.fences();
    }

    @ModifyConstant(method = {"rotate", "close"}, constant = @Constant(intValue = 3))
    private int javaReforged$useActualRingCapacity(int vanillaCount) {
        return this.buffers.length;
    }
}
