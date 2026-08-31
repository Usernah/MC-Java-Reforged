package net.jr.client.runtime.render.experimental;

public interface ExperimentalRenderFeature {
    String id();

    default void enable() {
    }

    void disable();
}
