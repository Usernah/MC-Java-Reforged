package net.jr.experimental_features.minigame.battle.client;

import net.jr.Java_reforged;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.client.ClientInstance;
import net.jr.experimental_features.minigame.battle.BattlePhase;
import net.jr.experimental_features.minigame.client.BattleClientState;
import net.minecraft.client.CameraType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.HashMap;
import java.util.Map;


@EventBusSubscriber(modid = Java_reforged.MODID, value = Dist.CLIENT)
public final class BattleWinnerCamera {
    private static final Map<Integer, CameraType> PREVIOUS_VIEWS = new HashMap<>();

    private BattleWinnerCamera() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        BattlePhase phase = BattleClientState.phase();
        boolean followingWinner = phase == BattlePhase.WINNER_ANOUNCEMENT || phase == BattlePhase.FINISH;
        for (ClientInstance client : ClientRuntime.INSTANCE.clients().all()) {
            int slotId = client.slotId();
            boolean shouldFollow = followingWinner && client.player() != null
                && !BattleClientState.isWinner(client.player().getUUID().toString());
            if (shouldFollow) {
                PREVIOUS_VIEWS.putIfAbsent(slotId, client.options().cameraType());
                client.options().setCameraType(CameraType.THIRD_PERSON_BACK);
            } else {
                CameraType previous = PREVIOUS_VIEWS.remove(slotId);
                if (previous != null) {
                    client.options().setCameraType(previous);
                }
            }
        }
    }
}
