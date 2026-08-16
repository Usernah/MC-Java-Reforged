package net.jr.client.runtime.audio;

import net.jr.client.runtime.context.LocalClientAcces;
import net.jr.client.runtime.context.LocalClientExecution;
import net.jr.client.runtime.render.state.WorldEngineStateScope;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;

/** Runs the singleton weather engine once for every ready Client. */
public final class WeatherSoundTicker {
    private WeatherSoundTicker() {
    }

    public static void tickConnectedClients(LevelRenderer levelRenderer) {
        Minecraft minecraft = Minecraft.getInstance();
        for (int clientId = 0; clientId < LocalClientAcces.MAX_CLIENTS; clientId++) {
            if (!canTick(clientId)) {
                continue;
            }
            LocalClientExecution.runForClient(clientId, () -> tickCurrentClient(minecraft, levelRenderer));
        }
    }

    private static boolean canTick(int clientId) {
        return LocalClientAcces.connected(clientId)
            && LocalClientAcces.visible(clientId)
            && LocalClientAcces.level(clientId) != null
            && LocalClientAcces.player(clientId) != null
            && LocalClientAcces.player(clientId).level() == LocalClientAcces.level(clientId)
            && !LocalClientAcces.player(clientId).isRemoved();
    }

    private static void tickCurrentClient(Minecraft minecraft, LevelRenderer levelRenderer) {
        try (WorldEngineStateScope ignored = WorldEngineStateScope.bind(minecraft, LocalClientAcces.level())) {
            LocalClientAcces.level().tickWeatherEffects();
        }
    }
}
