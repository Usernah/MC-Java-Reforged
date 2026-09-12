package net.jr.mixin.compat.rpgseries;

import net.jr.client.compat.rpgseries.SpellEngineRuntimeState;
import net.spell_engine.client.casting.ClientCastController;
import net.spell_engine.client.input.SpellHotbar;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ClientCastController.class, remap = false)
abstract class ClientCastControllerMixin {
    @Redirect(
        method = "applyInstantGlobalCooldown",
        at = @At(
            value = "FIELD",
            target = "Lnet/spell_engine/client/input/SpellHotbar;INSTANCE:Lnet/spell_engine/client/input/SpellHotbar;",
            opcode = Opcodes.GETSTATIC
        )
    )
    private SpellHotbar javaReforged$currentHotbar() {
        return SpellEngineRuntimeState.hotbar();
    }
}
