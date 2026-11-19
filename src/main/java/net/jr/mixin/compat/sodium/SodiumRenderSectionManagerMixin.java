package net.jr.mixin.compat.sodium;

import net.jr.api.client.render.world.WorldRenderContexts;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager", remap = false)
abstract class SodiumRenderSectionManagerMixin {
    @Inject(method = "prepareRenderTrees", at = @At("HEAD"), cancellable = true, remap = false)
    private void javaReforged$keepCullingSerial(
        @Coerce Object viewport,
        @Coerce Object fogParameters,
        boolean useOcclusionCulling,
        CallbackInfo ci
    ) {
        if (WorldRenderContexts.currentOrNull() != null) {
            ci.cancel();
        }
    }

    @ModifyVariable(
        method = "finalizeRenderLists",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0,
        remap = false
    )
    private boolean javaReforged$finalizeSplitViewSynchronously(boolean updateChunksImmediately) {
        return updateChunksImmediately || WorldRenderContexts.currentOrNull() != null;
    }

}
