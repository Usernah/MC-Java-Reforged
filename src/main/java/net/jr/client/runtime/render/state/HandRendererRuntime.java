package net.jr.client.runtime.render.state;

import java.util.ArrayDeque;
import java.util.Deque;

import net.jr.client.runtime.context.LocalClientAcces;
import net.jr.client.runtime.context.LocalClientExecution;
import net.jr.client.runtime.state.HandState;
import net.jr.mixin.runtime.ItemInHandRendererSSAccessor;
import net.minecraft.client.renderer.ItemInHandRenderer;

/** Binds one Client's hand data to the singleton ItemInHandRenderer. */
public final class HandRendererRuntime {
    private static final ThreadLocal<Deque<Binding>> BINDINGS = ThreadLocal.withInitial(ArrayDeque::new);

    private HandRendererRuntime() {
    }

    public static void begin(ItemInHandRenderer renderer) {
        ItemInHandRendererSSAccessor accessor = (ItemInHandRendererSSAccessor)renderer;
        HandState previousEngineState = new HandState();
        previousEngineState.capture(accessor);

        HandState clientState = LocalClientAcces.render().hands();
        clientState.install(accessor);
        BINDINGS.get().push(new Binding(renderer, clientState, previousEngineState));
    }

    public static void end(ItemInHandRenderer renderer) {
        Deque<Binding> bindings = BINDINGS.get();
        if (bindings.isEmpty() || bindings.peek().renderer() != renderer) {
            throw new IllegalStateException("ItemInHandRenderer state scope is unbalanced");
        }
        Binding binding = bindings.pop();
        ItemInHandRendererSSAccessor accessor = (ItemInHandRendererSSAccessor)renderer;
        binding.clientState().capture(accessor);
        binding.previousEngineState().install(accessor);
        if (bindings.isEmpty()) {
            BINDINGS.remove();
        }
    }

    public static void tickConnectedClients(ItemInHandRenderer renderer) {
        for (int clientId = 0; clientId < LocalClientAcces.MAX_CLIENTS; clientId++) {
            if (!LocalClientAcces.connected(clientId) || LocalClientAcces.player(clientId) == null || LocalClientAcces.level(clientId) == null) {
                continue;
            }
            LocalClientExecution.runForClient(clientId, renderer::tick);
        }
    }

    private record Binding(ItemInHandRenderer renderer, HandState clientState, HandState previousEngineState) {
    }
}
