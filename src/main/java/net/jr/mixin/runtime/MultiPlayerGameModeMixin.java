package net.jr.mixin.runtime;

import net.jr.client.runtime.player.LegacyGameplayModes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
    @ModifyConstant(method = "continueDestroyBlock", constant = @Constant(intValue = 5))
    private int javaReforged$useLegacyCreativeBreakCadence(int original) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null
                && minecraft.player.getAbilities().instabuild
                && LegacyGameplayModes.usesLegacyConstruction(minecraft.player)
                ? 4
                : original;
    }
}
