package net.alnv.javareforged.mixin.SSM;

import java.util.function.Consumer;
import net.alnv.javareforged.ClientRuntime.runtime.TerrainDebug;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientChunkCache.class)
public abstract class ClientChunkCacheDiagnosticsMixin {
    @Shadow @Final private ClientLevel level;

    @Inject(method = "replaceWithPacketData", at = @At("HEAD"))
    private void splitTest$diagReplaceHead(int x, int z, FriendlyByteBuf buffer, CompoundTag tag, Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> consumer, CallbackInfoReturnable<LevelChunk> cir) {
        this.splitTest$recordCache("replace:head", x, z, this.splitTest$hasChunk(x, z));
    }

    @Inject(method = "replaceWithPacketData", at = @At("TAIL"))
    private void splitTest$diagReplaceTail(int x, int z, FriendlyByteBuf buffer, CompoundTag tag, Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> consumer, CallbackInfoReturnable<LevelChunk> cir) {
        this.splitTest$recordCache("replace:tail", x, z, cir.getReturnValue() != null || this.splitTest$hasChunk(x, z));
    }

    @Inject(method = "drop", at = @At("HEAD"))
    private void splitTest$diagDropHead(ChunkPos pos, CallbackInfo ci) {
        this.splitTest$recordCache("drop:head", pos.x, pos.z, this.splitTest$hasChunk(pos.x, pos.z));
    }

    @Inject(method = "drop", at = @At("TAIL"))
    private void splitTest$diagDropTail(ChunkPos pos, CallbackInfo ci) {
        this.splitTest$recordCache("drop:tail", pos.x, pos.z, this.splitTest$hasChunk(pos.x, pos.z));
    }

    @Inject(method = "updateViewCenter", at = @At("HEAD"))
    private void splitTest$diagCenterHead(int x, int z, CallbackInfo ci) {
        this.splitTest$recordCache("center:head", x, z, true);
    }

    @Inject(method = "updateViewCenter", at = @At("TAIL"))
    private void splitTest$diagCenterTail(int x, int z, CallbackInfo ci) {
        this.splitTest$recordCache("center:tail", x, z, true);
    }

    @Inject(method = "updateViewRadius", at = @At("HEAD"))
    private void splitTest$diagRadiusHead(int radius, CallbackInfo ci) {
        this.splitTest$recordCache("radius:head", radius, 0, true);
    }

    @Inject(method = "updateViewRadius", at = @At("TAIL"))
    private void splitTest$diagRadiusTail(int radius, CallbackInfo ci) {
        this.splitTest$recordCache("radius:tail", radius, 0, true);
    }

    private boolean splitTest$hasChunk(int x, int z) {
        return ((ClientChunkCache)(Object)this).getChunk(x, z, ChunkStatus.FULL, false) != null;
    }

    private void splitTest$recordCache(String event, int chunkX, int chunkZ, boolean chunkPresent) {
        if (!TerrainDebug.enabled()) {
            return;
        }
        ClientChunkCache cache = (ClientChunkCache)(Object)this;
        TerrainDebug.ChunkRouteInfo route = TerrainDebug.chunkRouteInfo(event, null, this.level, cache, null, -1, false);
        TerrainDebug.recordChunkCacheMutation(route, chunkX, chunkZ, chunkPresent);
    }
}
