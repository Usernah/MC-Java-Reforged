package net.alnv.javareforged.mixin.SSM;

import net.alnv.javareforged.ClientRuntime.runtime.Client;
import net.alnv.javareforged.ClientRuntime.runtime.HudPass;
import net.alnv.javareforged.ClientRuntime.runtime.LocalClient;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiSSStateMixin {
    @Inject(method = {"resetTitleTimes", "setNowPlaying", "setOverlayMessage", "setChatDisabledByPlayerShown", "setTimes", "setSubtitle", "setTitle", "clear"}, at = @At("HEAD"))
    private void splitTest$installHudState(CallbackInfo callback) {
        if (Client.currentOrNull() != null) {
            HudPass.begin((Gui)(Object)this);
        }
    }

    @Inject(method = {"resetTitleTimes", "setNowPlaying", "setOverlayMessage", "setChatDisabledByPlayerShown", "setTimes", "setSubtitle", "setTitle", "clear"}, at = @At("RETURN"))
    private void splitTest$captureHudState(CallbackInfo callback) {
        if (Client.currentOrNull() != null) {
            HudPass.end((Gui)(Object)this);
        }
    }

    @Redirect(method = "tick()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;tick()V"))
    private void splitTest$tickGlobalChatOnce(ChatComponent chat) {
        HudPass.tickChat(chat);
    }

    @Inject(method = "renderCameraOverlays", at = @At("HEAD"), cancellable = true)
    private void splitTest$preventRespawnCrash(net.minecraft.client.gui.GuiGraphics guiGraphics, net.minecraft.client.DeltaTracker deltaTracker, CallbackInfo ci) {
        // Ejecutamos la misma validación que causó el crash:
        // Si el Runtime aún no tiene listo al jugador (es null), cancelamos este frame de renderizado
        LocalClient client = Client.currentOrNull();
        if (client != null && client.player() == null) {
            ci.cancel();
        }
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void splitTest$preventGlobalHudCrash(net.minecraft.client.gui.GuiGraphics guiGraphics, net.minecraft.client.DeltaTracker deltaTracker, CallbackInfo ci) {
        // Escudo definitivo: si el jugador o el modo de juego son null en este instante de carga, cancela el HUD entero
        LocalClient client = Client.currentOrNull();
        if (client != null && (client.player() == null || client.gameMode() == null)) {
            ci.cancel();
        }
    }

}
