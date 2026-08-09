package net.jr.mixin;

import net.jr.client.sound.music.MusicTransitionState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class MixinClientPacketListener {
    @Unique private boolean javareforged$suppressRespawnForcedStop;
    @Unique private boolean javareforged$suppressRespawnMusicStop;

    @Inject(method = "handleRespawn", at = @At("HEAD"))
    private void javareforged$protectMusicForRespawn(ClientboundRespawnPacket packet, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean preserveMusic = this.javareforged$shouldPreserveMusicAcrossRespawn(minecraft, packet);
        this.javareforged$suppressRespawnForcedStop = preserveMusic;
        this.javareforged$suppressRespawnMusicStop = preserveMusic;

        if (this.javareforged$suppressRespawnForcedStop) {
            MusicTransitionState.pushForcedSoundStopProtection();
            MusicTransitionState.requestMusicContinuityProtection(200);
        }

        if (this.javareforged$suppressRespawnMusicStop) {
            MusicTransitionState.pushRespawnMusicStopProtection();
        }
    }

    @Inject(method = "handleRespawn", at = @At("TAIL"))
    private void javareforged$clearRespawnMusicProtection(ClientboundRespawnPacket packet, CallbackInfo ci) {
        if (this.javareforged$suppressRespawnForcedStop) {
            MusicTransitionState.popForcedSoundStopProtection();
            this.javareforged$suppressRespawnForcedStop = false;
        }

        if (this.javareforged$suppressRespawnMusicStop) {
            MusicTransitionState.popRespawnMusicStopProtection();
            this.javareforged$suppressRespawnMusicStop = false;
        }
    }

    @Redirect(
        method = "handleRespawn",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/sounds/MusicManager;stopPlaying()V"
        )
    )
    private void javareforged$maybeStopMusicOnRespawn(MusicManager musicManager) {
        if (!MusicTransitionState.shouldProtectRespawnMusicStop()) {
            musicManager.stopPlaying();
        }
    }

    @Unique
    private boolean javareforged$shouldPreserveMusicAcrossRespawn(
        Minecraft minecraft,
        ClientboundRespawnPacket packet
    ) {
        LocalPlayer currentPlayer = minecraft.player;
        if (currentPlayer == null || currentPlayer.level() == null) {
            return true;
        }

        ResourceKey<Level> targetDimension = packet.commonPlayerSpawnInfo().dimension();
        boolean dimensionChanged = targetDimension != currentPlayer.level().dimension();
        boolean wasDying = currentPlayer.isDeadOrDying();
        return wasDying || !dimensionChanged;
    }
}
