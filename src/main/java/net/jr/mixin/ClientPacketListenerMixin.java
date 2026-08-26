package net.jr.mixin;

import net.jr.client.sound.music.RespawnMusicState;
import net.jr.client.sound.music.RespawnMusicTransitionHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Unique private final RespawnMusicState javaReforged$respawnMusicState = new RespawnMusicState();

    @Inject(method = "handleRespawn", at = @At("HEAD"))
    private void javaReforged$beginRespawnMusicProtection(ClientboundRespawnPacket packet, CallbackInfo ci) {
        RespawnMusicTransitionHandler.begin(Minecraft.getInstance(), packet, this.javaReforged$respawnMusicState);
    }

    @Inject(method = "handleRespawn", at = @At("TAIL"))
    private void javaReforged$finishRespawnMusicProtection(ClientboundRespawnPacket packet, CallbackInfo ci) {
        RespawnMusicTransitionHandler.finish(this.javaReforged$respawnMusicState);
    }

    @Redirect(method = "handleRespawn", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/sounds/MusicManager;stopPlaying()V"))
    private void javaReforged$guardRespawnMusicStop(MusicManager musicManager) {
        RespawnMusicTransitionHandler.stopUnlessProtected(musicManager);
    }
}
