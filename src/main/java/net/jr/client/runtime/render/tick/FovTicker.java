package net.jr.client.runtime.render.tick;

import net.jr.api.client.local.Client;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.client.ClientInstance;
import net.jr.client.runtime.context.InstanceExecution;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;

public final class FovTicker {
    private FovTicker() {
    }

    public static void tickConnectedClients() {
        for (ClientInstance client : ClientRuntime.INSTANCE.clients().all()) {
            if (client.level() == null || client.player() == null) {
                continue;
            }
            InstanceExecution.runForClient(client.slotId(), FovTicker::tickCurrentClient);
        }
    }

    private static void tickCurrentClient() {
        ClientInstance client = Client.current();
        float targetModifier = 1.0F;
        Entity cameraEntity = client.cameraEntity();
        if (cameraEntity == null) {
            cameraEntity = client.player();
        }
        if (cameraEntity instanceof AbstractClientPlayer player) {
            Minecraft minecraft = Minecraft.getInstance();
            targetModifier = player.getFieldOfViewModifier(
                minecraft.options.getCameraType().isFirstPerson(),
                minecraft.options.fovEffectScale().get().floatValue()
            );
        }
        client.render().fovState().tick(targetModifier);
    }
}
