package net.jr.client.runtime.network;

import java.util.UUID;

import net.jr.api.client.local.Client;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.context.SlotScope;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.Connection;
import javax.annotation.Nullable;
import net.jr.client.runtime.client.ClientInstance;
import net.minecraft.client.player.LocalPlayer;

public final class ClientPacketHandler {
    private ClientPacketHandler() {
    }

    public static void bindConnection(ClientPacketListener listener) {
        Connection connection = listener.getConnection();
        if (connection == null) {
            throw new IllegalStateException("ClientPacketListener has no connection");
        }

        Integer mappedSlotId = ClientRuntime.INSTANCE.connections().slotOrNull(connection);
        Integer activeSlotId = SlotScope.idOrNull();
        int slotId = mappedSlotId != null
                ? mappedSlotId
                : activeSlotId != null
                        ? activeSlotId
                        : ClientRuntime.INSTANCE.clients().slotForClientPacketListener(listener);
        ClientRuntime.INSTANCE.connections().bind(connection, slotId);
    }

    public static void closeDeathScreenAfterRespawn(Minecraft minecraft) {
        int slotId = SlotScope.requireId();
        Screen screen = ClientRuntime.INSTANCE.slots().slot(slotId).screenState().screen();
        if (screen instanceof DeathScreen || screen instanceof DeathScreen.TitleConfirmScreen) {
            minecraft.gui.setScreen(null);
        }
    }

    public static void clearLevel(ClientPacketListener listener) {
        ClientRuntime.INSTANCE.clients().onClientLevelCleared(listener);
    }

    public static void setIntegratedServerOwner(IntegratedServer server, UUID uuid) {
        Integer slotId = SlotScope.idOrNull();
        if (slotId == null || slotId == 0) {
            server.setUUID(uuid);
        }
    }

    public static ClientPacketListener loginListener(Minecraft minecraft, ClientPacketListener current) {
        Integer slotId = SlotScope.idOrNull();
        return slotId != null && slotId != 0 ? current : minecraft.getConnection();
    }

    public static void markInitialPositionSynchronized(ClientPacketListener listener) {
        ClientRuntime.INSTANCE.clients().onInitialPositionSynchronized(listener);
    }

    @Nullable
    public static ClientPacketListener activeConnection() {
        ClientInstance client = Client.currentOrNull();
        if (client == null) {
            Integer slotId = SlotScope.idOrNull();
            client = ClientRuntime.INSTANCE.clients().clientOrNull(slotId != null ? slotId : 0);
        }
        LocalPlayer player = client != null ? client.player() : null;
        return player != null ? player.connection : null;
    }
}
