package net.jr.client.runtime.render.state;

import java.util.ArrayDeque;
import java.util.Deque;
import net.jr.client.runtime.context.LocalClientScope;
import net.jr.client.runtime.state.HandState;
import net.jr.mixin.runtime.ItemInHandRendererSSAccessor;
import net.minecraft.client.renderer.ItemInHandRenderer;

public final class HandStateScope implements AutoCloseable {
    private static final ThreadLocal<Deque<Binding>> BINDINGS = ThreadLocal.withInitial(ArrayDeque::new);

    private final ItemInHandRenderer renderer;
    private boolean closed;

    private HandStateScope(ItemInHandRenderer renderer) {
        this.renderer = renderer;
        ItemInHandRendererSSAccessor accessor = (ItemInHandRendererSSAccessor)renderer;
        HandState previousEngineState = new HandState();
        previousEngineState.capture(accessor);
        HandState clientState = LocalClientScope.currentClient().render().hands();
        clientState.install(accessor);
        BINDINGS.get().push(new Binding(renderer, clientState, previousEngineState));
    }

    public static HandStateScope enter(ItemInHandRenderer renderer) {
        return new HandStateScope(renderer);
    }

    @Override
    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        Deque<Binding> bindings = BINDINGS.get();
        if (bindings.isEmpty() || bindings.peek().renderer() != this.renderer) {
            throw new IllegalStateException("ItemInHandRenderer state scope is unbalanced");
        }
        Binding binding = bindings.pop();
        ItemInHandRendererSSAccessor accessor = (ItemInHandRendererSSAccessor)this.renderer;
        binding.clientState().capture(accessor);
        binding.previousEngineState().install(accessor);
        if (bindings.isEmpty()) {
            BINDINGS.remove();
        }
    }

    private record Binding(ItemInHandRenderer renderer, HandState clientState, HandState previousEngineState) {
    }
}
