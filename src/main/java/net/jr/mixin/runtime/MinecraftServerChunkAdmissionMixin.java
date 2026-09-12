package net.jr.mixin.runtime;

import net.jr.server.chunk.LegacyChunkAdmissionLimiter;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerChunkAdmissionMixin {
    @Inject(method = "tickServer", at = @At("HEAD"))
    private void javaReforged$beginLegacyChunkAdmissionTick(BooleanSupplier haveTime, CallbackInfo callback) {
        LegacyChunkAdmissionLimiter.beginServerTick((MinecraftServer) (Object) this);
    }

    @Inject(method = "tickServer", at = @At("TAIL"))
    private void javaReforged$dispatchLegacyChunkAdmissions(BooleanSupplier haveTime, CallbackInfo callback) {
        LegacyChunkAdmissionLimiter.dispatchQueuedTickets();
    }

    @Inject(method = "stopServer", at = @At("TAIL"))
    private void javaReforged$clearLegacyChunkAdmissionState(CallbackInfo callback) {
        LegacyChunkAdmissionLimiter.onServerStopped((MinecraftServer) (Object) this);
    }
}
