package net.jr.mixin.compat.sodium;

import net.jr.client.compat.sodium.SodiumChunkBuildContextRouter;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.compile.executor.ChunkBuilder", remap = false)
abstract class SodiumChunkBuilderMixin {
    @Unique
    private ClientLevel javaReforged$level;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void javaReforged$captureLevel(
        ClientLevel level,
        @Coerce Object vertexType,
        CallbackInfo ci
    ) {
        this.javaReforged$level = level;
    }

    @Inject(method = "scheduleTask", at = @At("RETURN"), remap = false)
    private void javaReforged$bindJobToSourceLevel(
        @Coerce Object task,
        boolean important,
        @Coerce Object consumer,
        CallbackInfoReturnable<Object> cir
    ) {
        if (this.javaReforged$level != null && cir.getReturnValue() != null) {
            SodiumChunkBuildContextRouter.register(
                this,
                cir.getReturnValue(),
                this.javaReforged$level
            );
        }
    }

    @Inject(method = "shutdown", at = @At("HEAD"), remap = false)
    private void javaReforged$discardScheduledRoutes(CallbackInfo ci) {
        SodiumChunkBuildContextRouter.discardBuilder(this, this.javaReforged$level);
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
