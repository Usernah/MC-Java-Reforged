package net.jr.mixin.runtime;

import net.jr.client.runtime.terrain.SharedTerrainStore;
import net.jr.client.runtime.terrain.TerrainMeshVersion;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CompiledSectionMesh.class)
public abstract class TerrainMeshVersionMixin implements TerrainMeshVersion {
    @Unique private SharedTerrainStore.BuildTicket javaReforged$ticket;

    @Override public SharedTerrainStore.BuildTicket javaReforged$buildTicket() {
        return this.javaReforged$ticket;
    }

    @Override public void javaReforged$buildTicket(SharedTerrainStore.BuildTicket ticket) {
        this.javaReforged$ticket = ticket;
        ticket.attachMesh();
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void javaReforged$releasePublication(CallbackInfo ci) {
        if (this.javaReforged$ticket != null) {
            this.javaReforged$ticket.abandon();
        }
    }
}
