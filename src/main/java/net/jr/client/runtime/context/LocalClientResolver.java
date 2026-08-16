package net.jr.client.runtime.context;

import net.jr.client.input.InputApi;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.session.LocalClientSession;
import net.jr.client.runtime.slot.LocalClientSlot;
import net.jr.client.runtime.slot.LocalClientSlots;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;

/** Slot resolution policy used only by {@link LocalClientExecution}. */
final class LocalClientResolver {
    private LocalClientResolver() {
    }

    static LocalClientSlot requireSlot(LocalClientSlot slot) {
        if (slot == null) {
            throw new IllegalArgumentException("slot cannot be null");
        }
        return ClientRuntime.INSTANCE.slots().slot(slot.id());
    }

    static LocalClientSlot requirePrimary() {
        return ClientRuntime.INSTANCE.primarySlot();
    }

    static LocalClientSlot requireActive() {
        return ClientRuntime.INSTANCE.slots().slot(ActiveClientSlot.requireId());
    }

    static LocalClientSlot requireConnection(Connection connection) {
        return ClientRuntime.INSTANCE.slots().slot(ClientRuntime.INSTANCE.connections().requireSlot(connection));
    }

    static LocalClientSlot requireListener(ClientPacketListener listener) {
        return ClientRuntime.INSTANCE.slots().slot(ClientRuntime.INSTANCE.slotForClientPacketListener(listener));
    }

    static LocalClientSlot requireScreen(Screen screen) {
        if (screen == null) {
            throw new IllegalArgumentException("screen cannot be null");
        }

        LocalClientSlot match = null;
        for (int slotId = 0; slotId < LocalClientSlots.MAX_SLOTS; slotId++) {
            LocalClientSlot candidate = ClientRuntime.INSTANCE.slots().slot(slotId);
            if (candidate.screenState().screen() != screen) {
                continue;
            }
            if (match != null) {
                throw new IllegalStateException("Screen is bound to more than one local client: " + screen);
            }
            match = candidate;
        }
        if (match == null) {
            throw new IllegalStateException("Screen is not bound to any local client: " + screen);
        }
        return match;
    }

    static LocalClientSlot requireKeyboardMouseOwner() {
        return ClientRuntime.INSTANCE.slots().slot(InputApi.keyboardMouseClientId());
    }

    static LocalClientToken captureCurrentToken() {
        LocalClient client = LocalClientAcces.current();
        LocalClientSession session = requireSession(client.slotId());
        return new LocalClientToken(client.slotId(), session.sessionId(), session.generation());
    }

    static LocalClientToken captureScheduledTokenOrNull() {
        Integer scheduledSlotId = ActiveClientSlot.scheduledIdOrNull();
        if (scheduledSlotId != null) {
            return tokenForSlot(scheduledSlotId);
        }
        LocalClient current = LocalClientAcces.currentOrNull();
        if (current != null) {
            return tokenForSlot(current.slotId());
        }
        return null;
    }

    static LocalClientSlot requireToken(LocalClientToken token) {
        if (!isTokenValid(token)) {
            throw new IllegalStateException("Client token is no longer valid: " + token);
        }
        return ClientRuntime.INSTANCE.slots().slot(token.slotId());
    }

    static boolean isTokenValid(LocalClientToken token) {
        if (token == null || token.slotId() < 0 || token.slotId() >= LocalClientSlots.MAX_SLOTS) {
            return false;
        }
        LocalClientSession session = ClientRuntime.INSTANCE.sessions().sessionOrNull(token.slotId());
        return session != null
            && session.sessionId() == token.sessionId()
            && session.generation() == token.generation()
            && ClientRuntime.INSTANCE.slots().slot(token.slotId()).connected();
    }

    private static LocalClientSession requireSession(int slotId) {
        LocalClientSession session = ClientRuntime.INSTANCE.sessions().sessionOrNull(slotId);
        if (session == null) {
            throw new IllegalStateException("No client session exists for slot " + slotId);
        }
        return session;
    }

    private static LocalClientToken tokenForSlot(int slotId) {
        LocalClientSession session = requireSession(slotId);
        return new LocalClientToken(slotId, session.sessionId(), session.generation());
    }
}
