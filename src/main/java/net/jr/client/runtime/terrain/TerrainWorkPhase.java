package net.jr.client.runtime.terrain;

public final class TerrainWorkPhase {
    private static final ThreadLocal<Boolean> UPDATE_ALLOWED = ThreadLocal.withInitial(() -> Boolean.TRUE);

    private TerrainWorkPhase() {
    }

    public static Scope update() {
        return set(true);
    }

    public static Scope drawOnly() {
        return set(false);
    }

    public static boolean canUpdateTerrain() {
        return UPDATE_ALLOWED.get();
    }

    private static Scope set(boolean updateAllowed) {
        boolean previous = UPDATE_ALLOWED.get();
        UPDATE_ALLOWED.set(updateAllowed);
        return () -> UPDATE_ALLOWED.set(previous);
    }

    @FunctionalInterface
    public interface Scope extends AutoCloseable {
        @Override
        void close();
    }
}
