package net.jr.mixin.runtime;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.jr.api.client.local.Client;
import net.jr.api.client.split.SplitScreen;
import net.jr.client.runtime.render.pass.BottomHudPass;
import net.jr.client.runtime.render.pass.HudPass;
import net.jr.client.runtime.render.state.HudCameraOverlayScope;
import net.jr.client.runtime.render.state.HudStateRouter;
import net.jr.client.runtime.render.state.HudStateScope;
import net.jr.client.runtime.ui.ChatRouter;
import net.jr.client.runtime.viewport.ViewportHudLayout;
import net.jr.client.ui.presentation.UiPresentation;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public abstract class HudMixin {
    @Shadow
    @Final
    private ChatComponent chat;

    @Shadow
    private float vignetteBrightness;

    @Inject(
            method = "extractCameraOverlays",
            at = @At("HEAD"),
            cancellable = true
    )
    private void javaReforged$skipCameraEffectsInsideTransparentHud(
            GuiGraphicsExtractor graphics,
            DeltaTracker deltaTracker,
            CallbackInfo ci
    ) {
        if (HudCameraOverlayScope.isSuppressed()) {
            ci.cancel();
        }
    }

    @Inject(
            method = {"resetTitleTimes", "setNowPlaying", "setOverlayMessage", "setTimes", "setSubtitle", "setTitle", "clearTitles"},
            at = @At("HEAD")
    )
    private void javaReforged$installHudState(CallbackInfo callback) {
        if (Client.currentOrNull() != null) {
            HudStateScope.begin((Hud)(Object)this);
        }
    }

    @Inject(
            method = {"resetTitleTimes", "setNowPlaying", "setOverlayMessage", "setTimes", "setSubtitle", "setTitle", "clearTitles"},
            at = @At("RETURN")
    )
    private void javaReforged$captureHudState(CallbackInfo callback) {
        if (Client.currentOrNull() != null) {
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
        HudPass.tickChat(chat);
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
        return ChatRouter.component(this.chat);
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
        return Client.currentOrNull() != null
                ? HudStateRouter.vignetteBrightness()
                : this.vignetteBrightness;
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
        if (Client.currentOrNull() != null) {
            HudStateRouter.setVignetteBrightness(value);
        } else {
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
            at = @At("HEAD"),
            cancellable = true
    )
    private void javaReforged$routeBottomHudWithDeltaToFramebuffer(
            GuiGraphicsExtractor graphics,
            DeltaTracker deltaTracker,
            CallbackInfo ci
    ) {
        if (BottomHudPass.isVanillaExtractionSuppressed()) {
            ci.cancel();
        }
    }

    @Inject(
            method = {
                    "extractHealthLevel",
                    "extractArmorLevel",
                    "extractFoodLevel",
                    "extractVehicleHealth",
                    "extractAirLevel"
            },
            at = @At("HEAD"),
            cancellable = true
    )
    private void javaReforged$routeBottomHudWithoutDeltaToFramebuffer(
            GuiGraphicsExtractor graphics,
            CallbackInfo ci
    ) {
        if (BottomHudPass.isVanillaExtractionSuppressed()) {
            ci.cancel();
        }
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



    @ModifyVariable(
            method = "extractSelectedItemName(Lnet/minecraft/client/gui/GuiGraphicsExtractor;I)V",
            at = @At("STORE"),
            name = "highlightTip")
    private Component javaReforged$selectedItemFont(Component original) {
        return UiPresentation.currentFont().apply(original);
    }

    @ModifyConstant(method = "extractCrosshair", constant = @Constant(intValue = 15))
    private int cambiarTamanoMira(int original) {
        return original * (int)Client.current().options().bottomHudScaleMultiplier(SplitScreen.isActive());
    }

    @ModifyExpressionValue(
            method = "nextContextualInfoState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/waypoints/ClientWaypointManager;hasWaypoints()Z"
            )
    )
    private boolean javaReforged$applyLocatorBarPreference(boolean hasWaypoints) {
        return hasWaypoints && Client.current().options().showLocatorBar().get();
    }


}
