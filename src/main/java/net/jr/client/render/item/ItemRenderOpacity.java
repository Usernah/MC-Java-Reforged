package net.jr.client.render.item;

import net.minecraft.client.renderer.state.gui.GuiItemRenderState;

import java.util.Map;
import java.util.WeakHashMap;

public final class ItemRenderOpacity {
    private static final ThreadLocal<Float> ACTIVE_ALPHA = ThreadLocal.withInitial(() -> 1.0F);
    private static final ThreadLocal<Map<GuiItemRenderState, Integer>> STATE_COLORS =
        ThreadLocal.withInitial(WeakHashMap::new);

    private ItemRenderOpacity() {}

    public static void draw(float alpha, Runnable drawCall) {
        float previous = ACTIVE_ALPHA.get();
        ACTIVE_ALPHA.set(Math.max(0.0F, Math.min(alpha, 1.0F)));
        try {
            drawCall.run();
        } finally {
            ACTIVE_ALPHA.set(previous);
        }
    }

    public static GuiItemRenderState capture(GuiItemRenderState state) {
        float alpha = ACTIVE_ALPHA.get();
        if (alpha < 1.0F) {
            int alphaByte = Math.round(alpha * 255.0F);

            STATE_COLORS.get().put(
                    state,
                    (alphaByte << 24) | (alphaByte << 16) | (alphaByte << 8) | alphaByte
            );
        }
        return state;
    }

    public static int consumeColor(GuiItemRenderState state) {
        Map<GuiItemRenderState, Integer> colors = STATE_COLORS.get();
        Integer color = colors.remove(state);
        if (colors.isEmpty()) {
            STATE_COLORS.remove();
        }
        return color == null ? -1 : color;
    }

    public static void clearFrame() {
        Map<GuiItemRenderState, Integer> colors = STATE_COLORS.get();
        if (colors.isEmpty()) {
            STATE_COLORS.remove();
        }
    }
}
