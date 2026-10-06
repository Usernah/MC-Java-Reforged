package net.jr.mixin.compat.create;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.client.ClientInstance;
import net.jr.client.runtime.context.InstanceExecution;
import net.jr.client.runtime.context.SlotScope;
import net.jr.client.compat.create.CreateRuntimeState;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "com.simibubi.create.foundation.events.ClientEvents", remap = false)
abstract class ClientEventsMixin {
    @WrapMethod(method = "preTick")
    private static void javaReforged$preTickEveryClient(
        ClientTickEvent.Pre event,
        Operation<Void> original
    ) {
        runEveryClient(() -> {
            CreateRuntimeState.tickClientAudio();
            original.call(event);
        });
    }

    @WrapMethod(method = "tick")
    private static void javaReforged$tickEveryClient(
        ClientTickEvent.Post event,
        Operation<Void> original
    ) {
        runEveryClient(() -> original.call(event));
    }

    /*
     * Create normally runs this from ClientTick.Post, which is after the
     * primary player's world/entity tick. JR's secondary worlds tick after
     * that global event, so running their collision here uses a one-tick-old
     * player position and makes moving contraptions fight the client.
     *
     * Secondary slots execute the same call immediately after their own
     * Level.tick() in LocalClientManager.
     */
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
        runEveryClient(() -> original.call(event));
    }

    private static void runEveryClient(Runnable action) {
        boolean ran = false;
        for (ClientInstance client : ClientRuntime.INSTANCE.clients().all()) {
            if (client.player() == null || client.level() == null) {
                continue;
            }
            InstanceExecution.runForClient(client.slotId(), action);
            ran = true;
        }
        if (!ran) {
            InstanceExecution.runPrimary(action);
        }
    }
}
