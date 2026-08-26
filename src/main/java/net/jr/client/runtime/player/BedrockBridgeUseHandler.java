package net.jr.client.runtime.player;

import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;

public final class BedrockBridgeUseHandler {
    private BedrockBridgeUseHandler() {
    }

    @Nullable
    public static InteractionResult tryUse(
            MultiPlayerGameMode gameMode,
            Player player,
            InteractionHand hand
    ) {
        if (!(player instanceof LocalPlayer localPlayer)) {
            return null;
        }

        BedrockBridgePlacement.Candidate candidate = BedrockBridgePlacement.resolve(localPlayer, hand);
        if (candidate == null) {
            return null;
        }

        InteractionResult result = gameMode.useItemOn(localPlayer, hand, candidate.hitResult());
        return result instanceof InteractionResult.Success || result instanceof InteractionResult.Fail
                ? result
                : null;
    }
}
