package net.jr.mixin.runtime;

import net.jr.client.runtime.bridge.ChatComponentRuntimeBridge;
import net.jr.client.runtime.ui.LocalChatRouter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatComponent.class)
public abstract class ChatComponentRuntimeMixin implements ChatComponentRuntimeBridge {
    @Unique
    private int javaReforged$backgroundColor = ChatComponentRuntimeBridge.DEFAULT_BACKGROUND_COLOR;
    @Unique
    private int javaReforged$lastViewportWidth = -1;

    @Override
    public int javaReforged$getBackgroundColor() {
        return this.javaReforged$backgroundColor;
    }

    @Override
    public void javaReforged$setBackgroundColor(int rgb) {
        this.javaReforged$backgroundColor = rgb & 0xFFFFFF;
    }

    @Inject(method = "getWidth()I", at = @At("HEAD"), cancellable = true)
    private void javaReforged$useViewportWidth(CallbackInfoReturnable<Integer> callback) {
        Integer viewportWidth = LocalChatRouter.viewportChatWidthOrNull();
        if (viewportWidth != null) {
            callback.setReturnValue(viewportWidth);
        }
    }

    @Inject(
        method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V",
        at = @At("HEAD")
    )
    private void javaReforged$rewrapAfterViewportResize(GuiGraphicsExtractor graphics, Font font, int ticks, int mouseX, int mouseY, ChatComponent.DisplayMode displayMode, boolean changeCursorOnInsertions, CallbackInfo callback) {
        Integer viewportWidth = LocalChatRouter.viewportChatWidthOrNull();
        if (viewportWidth != null && viewportWidth != this.javaReforged$lastViewportWidth) {
            this.javaReforged$lastViewportWidth = viewportWidth;
            ((ChatComponent)(Object)this).rescaleChat();
        }
    }

    @Redirect(
        method = {
            "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
            "processMessageDeletionQueue()V",
            "deleteMessageOrDelay(Lnet/minecraft/network/chat/MessageSignature;)Lnet/minecraft/client/gui/components/ChatComponent$DelayedMessageDeletion;"
        },
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Hud;getGuiTicks()I")
    )
    private int javaReforged$useLocalChatTimestamp(Hud hud) {
        return LocalChatRouter.ticks(hud.getGuiTicks());
    }

    @ModifyVariable(
        method = "extractRenderState(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 1
    )
    private int javaReforged$useLocalChatRenderTime(int ticks) {
        return LocalChatRouter.ticks(ticks);
    }

    @Redirect(
        method = "extractRenderState(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ARGB;black(F)I")
    )
    private int javaReforged$drawConfiguredBackground(float alpha) {
        return ARGB.color(alpha, this.javaReforged$backgroundColor);
    }

    @Redirect(method = "lambda$extractRenderState$1", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ARGB;black(F)I"))
    private static int javaReforged$drawConfiguredMessageBackground(float alpha) {
        return LocalChatRouter.backgroundColor(ARGB.black(alpha));
    }
}
