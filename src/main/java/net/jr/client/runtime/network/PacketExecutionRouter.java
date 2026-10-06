package net.jr.client.runtime.network;

import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.context.InstanceExecution;
import net.jr.client.runtime.context.SlotScope;
import net.jr.client.runtime.session.DisconnectHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;

public final class PacketExecutionRouter {
    private PacketExecutionRouter() {
    }

    public static void handleIncoming(Connection connection, Packet<?> packet, PacketListener listener) {
        int slotId = resolveAndBindSlot(connection);
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null && minecraft.isSameThread()) {
            InstanceExecution.runForConnection(connection, () -> handle(packet, listener));
            return;
        }

        try (SlotScope.Scheduling ignored = SlotScope.schedule(slotId)) {
            handle(packet, listener);
        }
    }

    public static void handleQueued(Packet<?> packet, PacketListener listener) {
        if (listener instanceof ClientPacketListener clientListener
                && ClientRuntime.INSTANCE.connections().has(clientListener.getConnection())) {
            InstanceExecution.runForListener(clientListener, () -> handle(packet, listener));
            return;
        }
        handle(packet, listener);
    }

    public static void handleDisconnect(
        Connection connection,
        PacketListener listener,
        DisconnectionDetails details
    ) {
        Integer slotId = ClientRuntime.INSTANCE.connections().slotOrNull(connection);
        if (slotId == null || slotId == 0) {
            listener.onDisconnect(details);
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Runnable disconnect = () ->
            DisconnectHandler.disconnectSecondaryConnection(minecraft, slotId, connection);
        if (minecraft.isSameThread()) {
            disconnect.run();
        } else {
            minecraft.execute(disconnect);
        }
    }

    public static void runForListener(ClientPacketListener listener, Runnable action) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.isSameThread()) {
            action.run();
            return;
        }
        InstanceExecution.runForListener(listener, action);
    }

    public static Runnable wrapScheduled(Runnable task) {
        return InstanceExecution.wrapScheduled(task);
    }

    private static int resolveAndBindSlot(Connection connection) {
        Integer boundSlotId = ClientRuntime.INSTANCE.connections().slotOrNull(connection);
        if (boundSlotId != null) {
            return boundSlotId;
        }
        Integer activeSlotId = SlotScope.idOrNull();
        int slotId = activeSlotId != null ? activeSlotId : 0;
        ClientRuntime.INSTANCE.connections().bind(connection, slotId);
        return slotId;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void handle(Packet packet, PacketListener listener) {
        packet.handle(listener);
    }
}
