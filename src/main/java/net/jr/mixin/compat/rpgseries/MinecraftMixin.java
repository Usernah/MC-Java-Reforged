package net.jr.mixin.compat.rpgseries;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.jr.client.compat.rpgseries.SpellEngineRuntimeState;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = Minecraft.class, priority = 900)
abstract class MinecraftMixin {
    @WrapMethod(method = "tick")
    private void javaReforged$tickSpellEngineState(Operation<Void> original) {
        try (SpellEngineRuntimeState.Scope ignored = SpellEngineRuntimeState.activatePrimary()) {
            original.call();
        }
        SpellEngineRuntimeState.tickSecondaryMessages();
    }

    @WrapMethod(method = "handleKeybinds")
    private void javaReforged$handleCurrentSpellHotbar(Operation<Void> original) {
        try (SpellEngineRuntimeState.Scope ignored = SpellEngineRuntimeState.activateCurrent()) {
            original.call();
        }
    }

    @WrapMethod(method = "startUseItem")
    private void javaReforged$useCurrentSpellHotbar(Operation<Void> original) {
        try (SpellEngineRuntimeState.Scope ignored = SpellEngineRuntimeState.activateCurrent()) {
            original.call();
        }
    }
}
