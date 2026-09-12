package net.jr.mixin.compat.rpgseries;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.jr.client.compat.rpgseries.SpellEngineRuntimeState;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = MultiPlayerGameMode.class, priority = 900)
abstract class MultiPlayerGameModeMixin {
    @WrapMethod(method = "useItem")
    private InteractionResult javaReforged$useCurrentSpellHotbar(
        Player player,
        InteractionHand hand,
        Operation<InteractionResult> original
    ) {
        try (SpellEngineRuntimeState.Scope ignored = SpellEngineRuntimeState.activateCurrent()) {
            return original.call(player, hand);
        }
    }
}
