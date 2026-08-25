package net.jr.client.render;

import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;

import java.util.IdentityHashMap;
import java.util.Map;

public final class ItemRenderResolution {
    private static final int VANILLA_ITEM_SIZE = 16;

    private static final ThreadLocal<Integer> ACTIVE_SIZE =
            ThreadLocal.withInitial(() -> VANILLA_ITEM_SIZE);
    private static final Map<GuiItemRenderState, Integer> STATE_SIZES =
            new IdentityHashMap<>();

    private static int frameItemSize = VANILLA_ITEM_SIZE;

    private ItemRenderResolution() {}

    public static void drawAtSize(int size, Runnable drawCall) {
        int previous = ACTIVE_SIZE.get();
        ACTIVE_SIZE.set(Math.max(VANILLA_ITEM_SIZE, size));
        try {
            drawCall.run();
        } finally {
            ACTIVE_SIZE.set(previous);
        }
    }

    public static GuiItemRenderState capture(GuiItemRenderState state) {
        int size = ACTIVE_SIZE.get();
        if (size > VANILLA_ITEM_SIZE) {
            STATE_SIZES.put(state, size);
        }
        return state;
    }

    public static void prepareFrame(GuiRenderState renderState) {
        frameItemSize = VANILLA_ITEM_SIZE;
        renderState.forEachItem(state -> frameItemSize = Math.max(
                frameItemSize,
                STATE_SIZES.getOrDefault(state, VANILLA_ITEM_SIZE)
        ));
    }

    public static int atlasSlotSize(int vanillaSlotSize) {
        if (frameItemSize <= VANILLA_ITEM_SIZE) {
            return vanillaSlotSize;
        }
        return Math.max(
                vanillaSlotSize,
                (int)Math.ceil((double)vanillaSlotSize * frameItemSize / VANILLA_ITEM_SIZE)
        );
    }

    public static int preparedItemSize() {
        return frameItemSize;
    }

    public static void clearFrame() {
        STATE_SIZES.clear();
        frameItemSize = VANILLA_ITEM_SIZE;
    }
}
