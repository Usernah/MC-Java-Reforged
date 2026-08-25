package net.jr.client.runtime.terrain;

import java.util.function.Supplier;

public final class SharedViewAreaConstruction {
    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);

    private SharedViewAreaConstruction() {
    }

    public static boolean active() {
        return DEPTH.get() > 0;
    }

    public static <T> T construct(Supplier<T> constructor) {
        DEPTH.set(DEPTH.get() + 1);
        try {
            return constructor.get();
        } finally {
            int remaining = DEPTH.get() - 1;
            if (remaining == 0) {
                DEPTH.remove();
            } else {
                DEPTH.set(remaining);
            }
        }
    }
}
