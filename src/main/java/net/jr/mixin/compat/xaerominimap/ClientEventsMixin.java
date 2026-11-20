package net.jr.mixin.compat.xaerominimap;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.jr.client.compat.xaero.XaeroMinimapRuntimeState;
import net.jr.client.compat.xaero.XaeroMinimapWorldMatrices;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.client.ClientInstance;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import xaero.common.events.ClientEvents;

@Mixin(value = ClientEvents.class, remap = false)
abstract class ClientEventsMixin {
    @WrapMethod(method = "handleRenderGameOverlayEventPre")
    private void javaReforged$renderWorldElementsCurrentClient(Operation<Void> original) {
        XaeroMinimapRuntimeState.run(
            () -> XaeroMinimapWorldMatrices.run(original::call)
        );
    }

    @WrapMethod(method = "handleClientTickStart")
    private void javaReforged$tickEveryClient(Operation<Void> original) {
        for (ClientInstance client : ClientRuntime.INSTANCE.clients().all()) {
            if (client.player() != null) {
                client.slot().runForSlot(
                    () -> XaeroMinimapRuntimeState.run(original::call)
                );
            }
        }
    }

    @WrapMethod(method = "handleRenderTickStart")
    private void javaReforged$renderTickEveryClient(Operation<Void> original) {
        for (ClientInstance client : ClientRuntime.INSTANCE.clients().all()) {
            if (client.player() != null) {
                client.slot().runForSlot(
                    () -> XaeroMinimapRuntimeState.run(original::call)
                );
            }
        }
    }

    @WrapMethod(method = "handlePlayerTickStart")
    private void javaReforged$playerTickCurrentClient(Player player, Operation<Void> original) {
        for (ClientInstance client : ClientRuntime.INSTANCE.clients().all()) {
            if (client.player() == player) {
                client.slot().runForSlot(
                    () -> XaeroMinimapRuntimeState.run(() -> original.call(player))
                );
                return;
            }
        }
        original.call(player);
    }
}
