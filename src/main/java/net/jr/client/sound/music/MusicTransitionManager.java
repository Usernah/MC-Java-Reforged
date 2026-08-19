package net.jr.client.sound.music;

public final class MusicTransitionManager {
    private MusicTransitionManager() {
    }
    public static volatile boolean isTransitioning = false;
    public static volatile boolean worldLoadingActive = false;
    private static int forcedSoundStopProtectionDepth = 0;
    private static int respawnMusicStopProtectionDepth = 0;
    private static int musicContinuityProtectionTicks = 0;

    public static void beginWorldLoad() {
        worldLoadingActive = true;
        isTransitioning = true;
    }

    public static void finishWorldLoad() {
        worldLoadingActive = false;
        isTransitioning = false;
    }

    public static void clear() {
        finishWorldLoad();
        forcedSoundStopProtectionDepth = 0;
        respawnMusicStopProtectionDepth = 0;
        musicContinuityProtectionTicks = 0;
    }

    public static boolean shouldProtectWorldLoadAudio() {
        return worldLoadingActive;
    }

    public static void pushForcedSoundStopProtection() {
        forcedSoundStopProtectionDepth++;
    }

    public static void popForcedSoundStopProtection() {
        if (forcedSoundStopProtectionDepth > 0) {
            forcedSoundStopProtectionDepth--;
        }
    }

    public static boolean shouldProtectForcedSoundStop() {
        return forcedSoundStopProtectionDepth > 0;
    }

    public static void pushRespawnMusicStopProtection() {
        respawnMusicStopProtectionDepth++;
    }

    public static void popRespawnMusicStopProtection() {
        if (respawnMusicStopProtectionDepth > 0) {
            respawnMusicStopProtectionDepth--;
        }
    }

    public static boolean shouldProtectRespawnMusicStop() {
        return respawnMusicStopProtectionDepth > 0;
    }

    public static void requestMusicContinuityProtection(int ticks) {
        musicContinuityProtectionTicks = Math.max(musicContinuityProtectionTicks, ticks);
    }

    public static void clearMusicContinuityProtection() {
        musicContinuityProtectionTicks = 0;
    }

    public static void tickMusicContinuityProtection() {
        if (musicContinuityProtectionTicks > 0) {
            musicContinuityProtectionTicks--;
        }
    }

    public static boolean shouldProtectMusicContinuity() {
        return musicContinuityProtectionTicks > 0;
    }
}
