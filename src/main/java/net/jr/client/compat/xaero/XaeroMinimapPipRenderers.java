package net.jr.client.compat.xaero;

import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import xaero.hud.minimap.render.MinimapPipRenderState;
import xaero.hud.minimap.render.MinimapPipRenderer;

public final class XaeroMinimapPipRenderers {
    private static final Map<MinimapPipRenderState, MinimapPipRenderer<?>> RENDERERS = new IdentityHashMap<>();
    private static final ThreadLocal<Boolean> ROUTING = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<MinimapPipRenderState> RENDERING_STATE = new ThreadLocal<>();

    private XaeroMinimapPipRenderers() {
    }

    public static boolean isRouting() {
        return ROUTING.get();
    }

    public static MinimapPipRenderState renderingStateOrNull() {
        return RENDERING_STATE.get();
    }

    public static void render(MinimapPipRenderState state, Runnable action) {
        MinimapPipRenderState previous = RENDERING_STATE.get();
        RENDERING_STATE.set(state);
        try {
            action.run();
        } finally {
            if (previous == null) {
                RENDERING_STATE.remove();
            } else {
                RENDERING_STATE.set(previous);
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void prepare(
        MinimapPipRenderState state,
        GuiRenderState guiRenderState,
        FeatureRenderDispatcher featureRenderDispatcher,
        int guiScale
    ) {
        MinimapPipRenderer renderer = RENDERERS.computeIfAbsent(
            state,
            key -> new MinimapPipRenderer(key.getClass())
        );
        ROUTING.set(true);
        try {
            renderer.prepare(state, guiRenderState, featureRenderDispatcher, guiScale);
        } finally {
            ROUTING.remove();
        }
    }

    public static void release(MinimapPipRenderState state) {
        MinimapPipRenderer<?> renderer = RENDERERS.remove(state);
        if (renderer == null) {
            return;
        }
        ROUTING.set(true);
        try {
            renderer.close();
        } finally {
            ROUTING.remove();
        }
    }

}
