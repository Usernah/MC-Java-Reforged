package net.jr.client.runtime.render.state;

import java.util.ArrayDeque;
import java.util.Deque;

import net.jr.client.runtime.state.HudState;
import net.jr.mixin.accessors.HudAccessor;
import net.minecraft.client.gui.Hud;

public final class HudStateScope implements AutoCloseable {
    private static final ThreadLocal<Deque<Binding>> BINDINGS = ThreadLocal.withInitial(ArrayDeque::new);
    private final Hud hud;
    private boolean closed;

    private HudStateScope(Hud hud, HudState slotState) {
        this.hud = hud;
        HudAccessor accessor = (HudAccessor)hud;
        HudState previousEngineState = new HudState();
        previousEngineState.capture(accessor);
        slotState.install(accessor);
        BINDINGS.get().push(new Binding(hud, slotState, previousEngineState));
    }

    public static HudStateScope enter(Hud hud, HudState slotState) {
        return new HudStateScope(hud, slotState);
    }

    @Override
    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        Deque<Binding> bindings = BINDINGS.get();
        if (bindings.isEmpty() || bindings.peek().hud() != this.hud) {
            throw new IllegalStateException("Hud state scope is unbalanced");
        }
        Binding binding = bindings.pop();
        HudAccessor accessor = (HudAccessor)this.hud;
        binding.slotState().capture(accessor);
        binding.previousEngineState().install(accessor);
        if (bindings.isEmpty()) {
            BINDINGS.remove();
        }
    }

    private record Binding(Hud hud, HudState slotState, HudState previousEngineState) {
    }
}
