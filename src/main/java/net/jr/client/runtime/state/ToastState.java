package net.jr.client.runtime.state;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.sounds.SoundEvent;

public final class ToastState {
    private final List<Object> visibleToasts = new ArrayList<>();
    private final BitSet occupiedSlots = new BitSet();
    private final Deque<Toast> queued = new ArrayDeque<>();
    private final Set<SoundEvent> playedToastSounds = new HashSet<>();

    public List<Object> visibleToasts() {
        return this.visibleToasts;
    }

    public BitSet occupiedSlots() {
        return this.occupiedSlots;
    }

    public Deque<Toast> queued() {
        return this.queued;
    }

    public Set<SoundEvent> playedToastSounds() {
        return this.playedToastSounds;
    }

    public void drainInto(ToastState target) {
        if (target == this) {
            return;
        }

        target.visibleToasts.addAll(0, this.visibleToasts);
        target.occupiedSlots.or(this.occupiedSlots);

        while (!this.queued.isEmpty()) {
            target.queued.addFirst(this.queued.removeLast());
        }

        target.playedToastSounds.addAll(this.playedToastSounds);
        this.clear();
    }

    public void clear() {
        this.visibleToasts.clear();
        this.occupiedSlots.clear();
        this.queued.clear();
        this.playedToastSounds.clear();
    }
}