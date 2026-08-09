package net.alnv.javareforged.mixin.SSM;

import net.alnv.javareforged.ClientRuntime.runtime.TerrainSectionOwners;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection")
public abstract class SectionRenderDispatcherRenderSectionMixin {
    @Inject(method = "createCompileTask", at = @At("RETURN"))
    private void splitTest$registerTerrainTask(RenderRegionCache regionCache, CallbackInfoReturnable<Object> callback) {
        TerrainSectionOwners.taskCreated(
            (SectionRenderDispatcher.RenderSection)(Object)this,
            callback.getReturnValue()
        );
    }

    @Inject(method = "getDistToPlayerSqr", at = @At("HEAD"), cancellable = true)
    private void splitTest$useColumnCompilationPriority(CallbackInfoReturnable<Double> callback) {
        callback.setReturnValue(
            TerrainSectionOwners.compilationPriority((SectionRenderDispatcher.RenderSection)(Object)this)
        );
    }

    @Inject(method = "doesChunkExistAt", at = @At("HEAD"), cancellable = true)
    private void splitTest$deferNeighborCheckUntilOwner(BlockPos pos, CallbackInfoReturnable<Boolean> callback) {
        if (TerrainSectionOwners.deferUntilOwner((SectionRenderDispatcher.RenderSection)(Object)this, "neighbor-check")) {
            callback.setReturnValue(false);
        }
    }

    @Redirect(
        method = {"doesChunkExistAt", "createCompileTask"},
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher;level:Lnet/minecraft/client/multiplayer/ClientLevel;")
    )
    private ClientLevel splitTest$useOwnerLevel(SectionRenderDispatcher dispatcher) {
        return TerrainSectionOwners.levelForSection((SectionRenderDispatcher.RenderSection)(Object)this);
    }

    @Redirect(
        method = "createVertexSorting",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher;getCameraPosition()Lnet/minecraft/world/phys/Vec3;")
    )
    private Vec3 splitTest$useOwnerCameraPosition(SectionRenderDispatcher dispatcher) {
        return TerrainSectionOwners.cameraPositionForSection((SectionRenderDispatcher.RenderSection)(Object)this);
    }
}
