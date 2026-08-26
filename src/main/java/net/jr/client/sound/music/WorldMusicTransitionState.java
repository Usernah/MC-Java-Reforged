package net.jr.client.sound.music;

public final class WorldMusicTransitionState {
    private boolean fadeOnLoad;
    private boolean internalDisconnect;

    public boolean fadeOnLoad() { return this.fadeOnLoad; }
    public void setFadeOnLoad(boolean fade) { this.fadeOnLoad = fade; }
    public boolean internalDisconnect() { return this.internalDisconnect; }
    public void setInternalDisconnect(boolean internal) { this.internalDisconnect = internal; }
}
