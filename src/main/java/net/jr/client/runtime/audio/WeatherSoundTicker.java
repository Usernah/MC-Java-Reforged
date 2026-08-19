package net.jr.client.runtime.audio;

import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.client.LocalClient;
import net.jr.client.runtime.client.LocalClientReadinessPolicy;
import net.jr.client.runtime.context.LocalClientExecution;
import net.jr.client.runtime.render.state.WorldEngineStateScope;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;

public final class WeatherSoundTicker {
    private WeatherSoundTicker() {
    }

    public static void tickConnectedClients(LevelRenderer levelRenderer) {
        Minecraft minecraft = Minecraft.getInstance();
        for (LocalClient client : ClientRuntime.INSTANCE.clients().all()) {
            if (!LocalClientReadinessPolicy.worldBound(client)) {
                continue;
            }
            LocalClientExecution.runForClient(client.slotId(), () -> tickCurrentClient(minecraft, client));
        }
    }

    private static void tickCurrentClient(Minecraft minecraft, LocalClient client) {
        try (WorldEngineStateScope ignored = WorldEngineStateScope.bind(minecraft, client.level())) {
            client.level().tickWeatherEffects();
        }
    }
}
