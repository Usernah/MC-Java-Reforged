package net.jr.experimental_features.minigame;

import net.jr.Java_reforged;
import net.jr.client.input.binding.ModKeyBindings;
import net.jr.experimental_features.minigame.network.BattleNetwork;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = Java_reforged.MODID, value = Dist.CLIENT)
public final class MinigameKeyHandler {
    private MinigameKeyHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        boolean startPressed = ModKeyBindings.MINIGAME_START.consumeClick();
        boolean togglePressed = ModKeyBindings.BATTLE_TEST_TOGGLE.consumeClick();
        var connection = Minecraft.getInstance().getConnection();
        if (connection == null || !connection.hasChannel(BattleNetwork.ControlRequest.TYPE)) {
            return;
        }

        if (startPressed) {
            ClientPacketDistributor.sendToServer(new BattleNetwork.ControlRequest(false));
        }
        if (togglePressed) {
            ClientPacketDistributor.sendToServer(new BattleNetwork.ControlRequest(true));
        }
    }
}
