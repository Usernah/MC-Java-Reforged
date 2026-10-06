package net.jr.client.compat.rpgseries;

import java.util.Map;
import java.util.WeakHashMap;

import net.jr.api.client.local.Client;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.client.ClientInstance;
import net.jr.client.runtime.context.InstanceExecution;
import net.jr.mixin.compat.rpgseries.HudMessagesAccessor;
import net.spell_engine.client.gui.HudMessages;
import net.spell_engine.client.input.SpellHotbar;

public final class SpellEngineRuntimeState {
    private static final SpellHotbar PRIMARY_HOTBAR = SpellHotbar.INSTANCE;
    private static final HudMessages PRIMARY_MESSAGES = HudMessages.INSTANCE;
    private static final Map<ClientInstance, State> STATES = new WeakHashMap<>();

    private SpellEngineRuntimeState() {
    }

    public static SpellHotbar hotbar() {
        return currentState().hotbar;
    }

    public static HudMessages messages() {
        return currentState().messages;
    }

    public static Scope activateCurrent() {
        return activate(currentState());
    }

    public static Scope activatePrimary() {
        return activate(state(ClientRuntime.INSTANCE.clients().client(0)));
    }

    public static void tickSecondaryMessages() {
        for (ClientInstance client : ClientRuntime.INSTANCE.clients().all()) {
            if (client.isPrimary()) {
                continue;
            }
            InstanceExecution.runForClient(client.slotId(), () -> messages().tick());
        }
    }

    private static Scope activate(State state) {
        SpellHotbar previousHotbar = SpellHotbar.INSTANCE;
        HudMessages previousMessages = HudMessages.INSTANCE;
        SpellHotbar.INSTANCE = state.hotbar;
        HudMessagesAccessor.javaReforged$setInstance(state.messages);
        return new Scope(previousHotbar, previousMessages);
    }

    private static State currentState() {
        ClientInstance client = Client.currentOrNull();
        if (client == null) {
            client = ClientRuntime.INSTANCE.clients().client(0);
        }
        return state(client);
    }

    private static State state(ClientInstance client) {
        synchronized (STATES) {
            return STATES.computeIfAbsent(client, State::new);
        }
    }

    public static final class Scope implements AutoCloseable {
        private final SpellHotbar previousHotbar;
        private final HudMessages previousMessages;
        private boolean closed;

        private Scope(SpellHotbar previousHotbar, HudMessages previousMessages) {
            this.previousHotbar = previousHotbar;
            this.previousMessages = previousMessages;
        }

        @Override
        public void close() {
            if (this.closed) {
                return;
            }
            this.closed = true;
            SpellHotbar.INSTANCE = this.previousHotbar;
            HudMessagesAccessor.javaReforged$setInstance(this.previousMessages);
        }
    }

    private static final class State {
        private final SpellHotbar hotbar;
        private final HudMessages messages;

        private State(ClientInstance client) {
            this.hotbar = client.isPrimary() ? PRIMARY_HOTBAR : new SpellHotbar();
            this.messages = client.isPrimary() ? PRIMARY_MESSAGES : new HudMessages();
        }
    }
}
