package net.jr.mixin.SSM;

import net.jr.ClientRuntime.runtime.Client;
import net.jr.ClientRuntime.runtime.HudPass;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public abstract class HudSSStateMixin {
    @Inject(
        method = {"resetTitleTimes", "setNowPlaying", "setOverlayMessage", "setTimes", "setSubtitle", "setTitle", "clearTitles"},
        at = @At("HEAD")
    )
    private void splitTest$installHudState(CallbackInfo callback) {
        if (Client.currentOrNull() != null) {
            HudPass.begin((Hud)(Object)this);
        }
    }

    @Inject(
        method = {"resetTitleTimes", "setNowPlaying", "setOverlayMessage", "setTimes", "setSubtitle", "setTitle", "clearTitles"},
        at = @At("RETURN")
    )
    private void splitTest$captureHudState(CallbackInfo callback) {
        if (Client.currentOrNull() != null) {
            HudPass.end((Hud)(Object)this);
        }
    }

    @Redirect(method = "tick()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;tick()V"))
    private void splitTest$tickGlobalChatOnce(ChatComponent chat) {
        HudPass.tickChat(chat);
    }
}
