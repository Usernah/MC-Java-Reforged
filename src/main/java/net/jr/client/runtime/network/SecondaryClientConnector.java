package net.jr.client.runtime.network;

import com.mojang.authlib.GameProfile;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.client.multiplayer.LevelLoadTracker;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ResolvedServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerNameResolver;
import net.minecraft.client.resources.server.ServerPackManager;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.login.ClientLoginPacketListener;
import net.minecraft.network.protocol.login.LoginProtocols;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.server.network.EventLoopGroupHolder;

public final class SecondaryClientConnector {
    private SecondaryClientConnector() {
    }

    public static Connection connectToIntegratedServer(
        Minecraft minecraft,
        ConnectionSlotRegistry connections,
        int slotId,
        GameProfile profile
    ) {
        if (minecraft.getSingleplayerServer() == null) {
            throw new IllegalStateException("Secondary local clients require an integrated server in this path");
        }
        SocketAddress address = minecraft.getSingleplayerServer().getConnection().startMemoryChannel();
        Connection connection = Connection.connectToLocalServer(address);
        connections.bind(connection, slotId);
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

    public static Connection connectToRemoteServer(
        Minecraft minecraft,
        ConnectionSlotRegistry connections,
        int slotId,
        GameProfile profile,
        ServerData serverData
    ) {
        ServerAddress hostAndPort = ServerAddress.parseString(serverData.ip);
        InetSocketAddress address = ServerNameResolver.DEFAULT
            .resolveAddress(hostAndPort)
            .map(ResolvedServerAddress::asInetSocketAddress)
            .orElseThrow(() -> new IllegalStateException("Could not resolve local client server address " + serverData.ip));

        Connection connection = Connection.connectToServer(
            address,
            EventLoopGroupHolder.remote(minecraft.options.useNativeTransport()),
            minecraft.getDebugOverlay().getBandwidthLogger()
        );
        connections.bind(connection, slotId);
        minecraft.getDownloadedPackSource().configureForServerControl(
            connection,
            convertPackStatus(serverData.getResourcePackStatus())
        );

        ClientHandshakePacketListenerImpl listener = new ClientHandshakePacketListenerImpl(
            connection,
            minecraft,
            serverData,
            null,
            false,
            null,
            component -> {
            },
            new LevelLoadTracker(),
            null
        );
        connection.initiateServerboundPlayConnection(
            address.getHostName(),
            address.getPort(),
            LoginProtocols.SERVERBOUND,
            LoginProtocols.CLIENTBOUND,
            listener,
            false
        );
        connection.send((Packet<?>)new ServerboundHelloPacket(profile.name(), profile.id()));
        return connection;
    }

    private static ServerPackManager.PackPromptStatus convertPackStatus(ServerData.ServerPackStatus status) {
        return switch (status) {
            case ENABLED -> ServerPackManager.PackPromptStatus.ALLOWED;
            case DISABLED -> ServerPackManager.PackPromptStatus.DECLINED;
            case PROMPT -> ServerPackManager.PackPromptStatus.PENDING;
        };
    }
}
