package net.jr.mixin.compat.bettercombat;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.jr.client.compat.bettercombat.BetterCombatInteractors;
import net.jr.client.runtime.client.ClientInstance;
import net.jr.client.runtime.client.InstanceManager;
import net.jr.client.runtime.client.ReadinessPolicy;
import net.jr.client.runtime.context.InstanceExecution;
import net.jr.client.runtime.session.ClientSession;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = InstanceManager.class, remap = false)
abstract class LocalClientManagerMixin {
    @WrapMethod(method = "tickSecondarySession")
    private void javaReforged$tickBetterCombat(
        Minecraft minecraft,
        ClientInstance client,
        ClientSession session,
        Operation<Void> original
    ) {
        if (ReadinessPolicy.gameplayReady(client)) {
            InstanceExecution.runForClient(
                client.slotId(),
                () -> BetterCombatInteractors.current(minecraft).preTick()
            );
        }
        original.call(minecraft, client, session);
        if (ReadinessPolicy.gameplayReady(client)) {
            InstanceExecution.runForClient(
                client.slotId(),
                () -> BetterCombatInteractors.current(minecraft).postTick()
            );
        }
    }
}
