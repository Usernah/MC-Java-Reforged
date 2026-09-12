package net.jr.mixin.compat.xaeroworldmap;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xaero.map.core.XaeroWorldMapCore;

@Mixin(value = XaeroWorldMapCore.class, remap = false)
abstract class WorldMapCoreMixin {
    @Redirect(
        method = "onPlayNetHandler",
        at = @At(
            value = "INVOKE",
            target = "Lxaero/map/core/XaeroWorldMapCore;cleanupCurrentSession()V",
            ordinal = 0
        )
    )
    private static void javaReforged$keepExistingSessions() {
    }
}
