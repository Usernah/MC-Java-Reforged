package net.jr.mixin.compat.xaerominimap;

import net.jr.client.compat.xaero.XaeroMinimapRuntimeState;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.client.LocalClient;
import net.jr.client.runtime.client.LocalClientReadinessPolicy;
import net.jr.client.runtime.context.LocalClientExecution;
import net.jr.client.runtime.slot.LocalClientSlot;
import net.jr.client.runtime.viewport.GuiViewportScope;
import net.jr.client.runtime.viewport.ViewportArea;
import net.jr.client.runtime.viewport.ViewportRenderScope;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.IXaeroMinimap;
import xaero.common.anim.MultiplyAnimationHelper;
import xaero.common.events.ModClientEvents;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.module.MinimapSession;

@Mixin(value = ModClientEvents.class, remap = false)
abstract class ModClientEventsMixin {
    @Shadow
    protected IXaeroMinimap modMain;

    @Inject(method = "handleRenderModOverlay", at = @At("HEAD"), cancellable = true)
    private void javaReforged$renderEveryViewport(
        GuiGraphicsExtractor graphics,
        DeltaTracker deltaTracker,
        CallbackInfo callback
    ) {
        callback.cancel();
        MultiplyAnimationHelper.tick();
        if (Minecraft.getInstance().gui.hud.isHidden()) {
            return;
        }

        if (ViewportRenderScope.hasActiveViewport()) {
            this.javaReforged$renderCurrent(graphics, deltaTracker);
            return;
        }

        ClientRuntime runtime = ClientRuntime.INSTANCE;
        for (LocalClientSlot slot : runtime.viewports().drawableSlots()) {
            LocalClient client = runtime.clients().clientOrNull(slot.id());
            if (client == null || !LocalClientReadinessPolicy.gameplayReady(client)) {
                continue;
            }

            LocalClientExecution.runForClient(slot.id(), () -> {
                ViewportArea viewport = runtime.viewports().viewport(slot.id());
                try (GuiViewportScope ignored = GuiViewportScope.enter(graphics, viewport)) {
                    this.javaReforged$renderCurrent(graphics, deltaTracker);
                }
            });
        }
    }

    private void javaReforged$renderCurrent(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        XaeroMinimapRuntimeState.run(() -> {
            MinimapSession session = BuiltInHudModules.MINIMAP.getCurrentSession();
            if (session == null) {
                return;
            }
            this.modMain.getHudRenderer().render(
                this.modMain.getHud(),
                graphics,
                deltaTracker.getGameTimeDeltaPartialTick(true)
            );
            this.modMain.getMinimap().getWaypointMapRenderer().drawSetChange(
                session,
                graphics,
                Minecraft.getInstance().getWindow()
            );
        });
    }
}
