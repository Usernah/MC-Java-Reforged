package net.jr.client.runtime.client;

import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.slot.LocalClientSlot;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;

public final class LocalClientReadinessPolicy {
    private LocalClientReadinessPolicy() {
    }

    public static boolean worldBound(int slotId) {
        LocalClient client = ClientRuntime.INSTANCE.clients().clientOrNull(slotId);
        return client != null && worldBound(client);
    }

    public static boolean worldBound(LocalClient client) {
        LocalClientSlot slot = client.slot();
        LocalPlayer player = slot.gameplayState().player();
        ClientLevel level = slot.renderState().level();
        return player != null
            && level != null
            && player.level() == level
            && !player.isRemoved();
    }

    public static boolean gameplayBound(int slotId) {
        LocalClient client = ClientRuntime.INSTANCE.clients().clientOrNull(slotId);
        return client != null && gameplayBound(client);
    }

    public static boolean gameplayBound(LocalClient client) {
        MultiPlayerGameMode gameMode = client.gameMode();
        return worldBound(client) && gameMode != null;
    }

    public static boolean worldReady(int slotId) {
        LocalClient client = ClientRuntime.INSTANCE.clients().clientOrNull(slotId);
        return client != null && worldReady(client);
    }

    public static boolean worldReady(LocalClient client) {
        return worldBound(client) && !ClientRuntime.INSTANCE.clients().isJoining(client.slotId());
    }

    public static boolean gameplayReady(int slotId) {
        LocalClient client = ClientRuntime.INSTANCE.clients().clientOrNull(slotId);
        return client != null && gameplayReady(client);
    }

    public static boolean gameplayReady(LocalClient client) {
        return gameplayBound(client) && !ClientRuntime.INSTANCE.clients().isJoining(client.slotId());
    }
}
