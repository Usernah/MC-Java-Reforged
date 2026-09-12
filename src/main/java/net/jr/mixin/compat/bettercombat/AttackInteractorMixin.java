package net.jr.mixin.compat.bettercombat;

import net.bettercombat.client.AttackInteractor;
import net.bettercombat.mixin.client.MinecraftClientAccessor;
import net.jr.client.runtime.bridge.MinecraftClientAccess;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = AttackInteractor.class, remap = false)
abstract class AttackInteractorMixin {
    @Redirect(
        method = "startUpswing",
        at = @At(
            value = "INVOKE",
            target = "Lnet/bettercombat/mixin/client/MinecraftClientAccessor;getMissTime()I"
        )
    )
    private int javaReforged$getMissTime(MinecraftClientAccessor accessor) {
        return MinecraftClientAccess.missTime((Minecraft)(Object)accessor);
    }

    @Redirect(
        method = "setMiningCooldown",
        at = @At(
            value = "INVOKE",
            target = "Lnet/bettercombat/mixin/client/MinecraftClientAccessor;setAttackCooldown(I)V"
        )
    )
    private void javaReforged$setAttackCooldown(MinecraftClientAccessor accessor, int ticks) {
        MinecraftClientAccess.setMissTime((Minecraft)(Object)accessor, ticks);
    }

    @Redirect(
        method = "setItemUseCooldown",
        at = @At(
            value = "INVOKE",
            target = "Lnet/bettercombat/mixin/client/MinecraftClientAccessor;setItemUseCooldown(I)V"
        )
    )
    private void javaReforged$setItemUseCooldown(MinecraftClientAccessor accessor, int ticks) {
        MinecraftClientAccess.setRightClickDelay((Minecraft)(Object)accessor, ticks);
    }
}
