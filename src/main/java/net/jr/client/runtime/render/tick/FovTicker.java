package net.jr.client.runtime.render.tick;

import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.client.LocalClient;
import net.jr.client.runtime.context.LocalClientExecution;
import net.jr.client.runtime.context.LocalClientScope;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;

public final class FovTicker {
    private FovTicker() {
    }

    public static void tickConnectedClients() {
        for (LocalClient client : ClientRuntime.INSTANCE.clients().all()) {
            if (client.level() == null || client.player() == null) {
                continue;
            }
            LocalClientExecution.runForClient(client.slotId(), FovTicker::tickCurrentClient);
        }
    }

    private static void tickCurrentClient() {
        LocalClient client = LocalClientScope.currentClient();
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
