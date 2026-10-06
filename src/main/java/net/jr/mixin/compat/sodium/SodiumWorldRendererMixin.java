package net.jr.mixin.compat.sodium;

import net.jr.api.client.local.Client;
import net.jr.client.compat.sodium.SodiumWorldRenderBackend;
import net.jr.api.client.render.world.WorldRenderContext;
import net.jr.api.client.render.world.WorldRenderContexts;
import net.jr.client.runtime.render.world.WorldRenderBackends;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer", remap = false)
abstract class SodiumWorldRendererMixin {
    @Inject(method = "deleteRendererState", at = @At("HEAD"), cancellable = true, remap = false)
    private void javaReforged$releaseOwnedRendererState(CallbackInfo ci) {
        if (WorldRenderBackends.sodium().releaseRendererState(this)) {
            ci.cancel();
        }
    }

    @Inject(method = "setLevel", at = @At("HEAD"), cancellable = true, remap = false)
    private void javaReforged$routeLevelToItsTerrainState(ClientLevel newLevel, CallbackInfo ci) {
        if (WorldRenderBackends.sodium().routeLevelChange(this, newLevel)) {
            ci.cancel();
        }
    }

    @Inject(method = "setupTerrain", at = @At("HEAD"), cancellable = true, remap = false)
    private void javaReforged$captureViewSetup(
        Camera camera,
        @Coerce Object viewport,
        @Coerce Object fogParameters,
        boolean useOcclusionCulling,
        boolean updateChunksImmediately,
        Matrix4f cullMatrix,
        CallbackInfo ci
    ) {
        SodiumWorldRenderBackend backend = WorldRenderBackends.sodium();
        if (backend.captureTerrainSetup(
            this,
            camera,
            viewport,
            fogParameters,
            useOcclusionCulling,
            updateChunksImmediately,
            cullMatrix
        )) {
            ci.cancel();
        }
    }

    @Inject(method = "initRenderer", at = @At("RETURN"), remap = false)
    private void javaReforged$rehydrateSharedTerrainAfterReload(CallbackInfo ci) {
        WorldRenderBackends.sodium().terrainRendererInitialized(this);
    }

    @Inject(method = "scheduleRebuildForChunk", at = @At("HEAD"), remap = false)
    private void javaReforged$selectDirtySectionSource(
        int x,
        int y,
        int z,
        boolean playerChanged,
        CallbackInfo ci
    ) {
        WorldRenderBackends.sodium().activateTerrainSourceForCurrentExecution(this);
    }

    @ModifyVariable(
        method = "drawChunkLayer",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0,
        remap = false
    )
    private double javaReforged$useActiveCameraX(double original) {
        WorldRenderContext context = WorldRenderContexts.currentOrNull();
        if (context == null) {
            return original;
        }

        if (context.player() == null  ||  !context.player().isAlive()) {
        return original;
        }

        if (context.camera() == null) {
            return original;
        }
        return context.camera().position().x;
    }

    @ModifyVariable(
        method = "drawChunkLayer",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 1,
        remap = false
    )
    private double javaReforged$useActiveCameraY(double original) {
        WorldRenderContext context = WorldRenderContexts.currentOrNull();
        return context != null ? context.camera().position().y : original;
    }

    @ModifyVariable(
        method = "drawChunkLayer",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 2,
        remap = false
    )
    private double javaReforged$useActiveCameraZ(double original) {
        WorldRenderContext context = WorldRenderContexts.currentOrNull();
        return context != null ? context.camera().position().z : original;
    }
}
