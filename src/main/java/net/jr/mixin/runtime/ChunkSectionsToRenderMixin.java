package net.jr.mixin.runtime;

import com.mojang.blaze3d.textures.GpuSampler;
import net.jr.client.runtime.render.experimental.TerrainMultiDraw;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkSectionsToRender.class)
public abstract class ChunkSectionsToRenderMixin {
    @Inject(method = "renderGroup", at = @At("HEAD"), cancellable = true)
    private void javaReforged$renderOpaqueMultiDraw(ChunkSectionLayerGroup group, GpuSampler sampler, CallbackInfo ci) {
        if (TerrainMultiDraw.render((ChunkSectionsToRender)(Object)this, group, sampler)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderGroup", at = @At("RETURN"))
    private void javaReforged$finishOpaqueVanillaFallback(ChunkSectionLayerGroup group, GpuSampler sampler, CallbackInfo ci) {
        TerrainMultiDraw.finishVanilla((ChunkSectionsToRender)(Object)this, group);
    }
}
