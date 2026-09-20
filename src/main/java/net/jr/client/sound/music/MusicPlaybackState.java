package net.jr.client.sound.music;

public final class MusicPlaybackState {
    int stopGraceTicks;
    boolean loadingMode;
    boolean fadeInNextTrack;
    boolean configuredStartDelayArmed;
    boolean resumeAfterWorldExit;
    boolean initialPlayback = true;
    boolean worldExitIntervalArmed;
    MusicPlaybackPolicy.Selection appliedPolicy = MusicPlaybackPolicy.VANILLA;
    int overrideRetryTicks;
}
