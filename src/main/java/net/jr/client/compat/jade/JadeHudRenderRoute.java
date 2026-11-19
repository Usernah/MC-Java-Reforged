package net.jr.client.compat.jade;

public final class JadeHudRenderRoute {
    private static final ThreadLocal<Boolean> REGISTERED_RENDER = new ThreadLocal<>();

    private JadeHudRenderRoute() {
    }

    public static boolean isRegisteredRender() {
        return Boolean.TRUE.equals(REGISTERED_RENDER.get());
    }

    public static void run(Runnable render) {
        Boolean previous = REGISTERED_RENDER.get();
        REGISTERED_RENDER.set(Boolean.TRUE);
        try {
            render.run();
        } finally {
            if (previous == null) {
                REGISTERED_RENDER.remove();
            } else {
                REGISTERED_RENDER.set(previous);
            }
        }
    }
}
