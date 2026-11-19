package net.jr.client.render.item;

import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.WeakHashMap;

public final class ItemRenderResolution {
    public static final int VANILLA_ITEM_SIZE = 16;

    private static final Request DEFAULT_REQUEST = new Request(VANILLA_ITEM_SIZE, Kind.SLOT);
    private static final ThreadLocal<Request> ACTIVE_REQUEST =
        ThreadLocal.withInitial(() -> DEFAULT_REQUEST);
    private static final ThreadLocal<Map<GuiItemRenderState, Request>> STATE_REQUESTS =
        ThreadLocal.withInitial(WeakHashMap::new);

    private ItemRenderResolution() {
    }

    public static void drawAtSize(int size, Runnable drawCall) {
        draw(new Request(requirePositive(size), Kind.SLOT), drawCall);
    }

    public static void drawCarriedAtSize(int size, Runnable drawCall) {
        draw(new Request(requirePositive(size), Kind.CARRIED), drawCall);
    }

    private static void draw(Request request, Runnable drawCall) {
        Request previous = ACTIVE_REQUEST.get();
        ACTIVE_REQUEST.set(request);
        try {
            drawCall.run();
        } finally {
            ACTIVE_REQUEST.set(previous);
        }
    }

    public static GuiItemRenderState capture(GuiItemRenderState state) {
        Request request = ACTIVE_REQUEST.get();
        if (!request.equals(DEFAULT_REQUEST)) {
            STATE_REQUESTS.get().put(state, request);
        }
        return state;
    }

    public static Map<GuiItemRenderState, Request> collect(GuiRenderState renderState) {
        Map<GuiItemRenderState, Request> pending = STATE_REQUESTS.get();
        Map<GuiItemRenderState, Request> requests = new IdentityHashMap<>();
        renderState.forEachItem(state -> {
            Request request = pending.remove(state);
            if (request != null) {
                requests.put(state, request);
            }
        });
        if (pending.isEmpty()) {
            STATE_REQUESTS.remove();
        }
        return requests;
    }

    public static Request requestFor(
        Map<GuiItemRenderState, Request> requests,
        GuiItemRenderState state
    ) {
        return requests.getOrDefault(state, DEFAULT_REQUEST);
    }

    private static int requirePositive(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Item render size must be positive");
        }
        return size;
    }

    public enum Kind {
        SLOT,
        CARRIED
    }

    public record Request(int size, Kind kind) {
    }
}
