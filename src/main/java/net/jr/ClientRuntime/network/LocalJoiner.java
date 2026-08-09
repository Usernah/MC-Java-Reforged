package net.jr.ClientRuntime.network;

import com.mojang.authlib.GameProfile;
import java.net.SocketAddress;
import net.jr.ClientRuntime.runtime.LocalPlayers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.client.multiplayer.LevelLoadTracker;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.login.ClientLoginPacketListener;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;

public final class LocalJoiner {
    private LocalJoiner() {
    }

    public static Connection connectToIntegratedServer(Minecraft minecraft, LocalPlayers players, int slotId, GameProfile profile) {
        if (minecraft.getSingleplayerServer() == null) {
            throw new IllegalStateException("Secondary local players require an integrated server in this test path");
        }
        SocketAddress address = minecraft.getSingleplayerServer().getConnection().startMemoryChannel();
        Connection connection = Connection.connectToLocalServer(address);
        players.connections().bind(connection, slotId);
        ClientHandshakePacketListenerImpl listener = new ClientHandshakePacketListenerImpl(
            connection,
            minecraft,
            null,
            null,
            false,
            null,
            component -> {
            },
            new LevelLoadTracker(),
            null
        );
        connection.initiateServerboundPlayConnection("local", 0, (ClientLoginPacketListener)listener);
        connection.send((Packet<?>)new ServerboundHelloPacket(profile.name(), profile.id()));
        return connection;
    }
}
