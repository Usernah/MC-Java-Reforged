package net.jr.ClientRuntime.runtime;

import net.jr.client.input.InputApi;
import net.jr.ClientRuntime.player.PlayerSession;
import net.jr.ClientRuntime.slot.PlayerSlot;
import net.jr.ClientRuntime.slot.PlayerSlots;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;

/** Slot resolution policy used only by {@link ClientBoundary}. */
final class ClientResolver {
    private ClientResolver() {
    }

    static PlayerSlot requireSlot(PlayerSlot slot) {
        if (slot == null) {
            throw new IllegalArgumentException("slot cannot be null");
        }
        return LocalPlayers.INSTANCE.slots().slot(slot.id());
    }

    static PlayerSlot requirePrimary() {
        return LocalPlayers.INSTANCE.primarySlot();
    }

    static PlayerSlot requireActive() {
        return LocalPlayers.INSTANCE.slots().slot(ActiveSlot.requireId());
    }

    static PlayerSlot requireConnection(Connection connection) {
        return LocalPlayers.INSTANCE.slots().slot(LocalPlayers.INSTANCE.connections().requireSlot(connection));
    }

    static PlayerSlot requireListener(ClientPacketListener listener) {
        return LocalPlayers.INSTANCE.slots().slot(LocalPlayers.INSTANCE.slotForClientPacketListener(listener));
    }

    static PlayerSlot requireScreen(Screen screen) {
        if (screen == null) {
            throw new IllegalArgumentException("screen cannot be null");
        }

        PlayerSlot match = null;
        for (int slotId = 0; slotId < PlayerSlots.MAX_SLOTS; slotId++) {
            PlayerSlot candidate = LocalPlayers.INSTANCE.slots().slot(slotId);
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

    static PlayerSlot requireKeyboardMouseOwner() {
        return LocalPlayers.INSTANCE.slots().slot(InputApi.keyboardMouseClientId());
    }

    static ClientToken captureCurrentToken() {
        LocalClient client = Client.current();
        PlayerSession session = requireSession(client.slotId());
        return new ClientToken(client.slotId(), session.sessionId(), session.generation());
    }

    static ClientToken captureScheduledTokenOrNull() {
        Integer scheduledSlotId = ActiveSlot.scheduledIdOrNull();
        if (scheduledSlotId != null) {
            return tokenForSlot(scheduledSlotId);
        }
        LocalClient current = Client.currentOrNull();
        if (current != null) {
            return tokenForSlot(current.slotId());
        }
        return null;
    }

    static PlayerSlot requireToken(ClientToken token) {
        if (!isTokenValid(token)) {
            throw new IllegalStateException("Client token is no longer valid: " + token);
        }
        return LocalPlayers.INSTANCE.slots().slot(token.slotId());
    }

    static boolean isTokenValid(ClientToken token) {
        if (token == null || token.slotId() < 0 || token.slotId() >= PlayerSlots.MAX_SLOTS) {
            return false;
        }
        PlayerSession session = LocalPlayers.INSTANCE.sessions().sessionOrNull(token.slotId());
        return session != null
            && session.sessionId() == token.sessionId()
            && session.generation() == token.generation()
            && LocalPlayers.INSTANCE.slots().slot(token.slotId()).connected();
    }

    private static PlayerSession requireSession(int slotId) {
        PlayerSession session = LocalPlayers.INSTANCE.sessions().sessionOrNull(slotId);
        if (session == null) {
            throw new IllegalStateException("No client session exists for slot " + slotId);
        }
        return session;
    }

    private static ClientToken tokenForSlot(int slotId) {
        PlayerSession session = requireSession(slotId);
        return new ClientToken(slotId, session.sessionId(), session.generation());
    }
}
