package net.jr.client.runtime.client;

import net.jr.api.client.local.Client;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.slot.Slots;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.Connection;

public final class ReadinessPolicy {
    private ReadinessPolicy() {
    }

    public static boolean currentGameplayReady() {
        ClientInstance client = Client.currentOrNull();
        return client != null && gameplayReady(client);
    }

    public static boolean worldBound(int slotId) {
        ClientInstance client = ClientRuntime.INSTANCE.clients().clientOrNull(slotId);
        return client != null && worldBound(client);
    }

    public static boolean worldBound(ClientInstance client) {
        Slots slot = client.slot();
        LocalPlayer player = slot.gameplayState().player();
        ClientLevel level = slot.renderState().level();
        return player != null
            && level != null
            && player.level() == level
            && !player.isRemoved();
    }

    public static boolean gameplayBound(int slotId) {
        ClientInstance client = ClientRuntime.INSTANCE.clients().clientOrNull(slotId);
        return client != null && gameplayBound(client);
    }

    public static boolean gameplayBound(ClientInstance client) {
        MultiPlayerGameMode gameMode = client.gameMode();
        return worldBound(client) && gameMode != null;
    }

    public static boolean worldReady(int slotId) {
        ClientInstance client = ClientRuntime.INSTANCE.clients().clientOrNull(slotId);
        return client != null && worldReady(client);
    }

    public static boolean worldReady(ClientInstance client) {
        return worldBound(client) && sessionReady(client);
    }

    public static boolean gameplayReady(int slotId) {
        ClientInstance client = ClientRuntime.INSTANCE.clients().clientOrNull(slotId);
        return client != null && gameplayReady(client);
    }

    public static boolean gameplayReady(ClientInstance client) {
        return gameplayBound(client) && sessionReady(client);
    }

    private static boolean sessionReady(ClientInstance client) {
        if (client.isPrimary()) {
            return !ClientRuntime.INSTANCE.clients().isJoining(0);
        }
        LocalPlayer player = client.player();
        var session = ClientRuntime.INSTANCE.clients().sessionOrNull(client.slotId());
        if (player == null || player.connection == null || session == null) {
            return false;
        }
        Connection connection = player.connection.getConnection();
        return connection != null && session.isWorldReadyFor(connection);
    }
}
