package net.jr.experimental_features.minigame.battle.client;


import net.jr.Java_reforged;
import net.jr.experimental_features.minigame.client.BattleClientState;
import net.jr.mixin.accessors.ClientInputAccessor;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;

@EventBusSubscriber(modid = Java_reforged.MODID, value = Dist.CLIENT)
public final class BattleMovementLimiter {

    private BattleMovementLimiter() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (!(event.getEntity() instanceof LocalPlayer) || !BattleClientState.isMovementLocked()) {
            return;
        }

        ClientInput clientInput = event.getInput();

        Input moveBlock = new Input(
                false,
                false,
                false,
                false,
                false,
                clientInput.keyPresses.shift(),
                false
        );

        ((ClientInputAccessor) event.getInput()).javaReforged$setMoveVector(Vec2.ZERO);
        ((ClientInputAccessor) event.getInput()).javaReforged$setKeyPresses(moveBlock);

    }
}
