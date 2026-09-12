package net.jr.mixin.compat.xaeroworldmap;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.jr.client.compat.xaero.XaeroWorldMapRuntime;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import xaero.map.events.ClientEvents;

@Mixin(value = ClientEvents.class, remap = false)
abstract class ClientEventsMixin {
    @WrapMethod(method = "handleClientTickStart")
    private void javaReforged$tickEveryClient(Operation<Void> original) {
        XaeroWorldMapRuntime.runEveryClient(original::call);
    }

    @WrapMethod(method = "handleClientRunTickStart")
    private void javaReforged$runTickEveryClient(Operation<Void> original) {
        XaeroWorldMapRuntime.runEveryClient(original::call);
    }

    @WrapMethod(method = "handlePlayerTickStart")
    private void javaReforged$playerTickCurrentClient(Player player, Operation<Void> original) {
        XaeroWorldMapRuntime.runForPlayer(original::call, player);
    }

    @WrapMethod(method = "handleRenderTick")
    private boolean javaReforged$renderTickEveryClient(boolean start, Operation<Boolean> original) {
        boolean[] cancel = {false};
        XaeroWorldMapRuntime.runEveryClient(() -> cancel[0] |= original.call(start));
        return cancel[0];
    }
}
