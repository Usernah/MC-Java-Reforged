package net.jr.mixin.compat.jade;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.jr.client.compat.jade.JadeRuntimeState;
import net.jr.api.client.ui.Viewport;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.client.ClientInstance;
import net.jr.client.runtime.context.InstanceExecution;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.mojang.blaze3d.platform.Window;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent.BossEventProgress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import snownee.jade.api.ui.Rect2f;
import snownee.jade.util.ClientProxy;

@Mixin(value = ClientProxy.class, remap = false)
abstract class ClientProxyMixin {
    @WrapMethod(method = "onClientTick")
    private static void javaReforged$tickEveryClient(Post event, Operation<Void> original) {
        for (ClientInstance client : ClientRuntime.INSTANCE.clients().all()) {
            InstanceExecution.runForClient(
                client.slotId(),
                () -> JadeRuntimeState.run(() -> original.call(event))
            );
        }
    }

    @WrapMethod(method = "onRenderTick")
    private static void javaReforged$renderCurrentClient(
        GuiGraphicsExtractor graphics,
        float tickDelta,
        Operation<Void> original
    ) {
        JadeRuntimeState.run(() -> original.call(graphics, tickDelta));
    }

    @WrapMethod(method = "drawBossBarPre")
    private static void javaReforged$readCurrentBossBar(
        BossEventProgress event,
        Operation<Void> original
    ) {
        JadeRuntimeState.run(() -> original.call(event));
    }

    @WrapMethod(method = "drawBossBarPostInternal")
    private static void javaReforged$writeCurrentBossBar(
        BossEventProgress event,
        Operation<Void> original
    ) {
        JadeRuntimeState.run(() -> original.call(event));
    }

    @WrapMethod(method = "getBossBarRect")
    private static Rect2f javaReforged$currentBossBarRect(Operation<Rect2f> original) {
        return JadeRuntimeState.call(original::call);
    }

    @Redirect(
        method = "getBossBarRect",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/Window;getGuiScaledWidth()I"
        )
    )
    private static int javaReforged$viewportWidth(Window window) {
        return Viewport.current().logicalGuiWidth();
    }

    @WrapMethod(method = "onPlayerLeave")
    private static void javaReforged$disconnectCurrentClient(
        LoggingOut event,
        Operation<Void> original
    ) {
        JadeRuntimeState.run(() -> original.call(event));
    }
}
