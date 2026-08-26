package net.jr.mixin.runtime;

import net.jr.client.runtime.context.LocalClientScope;
import net.jr.client.runtime.render.pass.HudRenderPass;
import net.jr.client.runtime.render.state.HudStateRouter;
import net.jr.client.runtime.render.state.HudStateScope;
import net.jr.client.runtime.ui.LocalChatRouter;
import net.jr.client.runtime.viewport.ViewportHudLayout;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.components.ChatComponent;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public abstract class HudMixin {
    @Shadow
    @Final
    private ChatComponent chat;

    @Shadow
    private float vignetteBrightness;

    @Inject(
            method = {"resetTitleTimes", "setNowPlaying", "setOverlayMessage", "setTimes", "setSubtitle", "setTitle", "clearTitles"},
            at = @At("HEAD")
    )
    private void javaReforged$installHudState(CallbackInfo callback) {
        if (LocalClientScope.currentClientOrNull() != null) {
            HudStateScope.begin((Hud)(Object)this);
        }
    }

    @Inject(
            method = {"resetTitleTimes", "setNowPlaying", "setOverlayMessage", "setTimes", "setSubtitle", "setTitle", "clearTitles"},
            at = @At("RETURN")
    )
    private void javaReforged$captureHudState(CallbackInfo callback) {
        if (LocalClientScope.currentClientOrNull() != null) {
            HudStateScope.end((Hud)(Object)this);
        }
    }

    @Redirect(
            method = "tick()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/ChatComponent;tick()V"
            )
    )
    private void javaReforged$tickGlobalChatOnce(ChatComponent chat) {
        HudRenderPass.tickChat(chat);
    }

    @Redirect(
            method = {
                    "extractChat(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V",
                    "tick()V",
                    "getChat()Lnet/minecraft/client/gui/components/ChatComponent;",
                    "onDisconnected()V"
            },
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/gui/Hud;chat:Lnet/minecraft/client/gui/components/ChatComponent;"
            )
    )
    private ChatComponent javaReforged$useCurrentClientChat(Hud hud) {
        return LocalChatRouter.component(this.chat);
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
        return HudStateRouter.vignetteBrightness(this.vignetteBrightness);
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
        if (!HudStateRouter.setVignetteBrightness(value)) {
            this.vignetteBrightness = value;
        }
    }

    @Inject(
            method = {
                    "extractHotbar",
                    "extractContextualInfoBarBackground",
                    "extractExperienceLevel",
                    "extractContextualInfoBar",
                    "maybeExtractSelectedItemName",
                    "maybeExtractSpectatorTooltip"
            },
            at = @At("HEAD")
    )
    private void javaReforged$pushBottomHudTransformWithDelta(
            GuiGraphicsExtractor graphics,
            DeltaTracker deltaTracker,
            CallbackInfo ci
    ) {
        ViewportHudLayout.pushBottomTransform(graphics);
    }

    @Inject(
            method = {
                    "extractHotbar",
                    "extractContextualInfoBarBackground",
                    "extractExperienceLevel",
                    "extractContextualInfoBar",
                    "maybeExtractSelectedItemName",
                    "maybeExtractSpectatorTooltip"
            },
            at = @At("RETURN")
    )
    private void javaReforged$popBottomHudTransformWithDelta(
            GuiGraphicsExtractor graphics,
            DeltaTracker deltaTracker,
            CallbackInfo ci
    ) {
        ViewportHudLayout.popBottomTransform(graphics);
    }

    @Inject(
            method = {
                    "extractHealthLevel",
                    "extractArmorLevel",
                    "extractFoodLevel",
                    "extractVehicleHealth",
                    "extractAirLevel"
            },
            at = @At("HEAD")
    )
    private void javaReforged$pushBottomHudTransformWithoutDelta(
            GuiGraphicsExtractor graphics,
            CallbackInfo ci
    ) {
        ViewportHudLayout.pushBottomTransform(graphics);
    }

    @Inject(
            method = {
                    "extractHealthLevel",
                    "extractArmorLevel",
                    "extractFoodLevel",
                    "extractVehicleHealth",
                    "extractAirLevel"
            },
            at = @At("RETURN")
    )
    private void javaReforged$popBottomHudTransformWithoutDelta(
            GuiGraphicsExtractor graphics,
            CallbackInfo ci
    ) {
        ViewportHudLayout.popBottomTransform(graphics);
    }

    @ModifyArg(
            method = "extractItemHotbar",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V",
                    ordinal = 1
            ),
            index = 5
    )
    private int javaReforged$resizeHotbarSelectionHeight(int originalHeight) {
        return ViewportHudLayout.hotbarSelectionHeight();
    }
}
