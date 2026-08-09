package net.jr.ClientRuntime.runtime;

import java.util.Objects;
import java.util.function.Supplier;
import net.jr.ClientRuntime.slot.PlayerSlots;

public final class ActiveSlot {
    private static final ThreadLocal<Integer> ACTIVE_SLOT_ID = new ThreadLocal<>();
    private static final ThreadLocal<Integer> SCHEDULED_SLOT_ID = new ThreadLocal<>();

    private ActiveSlot() {
    }

    public static Scope enter(int slotId) {
        requireValid(slotId);
        Integer previous = ACTIVE_SLOT_ID.get();
        ACTIVE_SLOT_ID.set(slotId);
        return new Scope(slotId, previous);
    }

    public static void run(int slotId, Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable");
        try (Scope ignored = enter(slotId)) {
            runnable.run();
        }
    }

    public static Runnable wrap(int slotId, Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable");
        requireValid(slotId);
        return () -> run(slotId, runnable);
    }

    public static <T> T call(int slotId, Supplier<T> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        try (Scope ignored = enter(slotId)) {
            return supplier.get();
        }
    }

    public static int requireId() {
        Integer slotId = ACTIVE_SLOT_ID.get();
        if (slotId == null) {
            throw new IllegalStateException("No active player slot is bound to this execution path");
        }
        requireValid(slotId);
        return slotId;
    }

    public static Integer idOrNull() {
        return ACTIVE_SLOT_ID.get();
    }

    public static Scheduling schedule(int slotId) {
        requireValid(slotId);
        Integer previous = SCHEDULED_SLOT_ID.get();
        SCHEDULED_SLOT_ID.set(slotId);
        return new Scheduling(slotId, previous);
    }

    public static Integer scheduledIdOrNull() {
        return SCHEDULED_SLOT_ID.get();
    }

    private static void requireValid(int slotId) {
        if (slotId < 0 || slotId >= PlayerSlots.MAX_SLOTS) {
            throw new IndexOutOfBoundsException("Invalid player slot id " + slotId);
        }
    }

    public static final class Scope implements AutoCloseable {
        private final int installed;
        private final Integer previous;
        private boolean closed;

        private Scope(int installed, Integer previous) {
            this.installed = installed;
            this.previous = previous;
        }

        @Override
        public void close() {
            if (this.closed) {
                return;
            }
            requireCurrent(ACTIVE_SLOT_ID, this.installed, "active");
            this.closed = true;
            if (this.previous == null) {
                ACTIVE_SLOT_ID.remove();
            } else {
                ACTIVE_SLOT_ID.set(this.previous);
            }
        }
    }

    public static final class Scheduling implements AutoCloseable {
        private final int installed;
        private final Integer previous;
        private boolean closed;

        private Scheduling(int installed, Integer previous) {
            this.installed = installed;
            this.previous = previous;
        }

        @Override
        public void close() {
            if (this.closed) {
                return;
            }
            requireCurrent(SCHEDULED_SLOT_ID, this.installed, "scheduled");
            this.closed = true;
            if (this.previous == null) {
                SCHEDULED_SLOT_ID.remove();
            } else {
                SCHEDULED_SLOT_ID.set(this.previous);
            }
        }
    }

    private static void requireCurrent(ThreadLocal<Integer> storage, int expected, String kind) {
        Integer current = storage.get();
        if (current == null || current != expected) {
            throw new IllegalStateException(
                "Unbalanced " + kind + " slot scope: expected " + expected + ", found " + current
            );
        }
    }

    public static void bootstrapPrimary() {
        Integer current = ACTIVE_SLOT_ID.get();
        if (current != null) {
            throw new IllegalStateException(
                    "Primary slot bootstrap was already installed: " + current
            );
        }

        ACTIVE_SLOT_ID.set(0);
    }
}
