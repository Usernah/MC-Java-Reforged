package net.jr.mixin.compat.xaeroworldmap;

import net.jr.client.compat.xaero.XaeroWorldMapRuntime;
import net.jr.client.runtime.context.SlotScope;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.WorldMapSession;

@Mixin(value = WorldMapSession.class, remap = false)
abstract class WorldMapSessionMixin {
    @Inject(method = "getCurrentSession", at = @At("HEAD"), cancellable = true)
    private static void javaReforged$getSessionForActivePlayer(
        CallbackInfoReturnable<WorldMapSession> cir
    ) {
        /*
         * Do not let a scoped secondary player fall back to Xaero's global
         * session (normally the primary or most recently joined player).
         */
        if (SlotScope.idOrNull() != null) {
            cir.setReturnValue(XaeroWorldMapRuntime.currentSessionOrNull());
        }
    }
}
