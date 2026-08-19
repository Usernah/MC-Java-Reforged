package net.jr.client.runtime.context;

import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.client.LocalClient;
import net.jr.client.runtime.session.LocalClientSession;
import net.jr.client.runtime.slot.LocalClientSlotRegistry;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;

public final class LocalClientResolver {
    private LocalClientResolver() {
    }

    public static LocalClient requireClient(int slotId) {
        return ClientRuntime.INSTANCE.clients().client(slotId);
    }

    public static LocalClient requirePrimary() {
        return ClientRuntime.INSTANCE.clients().client(0);
    }

    public static LocalClient requireActive() {
        return requireClient(SlotScope.requireId());
    }

    public static LocalClient requireConnection(Connection connection) {
        return requireClient(ClientRuntime.INSTANCE.connections().requireSlot(connection));
    }

    public static LocalClient requireListener(ClientPacketListener listener) {
        return requireClient(ClientRuntime.INSTANCE.clients().slotForClientPacketListener(listener));
    }

    public static LocalClientToken captureCurrentToken() {
        LocalClient client = LocalClientScope.currentClient();
        LocalClientSession session = requireSession(client.slotId());
        return new LocalClientToken(client.slotId(), session.sessionId(), session.generation());
    }

    public static LocalClientToken captureScheduledTokenOrNull() {
        Integer scheduledSlotId = SlotScope.scheduledIdOrNull();
        if (scheduledSlotId != null) {
            return ClientRuntime.INSTANCE.clients().hasClient(scheduledSlotId)
                ? tokenForSlot(scheduledSlotId)
                : null;
        }

        LocalClient current = LocalClientScope.currentClientOrNull();
        return current != null ? tokenForSlot(current.slotId()) : null;
    }

    public static LocalClient requireToken(LocalClientToken token) {
        if (!isTokenValid(token)) {
            throw new IllegalStateException("Local client token is no longer valid: " + token);
        }
        return ClientRuntime.INSTANCE.clients().client(token.slotId());
    }

    public static boolean isTokenValid(LocalClientToken token) {
        if (
            token == null
                || token.slotId() < 0
                || token.slotId() >= LocalClientSlotRegistry.MAX_SLOTS
        ) {
            return false;
        }

        LocalClientSession session =
            ClientRuntime.INSTANCE.clients().sessionOrNull(token.slotId());

        return ClientRuntime.INSTANCE.clients().hasClient(token.slotId())
            && session != null
            && session.sessionId() == token.sessionId()
            && session.generation() == token.generation();
    }

    private static LocalClientSession requireSession(int slotId) {
        LocalClientSession session = ClientRuntime.INSTANCE.clients().sessionOrNull(slotId);
        if (session == null) {
            throw new IllegalStateException("No local client session exists for slot " + slotId);
        }
        return session;
    }

    private static LocalClientToken tokenForSlot(int slotId) {
        LocalClientSession session = requireSession(slotId);
        return new LocalClientToken(slotId, session.sessionId(), session.generation());
    }
}
