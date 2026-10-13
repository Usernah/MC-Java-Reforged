package net.jr.client.sound.action;

import net.jr.Java_reforged;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = Java_reforged.MODID, value = Dist.CLIENT)
public final class EmptyAttackSoundHandler {
    private EmptyAttackSoundHandler() {
    }

    @SubscribeEvent
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        Player player = event.getEntity();
        player.level().playLocalSound(
            player,
            SoundEvents.PLAYER_ATTACK_NODAMAGE,
            SoundSource.PLAYERS,
            1.0F,
            1.0F
        );
    }
}
