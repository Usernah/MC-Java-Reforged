package net.jr.client.sound.runtime;

import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.client.resources.sounds.TickableSoundInstance;

public final class PausedMusicState {
    private final Set<TickableSoundInstance> tickingSounds = new LinkedHashSet<>();

    public Set<TickableSoundInstance> tickingSounds() { return this.tickingSounds; }
    public void clear() { this.tickingSounds.clear(); }
}
