package net.jr.client.runtime.render.world;

import java.util.List;

public interface WorldRenderBackend {
    String id();

    void beginExtractionFrame(List<WorldRenderView> views);

    void beginRenderFrame(List<WorldRenderView> views);

    default Scope enterExtractionView(WorldRenderView view) {
        return Scope.NOOP;
    }

    default Scope enterRenderView(WorldRenderView view) {
        return Scope.NOOP;
    }

    @FunctionalInterface
    interface Scope extends AutoCloseable {
        Scope NOOP = () -> {
        };

        @Override
        void close();
    }
}
