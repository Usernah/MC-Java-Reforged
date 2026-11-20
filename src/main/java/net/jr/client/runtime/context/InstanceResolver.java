package net.jr.client.runtime.context;

import net.jr.api.client.local.Client;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.client.ClientInstance;
import net.jr.client.runtime.session.ClientSession;
import net.jr.client.runtime.slot.ClientSlots;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;

public final class InstanceResolver {
    private InstanceResolver() {
    }

    public static ClientInstance requireClient(int slotId) {
        return ClientRuntime.INSTANCE.clients().client(slotId);
    }

    public static ClientInstance requirePrimary() {
        return ClientRuntime.INSTANCE.clients().client(0);
    }

    public static ClientInstance requireActive() {
        return requireClient(SlotExecution.currentSlotId());
    }

    public static ClientInstance requireConnection(Connection connection) {
        return requireClient(ClientRuntime.INSTANCE.connections().requireSlot(connection));
    }

    public static ClientInstance requireListener(ClientPacketListener listener) {
        return requireClient(ClientRuntime.INSTANCE.clients().slotForClientPacketListener(listener));
    }

    public static SessionToken captureCurrentToken() {
        ClientInstance client = Client.current();
        ClientSession session = requireSession(client.slotId());
        return new SessionToken(client.slotId(), session.sessionId(), session.generation());
    }

    public static SessionToken captureScheduledTokenOrNull() {
        Integer scheduledSlotId = ScheduledSlot.currentOrNull();
        if (scheduledSlotId != null) {
            return ClientRuntime.INSTANCE.clients().hasClient(scheduledSlotId)
                ? tokenForSlot(scheduledSlotId)
                : null;
        }

        ClientInstance current = Client.currentOrNull();
        return current != null ? tokenForSlot(current.slotId()) : null;
    }

    public static ClientInstance requireToken(SessionToken token) {
        if (!isTokenValid(token)) {
            throw new IllegalStateException("Local client token is no longer valid: " + token);
        }
        return ClientRuntime.INSTANCE.clients().client(token.slotId());
    }

    public static boolean isTokenValid(SessionToken token) {
        if (
            token == null
                || token.slotId() < 0
                || token.slotId() >= ClientSlots.MAX_SLOTS
        ) {
            return false;
        }

        ClientSession session =
            ClientRuntime.INSTANCE.clients().sessionOrNull(token.slotId());

        return ClientRuntime.INSTANCE.clients().hasClient(token.slotId())
            && session != null
            && session.sessionId() == token.sessionId()
            && session.generation() == token.generation();
    }

    private static ClientSession requireSession(int slotId) {
        ClientSession session = ClientRuntime.INSTANCE.clients().sessionOrNull(slotId);
        if (session == null) {
            throw new IllegalStateException("No local client session exists for slot " + slotId);
        }
        return session;
    }

    private static SessionToken tokenForSlot(int slotId) {
        ClientSession session = requireSession(slotId);
        return new SessionToken(slotId, session.sessionId(), session.generation());
    }
}
