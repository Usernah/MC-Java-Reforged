package net.jr.mixin.runtime;

import javax.annotation.Nullable;
import net.jr.client.runtime.bridge.GuiScreenAccess;
import net.jr.client.runtime.input.LocalScreenInputHandler;
import net.jr.client.runtime.render.pass.GuiRenderPass;
import net.jr.client.runtime.render.pass.ToastRenderPass;
import net.jr.client.runtime.ui.LocalChatRouter;
import net.jr.client.runtime.ui.LocalScreenManager;
import net.jr.client.runtime.ui.LocalScreenTransitionHandler;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {
    @Shadow @Final
    private ChatListener chatListener;

    @Shadow @Final
    private GuiRenderState guiRenderState;

    @Redirect(
            method = {"tick()V", "chatListener()Lnet/minecraft/client/multiplayer/chat/ChatListener;", "addSocialInteractionsToast()V"},
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/Gui;chatListener:Lnet/minecraft/client/multiplayer/chat/ChatListener;")
    )
    private ChatListener javaReforged$resolveChatListener(Gui gui) {
        return LocalChatRouter.listener(this.chatListener);
    }

    @Redirect(
            method = "update",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/toasts/ToastManager;update()V")
    )
    private void javaReforged$updateToastStates(ToastManager manager) {
        ToastRenderPass.update(manager);
    }

    @Overwrite
    public void extractRenderState(DeltaTracker deltaTracker, boolean shouldRenderLevel, boolean gameLoadFinished) {
        GuiRenderPass.extract(
                (Gui) (Object) this,
                this.guiRenderState,
                deltaTracker,
                shouldRenderLevel,
                gameLoadFinished
        );
    }

    @Redirect(
            method = "setScreen",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/gui/Gui;screen:Lnet/minecraft/client/gui/screens/Screen;",
                    opcode = Opcodes.PUTFIELD
            )
    )
    private void javaReforged$captureScreenWrite(Gui gui, @Nullable Screen screen) {
        GuiScreenAccess.setScreen(gui, screen);
    }

    @Redirect(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V")
    )
    private void javaReforged$routeDeathScreen(Gui gui, Screen screen) {
        gui.setScreen(screen);
    }

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void javaReforged$handleScreenTransition(@Nullable Screen screen, CallbackInfo callback) {
        if (LocalScreenTransitionHandler.shouldCancelSetScreen(screen)) {
            callback.cancel();
        }
    }

    @Redirect(
            method = "setScreen",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;init(II)V")
    )
    private void javaReforged$initializeScreen(Screen screen, int width, int height) {
        LocalScreenManager.initialize(screen, Minecraft.getInstance(), width, height);
    }

    @Redirect(
            method = "setScreen",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MouseHandler;releaseMouse()V"),
            require = 0
    )
    private void javaReforged$releaseMouse(MouseHandler mouseHandler) {
        LocalScreenInputHandler.releaseMouse(mouseHandler);
    }

    @Redirect(
            method = "setScreen",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MouseHandler;grabMouse()V"),
            require = 0
    )
    private void javaReforged$grabMouse(MouseHandler mouseHandler) {
        LocalScreenInputHandler.grabMouse(mouseHandler);
    }
}
