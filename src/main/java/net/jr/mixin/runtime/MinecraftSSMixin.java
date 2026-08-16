package net.jr.mixin.runtime;

import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.context.LocalClientAcces;
import net.jr.client.runtime.context.LocalClientExecution;
import net.jr.client.runtime.context.LocalClient;
import net.jr.client.runtime.render.pass.HudRenderPass;
import net.jr.client.runtime.render.pass.WorldExtractionPass;
import net.jr.client.runtime.ui.LocalScreenTransitionHandler;
import net.jr.client.runtime.ui.LocalScreenManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.InputType;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayDeque;
import java.util.Deque;

@Mixin(Minecraft.class)
public abstract class MinecraftSSMixin {

    @Unique
    private Deque<LocalClientExecution.Scope> splitTest$runTickScopes;

    @Shadow
    private boolean pause;

    @Inject(method = "getLastInputType", at = @At("HEAD"), cancellable = true)
    private void splitTest$getSlotLastInputType(CallbackInfoReturnable<InputType> cir) {
        LocalClient client = LocalClientAcces.currentOrNull();
        if (client != null) {
            cir.setReturnValue(client.input().uiNavigation().lastInputType());
        }
    }

    @Inject(method = "setLastInputType", at = @At("HEAD"), cancellable = true)
    private void splitTest$setSlotLastInputType(InputType inputType, CallbackInfo ci) {
        LocalClient client = LocalClientAcces.currentOrNull();
        if (client != null) {
            client.input().uiNavigation().setLastInputType(inputType);
            ci.cancel();
        }
    }

    @Redirect(
            method = {"tick", "handleKeybinds", "continueAttack", "startAttack"},
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;missTime:I", opcode = Opcodes.GETFIELD)
    )
    private int splitTest$readSlotMissTime(Minecraft minecraft) {
        return LocalClientAcces.gameplay().missTime();
    }

    @Redirect(
            method = {"tick", "handleKeybinds", "continueAttack", "startAttack"},
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;missTime:I", opcode = Opcodes.PUTFIELD)
    )
    private void splitTest$writeSlotMissTime(Minecraft minecraft, int missTime) {
        LocalClientAcces.setMissTime(missTime);
    }

    @Redirect(
            method = {"tick", "handleKeybinds", "startUseItem"},
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;rightClickDelay:I", opcode = Opcodes.GETFIELD)
    )
    private int splitTest$readSlotRightClickDelay(Minecraft minecraft) {
        return LocalClientAcces.gameplay().rightClickDelay();
    }

    @Redirect(
            method = {"tick", "handleKeybinds", "startUseItem"},
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;rightClickDelay:I", opcode = Opcodes.PUTFIELD)
    )
    private void splitTest$writeSlotRightClickDelay(Minecraft minecraft, int rightClickDelay) {
        LocalClientAcces.setRightClickDelay(rightClickDelay);
    }

    @Inject(method = "getConnection", at = @At("HEAD"), cancellable = true)
    private void splitTest$getActiveSlotConnection(CallbackInfoReturnable<ClientPacketListener> cir) {
        LocalClient client = LocalClientAcces.currentOrNull();
        LocalPlayer player = client == null ? null : client.player();
        ClientPacketListener listener = player == null ? null : player.connection;
        cir.setReturnValue(listener);
    }

    @Inject(method = "runTick", at = @At("HEAD"))
    private void splitTest$enterPrimaryRunTick(boolean renderLevel, CallbackInfo ci) {
        ClientRuntime.INSTANCE.refreshWindow((Minecraft)(Object)this);
        if (this.splitTest$runTickScopes == null) {
            this.splitTest$runTickScopes = new ArrayDeque<>();
        }
        this.splitTest$runTickScopes.push(LocalClientExecution.enterPrimary((Minecraft)(Object)this));
    }

    @Inject(method = "runTick", at = @At("RETURN"))
    private void splitTest$exitPrimaryRunTick(boolean renderLevel, CallbackInfo ci) {
        if (this.splitTest$runTickScopes == null || this.splitTest$runTickScopes.isEmpty()) {
            throw new IllegalStateException("Primary runTick scope was not opened");
        }
        this.splitTest$runTickScopes.pop().close();
    }

    @Redirect(
            method = "renderFrame",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;pick(F)V")
    )
    private void splitTest$pickForVisiblePlayers(Minecraft minecraft, float partialTicks) {
        WorldExtractionPass.pickVisibleSlots(minecraft, partialTicks);
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void splitTest$tickSecondarySessions(CallbackInfo ci) {
        Minecraft minecraft = (Minecraft)(Object)this;
        ClientRuntime.INSTANCE.sessions().tickSecondarySessions(minecraft, ClientRuntime.INSTANCE);
    }

    @Inject(method = "handleKeybinds", at = @At("HEAD"), cancellable = true)
    private void splitTest$skipKeybindsUntilSlotGameplayReady(CallbackInfo ci) {
        if (!LocalClientAcces.gameplayReady()) {
            ci.cancel();
        }
    }

    @Redirect(
            method = "handleKeybinds",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/MouseHandler;isMouseGrabbed()Z"
            )
    )
    private boolean splitTest$usePerClientGameplayCapture(MouseHandler mouseHandler) {
        LocalClient client = LocalClientAcces.currentOrNull();
        if (client != null && client.slotId() > 0 && LocalClientAcces.connectedCount() > 1) {
            return ((Minecraft)(Object)this).isWindowActive();
        }
        return mouseHandler.isMouseGrabbed();
    }

    @Inject(method = "wrapRunnable", at = @At("HEAD"), cancellable = true)
    private void splitTest$wrapRunnableWithSlot(Runnable runnable, CallbackInfoReturnable<Runnable> cir) {
        cir.setReturnValue(LocalClientExecution.wrapScheduled(runnable));
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;tick()V"))
    private void splitTest$tickHudBySlot(Gui gui) {
        HudRenderPass.tick(gui);
    }

    @Inject(method = "pauseGame", at = @At("HEAD"), cancellable = true)
    private void splitTest$makePauseScreenLocal(boolean pauseOnly, CallbackInfo ci) {
        if (!LocalScreenManager.usesLocalPause()) {
            return;
        }
        LocalScreenManager.openLocalPause((Minecraft)(Object)this, pauseOnly);
        ci.cancel();
    }

    @Inject(method = "isPaused", at = @At("HEAD"), cancellable = true)
    private void splitTest$neverFreezeForLocalMultiplayer(CallbackInfoReturnable<Boolean> cir) {
        if (LocalScreenManager.usesLocalPause()) {
            cir.setReturnValue(false);
        }
    }

    @Redirect(
            method = {"runTick", "tick"},
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;pause:Z", opcode = Opcodes.GETFIELD)
    )
    private boolean splitTest$readPauseAsFalseForLocalMultiplayer(Minecraft minecraft) {
        if (LocalScreenManager.usesLocalPause()) {
            return false;
        }
        return this.pause;
    }

    @Redirect(
            method = "runTick",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;pause:Z", opcode = Opcodes.PUTFIELD)
    )
    private void splitTest$writePauseAsFalseForLocalMultiplayer(Minecraft minecraft, boolean pause) {
        this.pause = LocalScreenManager.usesLocalPause() ? false : pause;
    }

    @Redirect(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/Gui;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V"
            )
    )
    private void splitTest$bootstrapPrimaryScreen(Gui gui, Screen screen) {
        Minecraft minecraft = (Minecraft)(Object)this;
        LocalClientExecution.runPrimary(minecraft, () -> gui.setScreen(screen));
    }

    @Inject(method = "runTick", at = @At("TAIL"))
    private void splitTest$closeCompletedTestJoiningScreen(boolean renderLevel, CallbackInfo ci) {
        LocalScreenTransitionHandler.closeCompletedPrimaryJoiningScreen((Minecraft)(Object)this, LocalClientAcces.screen(0));
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void splitTest$tickSecondaryScreens(CallbackInfo ci) {
        LocalScreenManager.tickSecondary();
    }

    @Redirect(
            method = "resizeGui",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/Screen;resize(II)V"
            )
    )
    private void splitTest$resizeScreensThroughLocalClients(Screen screen, int width, int height) {
        if (!LocalScreenManager.slotUiPassOwnsScreens()) {
            screen.resize(width, height);
        }
    }

    @Inject(method = "resizeGui", at = @At("RETURN"))
    private void splitTest$refreshViewportsAfterResize(CallbackInfo ci) {
        ClientRuntime.INSTANCE.refreshWindow((Minecraft)(Object)this);
    }
}
