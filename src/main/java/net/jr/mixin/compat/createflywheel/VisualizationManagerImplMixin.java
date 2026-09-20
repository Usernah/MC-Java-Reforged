package net.jr.mixin.compat.createflywheel;

import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.client.LocalClient;
import net.minecraft.world.level.LevelAccessor;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Flywheel normally accepts only Minecraft's one globally installed level.
 * Java Reforged has one ClientLevel per local player, so entity/block visuals
 * must regard every owned level as visualizable even outside a slot scope.
 */
@Mixin(targets = "dev.engine_room.create_flywheel.impl.visualization.VisualizationManagerImpl", remap = false)
abstract class VisualizationManagerImplMixin {
    @Inject(
        method = "supportsVisualization",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/Minecraft;level:Lnet/minecraft/client/multiplayer/ClientLevel;",
            opcode = Opcodes.GETFIELD
        ),
        cancellable = true
    )
    private static void javaReforged$supportEveryLocalLevel(
        LevelAccessor level,
        CallbackInfoReturnable<Boolean> cir
    ) {
        for (LocalClient client : ClientRuntime.INSTANCE.clients().all()) {
            if (client.level() == level) {
                cir.setReturnValue(true);
                return;
            }
        }
    }
}
