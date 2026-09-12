package net.jr.mixin.compat.bettercombat;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.jr.client.compat.bettercombat.BetterCombatInteractors;
import net.jr.client.runtime.client.LocalClient;
import net.jr.client.runtime.client.LocalClientManager;
import net.jr.client.runtime.client.LocalClientReadinessPolicy;
import net.jr.client.runtime.context.LocalClientExecution;
import net.jr.client.runtime.session.LocalClientSession;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = LocalClientManager.class, remap = false)
abstract class LocalClientManagerMixin {
    @WrapMethod(method = "tickSecondarySession")
    private void javaReforged$tickBetterCombat(
        Minecraft minecraft,
        LocalClient client,
        LocalClientSession session,
        Operation<Void> original
    ) {
        if (LocalClientReadinessPolicy.gameplayReady(client)) {
            LocalClientExecution.runForClient(
                client.slotId(),
                () -> BetterCombatInteractors.current(minecraft).preTick()
            );
        }
        original.call(minecraft, client, session);
        if (LocalClientReadinessPolicy.gameplayReady(client)) {
            LocalClientExecution.runForClient(
                client.slotId(),
                () -> BetterCombatInteractors.current(minecraft).postTick()
            );
        }
    }
}
