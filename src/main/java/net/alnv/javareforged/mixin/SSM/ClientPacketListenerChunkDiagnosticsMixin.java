package net.alnv.javareforged.mixin.SSM;

import net.alnv.javareforged.ClientRuntime.runtime.ActiveSlot;
import net.alnv.javareforged.ClientRuntime.runtime.TerrainDebug;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheCenterPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheRadiusPacket;
import net.minecraft.world.level.ChunkPos;

import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerChunkDiagnosticsMixin {
    @Shadow private ClientLevel level;
    @Shadow private int serverChunkRadius;

    @Inject(method = "handleLevelChunkWithLight", at = @At("HEAD"))
    private void splitTest$diagLevelChunkHead(ClientboundLevelChunkWithLightPacket packet, CallbackInfo ci) {
        this.splitTest$recordPacket("levelChunk:head", packet.getX(), packet.getZ(), true);
    }

    @Inject(method = "handleLevelChunkWithLight", at = @At("TAIL"))
    private void splitTest$diagLevelChunkTail(ClientboundLevelChunkWithLightPacket packet, CallbackInfo ci) {
        this.splitTest$recordPacket("levelChunk:tail", packet.getX(), packet.getZ(), true);
    }

    @Inject(method = "handleForgetLevelChunk", at = @At("HEAD"))
    private void splitTest$diagForgetChunkHead(ClientboundForgetLevelChunkPacket packet, CallbackInfo ci) {
        ChunkPos pos = packet.pos();
        this.splitTest$recordPacket("forgetChunk:head", pos.x, pos.z, true);
    }

    @Inject(method = "handleForgetLevelChunk", at = @At("TAIL"))
    private void splitTest$diagForgetChunkTail(ClientboundForgetLevelChunkPacket packet, CallbackInfo ci) {
        ChunkPos pos = packet.pos();
        this.splitTest$recordPacket("forgetChunk:tail", pos.x, pos.z, true);
    }

    @Inject(method = "handleSetChunkCacheCenter", at = @At("HEAD"))
    private void splitTest$diagSetCenterHead(ClientboundSetChunkCacheCenterPacket packet, CallbackInfo ci) {
        this.splitTest$recordPacket("cacheCenter:head", packet.getX(), packet.getZ(), true);
    }

    @Inject(method = "handleSetChunkCacheCenter", at = @At("TAIL"))
    private void splitTest$diagSetCenterTail(ClientboundSetChunkCacheCenterPacket packet, CallbackInfo ci) {
        this.splitTest$recordPacket("cacheCenter:tail", packet.getX(), packet.getZ(), true);
    }

    @Inject(method = "handleSetChunkCacheRadius", at = @At("HEAD"))
    private void splitTest$diagSetRadiusHead(ClientboundSetChunkCacheRadiusPacket packet, CallbackInfo ci) {
        this.splitTest$recordPacket("cacheRadius:head", packet.getRadius(), 0, true);
    }

    @Inject(method = "handleSetChunkCacheRadius", at = @At("TAIL"))
    private void splitTest$diagSetRadiusTail(ClientboundSetChunkCacheRadiusPacket packet, CallbackInfo ci) {
        this.splitTest$recordPacket("cacheRadius:tail", packet.getRadius(), 0, true);
    }

    @Inject(method = "handleLightUpdatePacket", at = @At("HEAD"))
    private void splitTest$diagLightPacketHead(ClientboundLightUpdatePacket packet, CallbackInfo ci) {
        this.splitTest$recordPacket("light:head", packet.getX(), packet.getZ(), true);
    }

    @Inject(method = "handleLightUpdatePacket", at = @At("TAIL"))
    private void splitTest$diagLightPacketTail(ClientboundLightUpdatePacket packet, CallbackInfo ci) {
        this.splitTest$recordPacket("light:tail", packet.getX(), packet.getZ(), true);
    }

    private void splitTest$recordPacket(String event, int chunkX, int chunkZ, boolean inspectChunk) {
        if (!TerrainDebug.enabled()) {
            return;
        }
        ClientPacketListener listener = (ClientPacketListener)(Object)this;
        ClientLevel currentLevel = this.level;
        ClientChunkCache source = currentLevel == null ? null : currentLevel.getChunkSource();
        TerrainDebug.ChunkRouteInfo route = TerrainDebug.chunkRouteInfo(
            event,
            listener,
            currentLevel,
            source,
            ActiveSlot.idOrNull(),
            this.serverChunkRadius,
            Minecraft.getInstance().level == currentLevel
        );
        boolean chunkPresent = !inspectChunk;
        if (inspectChunk && currentLevel != null) {
            chunkPresent = currentLevel.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false) != null;
        }
        TerrainDebug.recordChunkPacketRoute(route, chunkX, chunkZ, chunkPresent);
    }
}
