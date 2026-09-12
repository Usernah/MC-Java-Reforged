package net.jr.mixin.runtime;

import net.jr.server.chunk.LegacyChunkAdmissionLimiter;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin {
    @Invoker("getPlayerViewDistance")
    abstract int javaReforged$getPlayerViewDistance(ServerPlayer player);

    @Inject(method = "updatePlayerStatus", at = @At("HEAD"))
    private void javaReforged$beginInitialChunkBootstrap(ServerPlayer player, boolean added, CallbackInfo callback) {
        if (added) {
            LegacyChunkAdmissionLimiter.beginBootstrap(player, this.javaReforged$getPlayerViewDistance(player));
        }
    }

    @Inject(method = "updatePlayerStatus", at = @At("RETURN"))
    private void javaReforged$clearInitialChunkBootstrap(ServerPlayer player, boolean added, CallbackInfo callback) {
        if (added) {
            LegacyChunkAdmissionLimiter.clearBootstrap();
        }
    }
}
