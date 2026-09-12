package net.jr.mixin.compat.rpgseries;

import net.jr.client.compat.rpgseries.SpellEngineRuntimeState;
import net.spell_engine.client.ClientNetwork;
import net.spell_engine.client.gui.HudMessages;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ClientNetwork.class, remap = false)
abstract class ClientNetworkMixin {
    @Redirect(
        method = {"lambda$handleSpellMessage$0", "lambda$handleSpellCooldownSync$0"},
        at = @At(
            value = "FIELD",
            target = "Lnet/spell_engine/client/gui/HudMessages;INSTANCE:Lnet/spell_engine/client/gui/HudMessages;",
            opcode = Opcodes.GETSTATIC
        )
    )
    private static HudMessages javaReforged$currentMessages() {
        return SpellEngineRuntimeState.messages();
    }
}
