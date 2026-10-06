package net.jr.mixin.compat.sodium;

import net.jr.client.compat.sodium.SodiumChunkBuildContextRouter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.compile.executor.ChunkBuilder$WorkerRunnable", remap = false)
abstract class SodiumChunkBuilderWorkerMixin {
    @Redirect(
        method = "run",
        at = @At(
            value = "INVOKE",
            target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/executor/ChunkJob;execute(Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildContext;)V"
        ),
        remap = false
    )
    private void javaReforged$executeTaskForItsLevel(@Coerce Object job, @Coerce Object fallbackContext) {
        SodiumChunkBuildContextRouter.execute(job, fallbackContext);
    }

    @Redirect(
        method = "run",
        at = @At(
            value = "INVOKE",
            target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildContext;cleanup()V"
        ),
        remap = false
    )
    private void javaReforged$cleanupTaskContext(@Coerce Object fallbackContext) {
        SodiumChunkBuildContextRouter.cleanup(fallbackContext);
    }
}
