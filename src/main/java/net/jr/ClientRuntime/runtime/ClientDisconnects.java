package net.jr.ClientRuntime.runtime;

import net.minecraft.client.Minecraft;

/**
 * High-level local-client disconnect gate.
 *
 * <p>Slot 0 owns the real vanilla world disconnect. Secondary slots only close
 * their local connection and clear their own client state.</p>
 */
public final class ClientDisconnects {
    private ClientDisconnects() {
    }

    /**
     * @return true when the caller should cancel vanilla's disconnect flow.
     */
    public static boolean disconnectCurrentFromPauseMenu(Minecraft minecraft) {
        LocalClient client = Client.currentOrNull();
        int clientId = client == null ? 0 : client.slotId();
        if (clientId == 0) {
            return false;
        }
        disconnectSecondary(minecraft, clientId);
        return true;
    }

    public static void disconnectSecondary(Minecraft minecraft, int clientId) {
        LocalPlayers.INSTANCE.disconnectSecondaryClient(minecraft, clientId);
    }
}
