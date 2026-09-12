package net.jr.client.sound.music;

import net.jr.client.runtime.client.LocalClient;
import net.jr.client.runtime.client.LocalClientReadinessPolicy;
import net.jr.client.runtime.context.LocalClientScope;
import net.jr.client.sound.bridge.MusicManagerBridge;
import net.jr.client.sound.bridge.SoundEngineBridge;
import net.jr.client.sound.config.SoundTransitionConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.client.sounds.SoundManager;
import org.jetbrains.annotations.Nullable;

public final class WorldMusicTransitionHandler {
    private WorldMusicTransitionHandler() {
    }

    public static void worldLoadStarted(MusicManager musicManager, WorldMusicTransitionState state) {
        MusicManagerBridge bridge = (MusicManagerBridge) musicManager;
        bridge.javaReforged$setLoadingMode(true);
        bridge.javaReforged$setStopGraceTicks(Math.max(20, SoundTransitionConfig.fadeOutTicks));
        bridge.javaReforged$fadeOutCurrentMusic();
        state.setFadeOnLoad(false);
        MusicTransitionManager.beginWorldLoad();
    }

    public static void levelSet(Minecraft minecraft, MusicManager musicManager, WorldMusicTransitionState state) {
        LocalClient client = LocalClientScope.currentClientOrNull();
        if (client == null || client.slotId() == 0) finish(minecraft, musicManager, state);
    }

    public static void disconnectStarted(
            Minecraft minecraft,
            MusicManager musicManager,
            SoundManager soundManager,
            WorldMusicTransitionState state
    ) {
        if (MusicTransitionManager.shouldProtectWorldLoadAudio() && state.internalDisconnect()) {
            state.setFadeOnLoad(false);
            return;
        }
        if (globalLevel(minecraft) != null) {
            SoundEngineBridge bridge = (SoundEngineBridge) soundManager;
            state.setFadeOnLoad(true);
            bridge.javaReforged$clearQueued();
            bridge.javaReforged$fadeSounds((float) SoundTransitionConfig.fadeOutTicks);
        } else {
            state.setFadeOnLoad(false);
        }
        release(musicManager);
    }

    public static void disconnectFinished(MusicManager musicManager, WorldMusicTransitionState state) {
        state.setFadeOnLoad(false);
        if (state.internalDisconnect()) return;
        MusicTransitionManager.clear();
        ((MusicManagerBridge) musicManager).javaReforged$resetAfterWorldExit();
    }

    public static void tick(Minecraft minecraft, MusicManager musicManager, WorldMusicTransitionState state) {
        if (MusicTransitionManager.shouldProtectWorldLoadAudio()
                && LocalClientReadinessPolicy.worldBound(0)) {
            finish(minecraft, musicManager, state);
        }
    }

    public static boolean allowForcedLevelStop(WorldMusicTransitionState state) {
        LocalClient client = LocalClientScope.currentClientOrNull();
        if (client != null && client.slotId() != 0) return false;
        return !MusicTransitionManager.shouldProtectWorldLoadAudio()
                && !MusicTransitionManager.shouldProtectForcedSoundStop()
                && !state.fadeOnLoad();
    }

    public static void runInternalDisconnect(WorldMusicTransitionState state, Runnable disconnect) {
        state.setInternalDisconnect(true);
        try {
            disconnect.run();
        } finally {
            state.setInternalDisconnect(false);
        }
    }

    private static void finish(Minecraft minecraft, MusicManager musicManager, WorldMusicTransitionState state) {
        release(musicManager);
        state.setFadeOnLoad(false);
    }

    private static void release(MusicManager musicManager) {
        MusicManagerBridge bridge = (MusicManagerBridge) musicManager;
        bridge.javaReforged$setLoadingMode(false);
        bridge.javaReforged$setStopGraceTicks(0);
        MusicTransitionManager.finishWorldLoad();
    }

    private static @Nullable LocalPlayer globalPlayer(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        return player != null && level != null && player.level() == level && !player.isRemoved() ? player : null;
    }

    private static @Nullable ClientLevel globalLevel(Minecraft minecraft) {
        return globalPlayer(minecraft) != null ? minecraft.level : null;
    }
}
