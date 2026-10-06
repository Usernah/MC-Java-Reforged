package net.jr.mixin.compat.sodium;

import net.jr.api.client.render.world.WorldRenderContext;
import net.jr.api.client.render.world.WorldRenderContexts;
import net.jr.client.compat.sodium.SodiumChunkBuildContextRouter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.compile.executor.ChunkBuilder", remap = false)
abstract class SodiumChunkBuilderMixin {
    @Inject(method = "scheduleTask", at = @At("RETURN"), remap = false)
    private void javaReforged$bindJobToSourceLevel(
        @Coerce Object task,
        boolean important,
        @Coerce Object consumer,
        CallbackInfoReturnable<Object> cir
    ) {
        WorldRenderContext context = WorldRenderContexts.currentOrNull();
        if (context != null && cir.getReturnValue() != null) {
            SodiumChunkBuildContextRouter.register(cir.getReturnValue(), context.level());
        }
    }

    @Redirect(
        method = "tryStealTask",
        at = @At(
            value = "INVOKE",
            target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/executor/ChunkJob;execute(Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildContext;)V"
        ),
        remap = false
    )
    private void javaReforged$executeStolenTaskForItsLevel(@Coerce Object job, @Coerce Object fallbackContext) {
        SodiumChunkBuildContextRouter.execute(job, fallbackContext);
    }

    @Redirect(
        method = "tryStealTask",
        at = @At(
            value = "INVOKE",
            target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildContext;cleanup()V"
        ),
        remap = false
    )
    private void javaReforged$cleanupStolenTaskContext(@Coerce Object fallbackContext) {
        SodiumChunkBuildContextRouter.cleanup(fallbackContext);
    }
}
