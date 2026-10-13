package net.jr.client.ui.container;

import net.jr.client.runtime.context.InstanceExecution;
import net.jr.network.ContainerOriginNetwork;
import net.jr.screens.GenericContainerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.neoforged.neoforge.client.network.handling.ClientPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ContainerOriginClient {
    private ContainerOriginClient() {
    }

    public static void handle(ContainerOriginNetwork.OpenOrigin origin, IPayloadContext context) {
        if (!(context instanceof ClientPayloadContext clientContext)
            || !(clientContext.listener() instanceof ClientPacketListener listener)) {
            return;
        }
        InstanceExecution.runForListener(listener, () -> {
            if (Minecraft.getInstance().gui.screen() instanceof GenericContainerScreen screen) {
                screen.setBlockOrigin(origin.containerId(), origin.blockId());
            }
        });
    }
}
