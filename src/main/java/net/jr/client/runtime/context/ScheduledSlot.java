package net.jr.client.runtime.context;

import javax.annotation.Nullable;
import net.jr.client.runtime.slot.ClientSlots;

public final class ScheduledSlot {
    private static final ThreadLocal<Integer> ACTIVE = new ThreadLocal<>();

    private ScheduledSlot() {
    }

    public static Binding bind(int slotId) {
        ClientSlots.requireSlotId(slotId);
        Integer previous = ACTIVE.get();
        ACTIVE.set(slotId);
        return new Binding(slotId, previous);
    }

    @Nullable
    public static Integer currentOrNull() {
        return ACTIVE.get();
    }

    public static final class Binding implements AutoCloseable {
        private final int installed;
        @Nullable
        private final Integer previous;
        private boolean closed;

        private Binding(int installed, @Nullable Integer previous) {
            this.installed = installed;
            this.previous = previous;
        }

        @Override
        public void close() {
            if (this.closed) {
                return;
            }
            Integer current = ACTIVE.get();
            if (current == null || current != this.installed) {
                throw new IllegalStateException("Scheduled slot binding is unbalanced");
            }
            this.closed = true;
            if (this.previous == null) {
                ACTIVE.remove();
            } else {
                ACTIVE.set(this.previous);
            }
        }
    }
}
