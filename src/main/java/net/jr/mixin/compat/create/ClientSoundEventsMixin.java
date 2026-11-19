package net.jr.mixin.compat.create;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.jr.client.compat.create.CreateClientEventDispatch;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets = "com.simibubi.create.foundation.sound.ClientSoundEvents", remap = false)
abstract class ClientSoundEventsMixin {
    @WrapMethod(method = "preTick")
    private static void javaReforged$preTickEveryClient(
        ClientTickEvent.Pre event,
        Operation<Void> original
    ) {
        CreateClientEventDispatch.runEveryClient(() -> original.call(event));
    }

    @WrapMethod(method = "postTick")
    private static void javaReforged$postTickEveryClient(
        ClientTickEvent.Post event,
        Operation<Void> original
    ) {
        CreateClientEventDispatch.runEveryClient(() -> original.call(event));
    }

    @WrapMethod(method = "reset")
    private static void javaReforged$resetEveryClient(Operation<Void> original) {
        CreateClientEventDispatch.runEveryClient(original::call);
    }
}
