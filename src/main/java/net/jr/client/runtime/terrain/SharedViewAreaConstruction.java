package net.jr.client.runtime.terrain;

import java.util.function.Supplier;

/**
 * Marks construction of a logical split-screen {@code ViewArea}.  Minecraft
 * 26.2 normally fills every ViewArea with its own RenderSection cycle.  The
 * split runtime owns those sections in {@link SharedTerrainStore}, so its
 * logical views deliberately build only the rotating storage metadata.
 */
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
