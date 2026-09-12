package net.jr.mixin.compat.rpgseries;

import net.jr.client.compat.rpgseries.SpellEngineRuntimeState;
import net.spell_engine.api.effect.EntityActionsAllowed;
import net.spell_engine.client.gui.HudMessages;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = EntityActionsAllowed.class, remap = false)
abstract class EntityActionsAllowedMixin {
    @Redirect(
        method = "isImpaired(Lnet/minecraft/world/entity/LivingEntity;Lnet/spell_engine/api/effect/EntityActionsAllowed$Player;Z)Z",
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
