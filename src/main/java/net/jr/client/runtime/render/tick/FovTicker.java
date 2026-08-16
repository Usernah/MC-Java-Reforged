package net.jr.client.runtime.render.tick;

import net.jr.client.runtime.context.LocalClientAcces;
import net.jr.client.runtime.context.LocalClientExecution;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.client.Minecraft;

/** Per-Client FOV smoothing driven from the single high-level GameRenderer tick. */
public final class FovTicker {
    private FovTicker() {
    }

    public static void tickConnectedClients() {
        for (int clientId = 0; clientId < LocalClientAcces.MAX_CLIENTS; clientId++) {
            if (!LocalClientAcces.connected(clientId) || LocalClientAcces.level(clientId) == null || LocalClientAcces.player(clientId) == null) {
                continue;
            }
            LocalClientExecution.runForClient(clientId, FovTicker::tickCurrentClient);
        }
    }

    private static void tickCurrentClient() {
        float targetModifier = 1.0F;
        Entity cameraEntity = LocalClientAcces.cameraEntity();
        if (cameraEntity == null) {
            cameraEntity = LocalClientAcces.player();
        }
        if (cameraEntity instanceof AbstractClientPlayer player) {
            Minecraft minecraft = Minecraft.getInstance();
            targetModifier = player.getFieldOfViewModifier(
                minecraft.options.getCameraType().isFirstPerson(),
                minecraft.options.fovEffectScale().get().floatValue()
            );
        }
        LocalClientAcces.render().fovState().tick(targetModifier);
    }
}
