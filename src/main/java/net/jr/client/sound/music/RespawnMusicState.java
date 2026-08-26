package net.jr.client.sound.music;

public final class RespawnMusicState {
    private boolean forcedStopProtected;
    private boolean musicStopProtected;

    public boolean forcedStopProtected() { return this.forcedStopProtected; }
    public void setForcedStopProtected(boolean protectedStop) { this.forcedStopProtected = protectedStop; }
    public boolean musicStopProtected() { return this.musicStopProtected; }
    public void setMusicStopProtected(boolean protectedStop) { this.musicStopProtected = protectedStop; }
}
