package net.jr.client.sound.runtime;

public final class StreamingChannelState {
    private boolean boosted;
    private int buffersToFill;
    private boolean recoveryAllowed;
    private int recoveryTicks;

    public boolean boosted() { return this.boosted; }
    public void markBoosted() { this.boosted = true; }
    public int buffersToFill() { return this.buffersToFill; }
    public void setBuffersToFill(int count) { this.buffersToFill = count; }
    public boolean recoveryAllowed() { return this.recoveryAllowed; }
    public void setRecoveryAllowed(boolean allowed) { this.recoveryAllowed = allowed; }
    public int recoveryTicks() { return this.recoveryTicks; }
    public void setRecoveryTicks(int ticks) { this.recoveryTicks = ticks; }
    public int consumeRecoveryTick() { return this.recoveryTicks--; }
}
