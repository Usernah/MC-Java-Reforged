package net.alnv.javareforged.ClientRuntime.runtime;

import java.util.ArrayDeque;
import java.util.Deque;
import net.alnv.javareforged.ClientRuntime.state.HandState;
import net.alnv.javareforged.mixin.SSM.ItemInHandRendererSSAccessor;
import net.minecraft.client.renderer.ItemInHandRenderer;

/** Binds one Client's hand data to the singleton ItemInHandRenderer. */
public final class Hands {
    private static final ThreadLocal<Deque<Binding>> BINDINGS = ThreadLocal.withInitial(ArrayDeque::new);

    private Hands() {
    }

    public static void begin(ItemInHandRenderer renderer) {
        ItemInHandRendererSSAccessor accessor = (ItemInHandRendererSSAccessor)renderer;
        HandState previousEngineState = new HandState();
        previousEngineState.capture(accessor);

        HandState clientState = Client.render().hands();
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
        for (int clientId = 0; clientId < Client.MAX_CLIENTS; clientId++) {
            if (!Client.connected(clientId) || Client.player(clientId) == null || Client.level(clientId) == null) {
                continue;
            }
            ClientBoundary.runForClient(clientId, renderer::tick);
        }
    }

    private record Binding(ItemInHandRenderer renderer, HandState clientState, HandState previousEngineState) {
    }
}
