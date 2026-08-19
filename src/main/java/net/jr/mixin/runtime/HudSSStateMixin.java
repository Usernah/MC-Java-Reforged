package net.jr.mixin.runtime;

import net.jr.client.runtime.client.LocalClient;
import net.jr.client.runtime.context.LocalClientScope;
import net.jr.client.runtime.render.pass.HudRenderPass;
import net.jr.client.runtime.render.state.HudStateScope;
import net.jr.client.runtime.ui.LocalChatRouter;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.components.ChatComponent;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public abstract class HudSSStateMixin {
    @Shadow
    @Final
    private ChatComponent chat;

    @Shadow
    private float vignetteBrightness;

    @Unique
    private static final float BOTTOM_HUD_SCALE = 1.0F;

    @Unique
    private static final float BOTTOM_HUD_Y_OFFSET = -22.0F;

    @Unique
    private static final float BOTTOM_HUD_PIVOT_Y_OFFSET = 1.0F;

    @Inject(
            method = {"resetTitleTimes", "setNowPlaying", "setOverlayMessage", "setTimes", "setSubtitle", "setTitle", "clearTitles"},
            at = @At("HEAD")
    )
    private void splitTest$installHudState(CallbackInfo callback) {
        if (LocalClientScope.currentClientOrNull() != null) {
            HudStateScope.begin((Hud)(Object)this);
        }
    }

    @Inject(
            method = {"resetTitleTimes", "setNowPlaying", "setOverlayMessage", "setTimes", "setSubtitle", "setTitle", "clearTitles"},
            at = @At("RETURN")
    )
    private void splitTest$captureHudState(CallbackInfo callback) {
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
        LocalClient client = LocalClientScope.currentClientOrNull();
        return client != null
                ? client.render().hud().vignetteBrightness()
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
        LocalClient client = LocalClientScope.currentClientOrNull();
        if (client != null) {
            client.render().hud().setVignetteBrightness(value);
        } else {
            this.vignetteBrightness = value;
        }
    }

    @Unique
    private static void javaReforged$pushBottomHudTransform(
            GuiGraphicsExtractor graphics
    ) {
        float pivotX = graphics.guiWidth() * 0.5F;
        float pivotY = graphics.guiHeight() + BOTTOM_HUD_PIVOT_Y_OFFSET;

        graphics.pose().pushMatrix();
        graphics.pose().translate(
                pivotX,
                pivotY + BOTTOM_HUD_Y_OFFSET
        );
        graphics.pose().scale(BOTTOM_HUD_SCALE);
        graphics.pose().translate(
                -pivotX,
                -pivotY
        );
    }

    @Unique
    private static void javaReforged$popBottomHudTransform(
            GuiGraphicsExtractor graphics
    ) {
        graphics.pose().popMatrix();
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
        javaReforged$pushBottomHudTransform(graphics);
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
        javaReforged$popBottomHudTransform(graphics);
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
        javaReforged$pushBottomHudTransform(graphics);
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
        javaReforged$popBottomHudTransform(graphics);
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
        return 24;
    }
}