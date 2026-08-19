package net.jr.client.runtime.render.state;

import java.util.ArrayDeque;
import java.util.Deque;
import net.jr.client.runtime.context.LocalClientScope;
import net.jr.client.runtime.state.HudState;
import net.jr.mixin.runtime.HudSSAccessor;
import net.minecraft.client.gui.Hud;

public final class HudStateScope implements AutoCloseable {
    private static final ThreadLocal<Deque<Binding>> BINDINGS = ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<Deque<HudStateScope>> ACTIVE_SCOPES = ThreadLocal.withInitial(ArrayDeque::new);

    private final Hud hud;
    private boolean closed;

    private HudStateScope(Hud hud) {
        this.hud = hud;
        HudSSAccessor accessor = (HudSSAccessor)hud;
        HudState previousEngineState = new HudState();
        previousEngineState.capture(accessor);
        HudState clientState = LocalClientScope.currentClient().render().hud();
        clientState.install(accessor);
        BINDINGS.get().push(new Binding(hud, clientState, previousEngineState));
    }

    public static HudStateScope enter(Hud hud) {
        return new HudStateScope(hud);
    }

    public static void begin(Hud hud) {
        ACTIVE_SCOPES.get().push(enter(hud));
    }

    public static void end(Hud hud) {
        Deque<HudStateScope> scopes = ACTIVE_SCOPES.get();
        if (scopes.isEmpty() || scopes.peek().hud != hud) {
            throw new IllegalStateException("Hud state scope is unbalanced");
        }
        HudStateScope scope = scopes.pop();
        scope.close();
        if (scopes.isEmpty()) {
            ACTIVE_SCOPES.remove();
        }
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
        HudSSAccessor accessor = (HudSSAccessor)this.hud;
        binding.clientState().capture(accessor);
        binding.previousEngineState().install(accessor);
        if (bindings.isEmpty()) {
            BINDINGS.remove();
        }
    }

    private record Binding(Hud hud, HudState clientState, HudState previousEngineState) {
    }
}
