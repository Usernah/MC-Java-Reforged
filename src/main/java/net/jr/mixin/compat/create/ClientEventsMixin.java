package net.jr.mixin.compat.create;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.jr.client.runtime.context.SlotScope;
import net.jr.client.compat.create.CreateClientEventDispatch;
import net.jr.client.compat.create.CreateRuntimeState;
import net.jr.client.compat.create.CreateHudRenderObjects;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.simibubi.create.foundation.events.ClientEvents", remap = false)
abstract class ClientEventsMixin {
    @Inject(method = "registerGuiOverlays", at = @At("HEAD"), cancellable = true)
    private static void javaReforged$registerHudObjects(
        RegisterGuiLayersEvent event,
        CallbackInfo callback
    ) {
        CreateHudRenderObjects.register();
        callback.cancel();
    }

    @WrapMethod(method = "preTick")
    private static void javaReforged$preTickEveryClient(
        ClientTickEvent.Pre event,
        Operation<Void> original
    ) {
        CreateClientEventDispatch.runEveryClient(() -> original.call(event));
    }

    @WrapMethod(method = "tick")
    private static void javaReforged$tickEveryClient(
        ClientTickEvent.Post event,
        Operation<Void> original
    ) {
        CreateClientEventDispatch.runEveryClient(() -> original.call(event));
    }


    @Redirect(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target =
                "Lcom/simibubi/create/content/contraptions/ContraptionHandler;"
                    + "tick(Lnet/minecraft/world/level/Level;)V"
        )
    )
    private static void javaReforged$tickContraptionsInWorldOrder(
        Level level
    ) {
        Integer slotId = SlotScope.idOrNull();
        if (slotId != null && slotId > 0) {
            return;
        }

        CreateRuntimeState.tickContraptionsAfterWorldTick(level);
    }

    @WrapMethod(method = "renderFrame")
    private static void javaReforged$renderFrameEveryClient(
        RenderFrameEvent.Pre event,
        Operation<Void> original
    ) {
        CreateClientEventDispatch.runEveryClient(() -> original.call(event));
    }
}
