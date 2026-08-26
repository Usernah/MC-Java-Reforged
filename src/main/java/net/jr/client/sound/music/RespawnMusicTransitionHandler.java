package net.jr.client.sound.music;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class RespawnMusicTransitionHandler {
    private RespawnMusicTransitionHandler() {
    }

    public static void begin(Minecraft minecraft, ClientboundRespawnPacket packet, RespawnMusicState state) {
        boolean preserve = shouldPreserve(minecraft, packet);
        state.setForcedStopProtected(preserve);
        state.setMusicStopProtected(preserve);
        if (state.forcedStopProtected()) {
            MusicTransitionManager.pushForcedSoundStopProtection();
            MusicTransitionManager.requestMusicContinuityProtection(200);
        }
        if (state.musicStopProtected()) MusicTransitionManager.pushRespawnMusicStopProtection();
    }

    public static void finish(RespawnMusicState state) {
        if (state.forcedStopProtected()) {
            MusicTransitionManager.popForcedSoundStopProtection();
            state.setForcedStopProtected(false);
        }
        if (state.musicStopProtected()) {
            MusicTransitionManager.popRespawnMusicStopProtection();
            state.setMusicStopProtected(false);
        }
    }

    public static void stopUnlessProtected(MusicManager musicManager) {
        if (!MusicTransitionManager.shouldProtectRespawnMusicStop()) musicManager.stopPlaying();
    }

    private static boolean shouldPreserve(Minecraft minecraft, ClientboundRespawnPacket packet) {
        LocalPlayer player = minecraft.player;
        if (player == null || player.level() == null) return true;
        ResourceKey<Level> targetDimension = packet.commonPlayerSpawnInfo().dimension();
        return player.isDeadOrDying() || targetDimension == player.level().dimension();
    }
}
