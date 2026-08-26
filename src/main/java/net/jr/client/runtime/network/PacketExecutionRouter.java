package net.jr.client.runtime.network;

import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.context.LocalClientExecution;
import net.jr.client.runtime.context.SlotScope;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;

public final class PacketExecutionRouter {
    private PacketExecutionRouter() {
    }

    public static void handleIncoming(Connection connection, Packet<?> packet, PacketListener listener) {
        int slotId = resolveAndBindSlot(connection);
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null && minecraft.isSameThread()) {
            LocalClientExecution.runForConnection(connection, () -> handle(packet, listener));
            return;
        }

        try (SlotScope.Scheduling ignored = SlotScope.schedule(slotId)) {
            handle(packet, listener);
        }
    }

    public static void handleQueued(Packet<?> packet, PacketListener listener) {
        if (listener instanceof ClientPacketListener clientListener
                && ClientRuntime.INSTANCE.connections().has(clientListener.getConnection())) {
            LocalClientExecution.runForListener(clientListener, () -> handle(packet, listener));
            return;
        }
        handle(packet, listener);
    }

    public static void runForListener(ClientPacketListener listener, Runnable action) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.isSameThread()) {
            action.run();
            return;
        }
        LocalClientExecution.runForListener(listener, action);
    }

    public static Runnable wrapScheduled(Runnable task) {
        return LocalClientExecution.wrapScheduled(task);
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
