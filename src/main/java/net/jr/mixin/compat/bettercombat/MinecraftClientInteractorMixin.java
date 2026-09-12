package net.jr.mixin.compat.bettercombat;

import net.jr.client.compat.bettercombat.BetterCombatInteractors;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Minecraft.class, priority = 900)
abstract class MinecraftClientInteractorMixin {
    @Inject(method = "clearDownloadedResourcePacks", at = @At("TAIL"))
    private void javaReforged$resetInteractors(CallbackInfo ci) {
        BetterCombatInteractors.reset();
    }
}
