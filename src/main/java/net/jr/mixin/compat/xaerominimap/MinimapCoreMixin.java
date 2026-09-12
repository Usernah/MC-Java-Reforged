package net.jr.mixin.compat.xaerominimap;

import net.jr.client.compat.xaero.XaeroMinimapRuntimeState;
import net.jr.client.compat.xaero.XaeroMinimapWorldMatrices;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.client.LocalClient;
import net.jr.client.runtime.client.LocalClientReadinessPolicy;
import net.jr.client.runtime.context.LocalClientExecution;
import net.jr.client.runtime.slot.LocalClientSlot;
import net.jr.client.runtime.viewport.ViewportArea;
import net.jr.client.runtime.viewport.ViewportRenderScope;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.HudMod;
import xaero.common.core.XaeroMinimapCore;

@Mixin(value = XaeroMinimapCore.class, remap = false)
abstract class MinimapCoreMixin {
    @Redirect(
        method = "onPlayNetHandler",
        at = @At(
            value = "INVOKE",
            target = "Lxaero/common/core/XaeroMinimapCore;cleanupCurrentSession()V",
            ordinal = 0
        )
    )
    private static void javaReforged$keepExistingSessions() {
    }

    @Inject(method = "onProjectionMatrix", at = @At("RETURN"))
    private static void javaReforged$captureProjection(Matrix4f projection, CallbackInfo callback) {
        XaeroMinimapWorldMatrices.captureProjection(projection);
    }

    @Inject(method = "onWorldModelViewMatrix", at = @At("RETURN"))
    private static void javaReforged$captureModelView(Matrix4fc modelView, CallbackInfo callback) {
        XaeroMinimapWorldMatrices.captureModelView(modelView);
    }

    @Inject(method = "beforeIngameGuiRender", at = @At("HEAD"), cancellable = true)
    private static void javaReforged$renderWorldElementsPerViewport(CallbackInfo callback) {
        if (!XaeroMinimapCore.isModLoaded() || ViewportRenderScope.hasActiveViewport()) {
            return;
        }
        callback.cancel();
        javaReforged$forEachViewport(
            () -> XaeroMinimapRuntimeState.run(
                () -> XaeroMinimapWorldMatrices.run(
                    () -> HudMod.INSTANCE.getEvents().handleRenderGameOverlayEventPre()
                )
            )
        );
    }

    @Inject(method = "afterIngameGuiRender", at = @At("HEAD"), cancellable = true)
    private static void javaReforged$finishWorldElementsPerViewport(CallbackInfo callback) {
        if (!XaeroMinimapCore.isModLoaded() || ViewportRenderScope.hasActiveViewport()) {
            return;
        }
        callback.cancel();
        javaReforged$forEachViewport(
            () -> XaeroMinimapRuntimeState.run(
                () -> HudMod.INSTANCE.getEvents().handleRenderGameOverlayEventPost()
            )
        );
    }

    private static void javaReforged$forEachViewport(Runnable action) {
        ClientRuntime runtime = ClientRuntime.INSTANCE;
        for (LocalClientSlot slot : runtime.viewports().drawableSlots()) {
            LocalClient client = runtime.clients().clientOrNull(slot.id());
            if (client == null || !LocalClientReadinessPolicy.gameplayReady(client)) {
                continue;
            }
            LocalClientExecution.runForClient(slot.id(), () -> {
                ViewportArea viewport = runtime.viewports().viewport(slot.id());
                ViewportRenderScope.run(viewport, action);
            });
        }
    }
}
