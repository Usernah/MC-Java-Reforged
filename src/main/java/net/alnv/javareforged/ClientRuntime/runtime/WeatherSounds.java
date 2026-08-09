package net.alnv.javareforged.ClientRuntime.runtime;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;

/** Runs the singleton weather engine once for every ready Client. */
public final class WeatherSounds {
    private WeatherSounds() {
    }

    public static void tickConnectedClients(LevelRenderer levelRenderer) {
        Minecraft minecraft = Minecraft.getInstance();
        for (int clientId = 0; clientId < Client.MAX_CLIENTS; clientId++) {
            if (!canTick(clientId)) {
                continue;
            }
            ClientBoundary.runForClient(clientId, () -> tickCurrentClient(minecraft, levelRenderer));
        }
    }

    private static boolean canTick(int clientId) {
        return Client.connected(clientId)
            && Client.visible(clientId)
            && Client.level(clientId) != null
            && Client.player(clientId) != null
            && Client.player(clientId).level() == Client.level(clientId)
            && !Client.player(clientId).isRemoved();
    }

    private static void tickCurrentClient(Minecraft minecraft, LevelRenderer levelRenderer) {
        try (WorldEngineStateScope ignored = WorldEngineStateScope.bind(minecraft, Client.level())) {
            levelRenderer.tickRain(Client.camera());
        }
    }
}
