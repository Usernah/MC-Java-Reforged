package net.jr.mixin.compat.sodium;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.jr.api.client.render.world.WorldRenderContext;
import net.jr.api.client.render.world.WorldRenderContexts;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.UniformBufferManager", remap = false)
abstract class SodiumUniformBufferManagerMixin {
    @Shadow
    private boolean hasUpdatedThisFrame;

    @Shadow
    private GpuBufferSlice uniformData;
    @Unique
    private int javaReforged$uniformSlot = -1;

    @Inject(method = "update", at = @At("HEAD"), remap = false)
    private void javaReforged$prepareUniformsForActiveView(
        @Coerce Object matrices,
        @Coerce Object fogParameters,
        CallbackInfo ci
    ) {
        WorldRenderContext context = WorldRenderContexts.currentOrNull();
        if (context == null) {
            return;
        }
        if (context.slotId() != this.javaReforged$uniformSlot || this.uniformData == null) {
            this.javaReforged$uniformSlot = context.slotId();
            this.hasUpdatedThisFrame = false;
        }
    }
}
