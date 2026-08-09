package net.jr.mixin;

import net.jr.client.sound.bridge.MusicManagerBridge;
import net.jr.client.sound.bridge.SoundEngineBridge;
import net.jr.client.sound.config.SoundTransitionConfig;
import net.jr.client.sound.music.MusicTransitionState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.client.sounds.SoundManager;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft {
    @Shadow public @Nullable LocalPlayer player;
    @Shadow public @Nullable ClientLevel level;
    @Shadow @Final private MusicManager musicManager;
    @Shadow @Final private SoundManager soundManager;

    @Unique private boolean shouldFadeMusicOnLoad = false;
    @Unique private boolean javareforged$worldLoadInternalDisconnect = false;

    @Inject(method = "doWorldLoad", at = @At("HEAD"))
    private void javareforged$onWorldLoadStart(CallbackInfo ci) {
        MusicManagerBridge musicManagerBridge = (MusicManagerBridge) this.musicManager;
        musicManagerBridge.moods$setLoadingMode(true);
        musicManagerBridge.moods$setStopGraceTicks(Math.max(20, SoundTransitionConfig.fadeOutTicks));
        musicManagerBridge.moods$fadeOutCurrentMusic();

        this.shouldFadeMusicOnLoad = false;
        MusicTransitionState.beginWorldLoad();
    }

    @Inject(method = "setLevel", at = @At("TAIL"))
    private void javareforged$onWorldLoadFinished(ClientLevel level, CallbackInfo ci) {
        this.javareforged$finishWorldLoadMusicState();
    }

    @Inject(
        method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;ZZ)V",
        at = @At("HEAD")
    )
    private void javareforged$onDisconnectStart(
        Screen nextScreen,
        boolean keepResourcePacks,
        boolean stopSound,
        CallbackInfo ci
    ) {
        if (MusicTransitionState.shouldProtectWorldLoadAudio() && this.javareforged$worldLoadInternalDisconnect) {
            this.shouldFadeMusicOnLoad = false;
            return;
        }

        ClientLevel activeLevel = this.javareforged$globalMusicLevel();
        if (activeLevel != null) {
            SoundEngineBridge soundHandler = (SoundEngineBridge) this.soundManager;
            this.shouldFadeMusicOnLoad = true;
            soundHandler.moods$clearQueued();
            soundHandler.moods$fadeSounds((float) SoundTransitionConfig.fadeOutTicks);
        } else {
            this.shouldFadeMusicOnLoad = false;
        }

        this.javareforged$releaseWorldLoadMusicProtection();
    }

    @Inject(
        method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;ZZ)V",
        at = @At("TAIL")
    )
    private void javareforged$onDisconnectFinished(
        Screen nextScreen,
        boolean keepResourcePacks,
        boolean stopSound,
        CallbackInfo ci
    ) {
        this.shouldFadeMusicOnLoad = false;

        if (this.javareforged$worldLoadInternalDisconnect) {
            return;
        }

        MusicTransitionState.clear();
        ((MusicManagerBridge) this.musicManager).moods$resetAfterWorldExit();
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void javareforged$syncMusicWorldTransitionState(CallbackInfo ci) {
        if (!MusicTransitionState.shouldProtectWorldLoadAudio()) {
            return;
        }

        if (this.javareforged$globalMusicLevel() != null && this.javareforged$globalMusicPlayer() != null) {
            this.javareforged$finishWorldLoadMusicState();
        }
    }

    @Redirect(
        method = "updateLevelInEngines(Lnet/minecraft/client/multiplayer/ClientLevel;Z)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/sounds/SoundManager;stop()V"
        )
    )
    private void javareforged$preventForcedLevelTransitionStop(SoundManager instance) {
        if (!MusicTransitionState.shouldProtectWorldLoadAudio()
            && !MusicTransitionState.shouldProtectForcedSoundStop()
            && !this.shouldFadeMusicOnLoad) {
            instance.stop();
        }
    }

    @Redirect(
        method = "doWorldLoad",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/Minecraft;disconnectWithProgressScreen()V"
        )
    )
    private void javareforged$preserveMusicDuringWorldLoadDisconnect(Minecraft minecraft) {
        this.javareforged$worldLoadInternalDisconnect = true;
        try {
            minecraft.disconnectWithProgressScreen();
        } finally {
            this.javareforged$worldLoadInternalDisconnect = false;
        }
    }

    @Unique
    private void javareforged$finishWorldLoadMusicState() {
        this.javareforged$releaseWorldLoadMusicProtection();
        this.shouldFadeMusicOnLoad = false;
    }

    @Unique
    private void javareforged$releaseWorldLoadMusicProtection() {
        MusicManagerBridge musicManagerBridge = (MusicManagerBridge) this.musicManager;
        musicManagerBridge.moods$setLoadingMode(false);
        musicManagerBridge.moods$setStopGraceTicks(0);
        MusicTransitionState.finishWorldLoad();
    }

    @Unique
    private @Nullable LocalPlayer javareforged$globalMusicPlayer() {
        LocalPlayer currentPlayer = this.player;
        ClientLevel currentLevel = this.level;
        return currentPlayer != null
            && currentLevel != null
            && currentPlayer.level() == currentLevel
            && !currentPlayer.isRemoved()
            ? currentPlayer
            : null;
    }

    @Unique
    private @Nullable ClientLevel javareforged$globalMusicLevel() {
        LocalPlayer currentPlayer = this.player;
        ClientLevel currentLevel = this.level;
        return currentPlayer != null
            && currentLevel != null
            && currentPlayer.level() == currentLevel
            && !currentPlayer.isRemoved()
            ? currentLevel
            : null;
    }
}
