package net.jr.experimental_features.minigame;

import net.jr.Java_reforged;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.item.FireChargeItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = Java_reforged.MODID)
public final class FireChargeHandler {
    private static final int FIREBALL_COOLDOWN_TICKS = 20;

    private FireChargeHandler() {
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof FireChargeItem)) {
            return;
        }

        Player player = event.getEntity();
        if (player.isShiftKeyDown()) {
            return;
        }

        if (player.getCooldowns().isOnCooldown(stack)) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            return;
        }

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);

        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        var look = player.getLookAngle();
        LargeFireball fireball = new LargeFireball(level, player, look.normalize(), 1);
        fireball.setPos(player.getEyePosition());
        fireball.setItem(stack);
        Projectile.spawnProjectileUsingShoot(fireball, level, stack, look.x, look.y, look.z, 1.5F, 1.0F);

        level.playSound(
            null,
            player.getOnPos(),
            SoundEvents.FIRECHARGE_USE,
            SoundSource.BLOCKS,
            1.0F,
            (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.2F + 1.0F
        );
        player.getCooldowns().addCooldown(stack, FIREBALL_COOLDOWN_TICKS);

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }
}
