package net.jr.mixin.runtime;

import net.jr.client.runtime.context.LocalClientAcces;
import net.jr.client.runtime.render.pass.HudRenderPass;
import net.jr.client.runtime.ui.LocalChatRouting;
import net.jr.client.runtime.context.LocalClient;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.components.ChatComponent;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public abstract class HudSSStateMixin {
    @Shadow
    @Final
    private ChatComponent chat;

    @Shadow
    private float vignetteBrightness;

    @Inject(
        method = {"resetTitleTimes", "setNowPlaying", "setOverlayMessage", "setTimes", "setSubtitle", "setTitle", "clearTitles"},
        at = @At("HEAD")
    )
    private void splitTest$installHudState(CallbackInfo callback) {
        if (LocalClientAcces.currentOrNull() != null) {
            HudRenderPass.begin((Hud)(Object)this);
        }
    }

    @Inject(
        method = {"resetTitleTimes", "setNowPlaying", "setOverlayMessage", "setTimes", "setSubtitle", "setTitle", "clearTitles"},
        at = @At("RETURN")
    )
    private void splitTest$captureHudState(CallbackInfo callback) {
        if (LocalClientAcces.currentOrNull() != null) {
            HudRenderPass.end((Hud)(Object)this);
        }
    }

    @Redirect(method = "tick()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;tick()V"))
    private void splitTest$tickGlobalChatOnce(ChatComponent chat) {
        HudRenderPass.tickChat(chat);
    }

    @Redirect(
        method = {
            "extractChat(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V",
            "tick()V",
            "getChat()Lnet/minecraft/client/gui/components/ChatComponent;",
            "onDisconnected()V"
        },
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/Hud;chat:Lnet/minecraft/client/gui/components/ChatComponent;")
    )
    private ChatComponent javaReforged$useCurrentClientChat(Hud hud) {
        return LocalChatRouting.component(this.chat);
    }

    @Redirect(
        method = {"updateVignetteBrightness", "extractVignette"},
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/gui/Hud;vignetteBrightness:F",
            opcode = Opcodes.GETFIELD
        )
    )
    private float javaReforged$getCurrentClientVignette(Hud hud) {
        LocalClient client = LocalClientAcces.currentOrNull();
        return client != null ? client.render().hud().vignetteBrightness() : this.vignetteBrightness;
    }

    @Redirect(
        method = "updateVignetteBrightness",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/gui/Hud;vignetteBrightness:F",
            opcode = Opcodes.PUTFIELD
        )
    )
    private void javaReforged$setCurrentClientVignette(Hud hud, float value) {
        LocalClient client = LocalClientAcces.currentOrNull();
        if (client != null) {
            client.render().hud().setVignetteBrightness(value);
        } else {
            this.vignetteBrightness = value;
        }
    }
}
