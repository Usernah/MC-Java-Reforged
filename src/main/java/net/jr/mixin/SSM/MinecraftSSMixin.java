package net.jr.mixin.SSM;

import net.jr.ClientRuntime.runtime.*;
import net.jr.client.input.InputApi;
import net.jr.ClientRuntime.test.ScreenProbe;
import net.minecraft.client.Minecraft;
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
    private Deque<ClientBoundary.Scope> splitTest$runTickScopes;

    @Shadow
    private boolean pause;

    @Redirect(
            method = {"tick", "handleKeybinds", "continueAttack", "startAttack"},
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;missTime:I", opcode = Opcodes.GETFIELD)
    )
    private int splitTest$readSlotMissTime(Minecraft minecraft) {
        return Client.gameplay().missTime();
    }

    @Redirect(
            method = {"tick", "handleKeybinds", "continueAttack", "startAttack"},
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;missTime:I", opcode = Opcodes.PUTFIELD)
    )
    private void splitTest$writeSlotMissTime(Minecraft minecraft, int missTime) {
        Client.setMissTime(missTime);
    }

    @Redirect(
            method = {"tick", "handleKeybinds", "startUseItem"},
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;rightClickDelay:I", opcode = Opcodes.GETFIELD)
    )
    private int splitTest$readSlotRightClickDelay(Minecraft minecraft) {
        return Client.gameplay().rightClickDelay();
    }

    @Redirect(
            method = {"tick", "handleKeybinds", "startUseItem"},
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;rightClickDelay:I", opcode = Opcodes.PUTFIELD)
    )
    private void splitTest$writeSlotRightClickDelay(Minecraft minecraft, int rightClickDelay) {
        Client.setRightClickDelay(rightClickDelay);
    }

    @Inject(method = "getConnection", at = @At("HEAD"), cancellable = true)
    private void splitTest$getActiveSlotConnection(CallbackInfoReturnable<ClientPacketListener> cir) {
        LocalClient client = Client.currentOrNull();
        LocalPlayer player = client == null ? null : client.player();
        ClientPacketListener listener = player == null ? null : player.connection;
        cir.setReturnValue(listener);
    }

    @Inject(method = "runTick", at = @At("HEAD"))
    private void splitTest$enterPrimaryRunTick(boolean renderLevel, CallbackInfo ci) {
        LocalPlayers.INSTANCE.refreshWindow((Minecraft)(Object)this);
        if (this.splitTest$runTickScopes == null) {
            this.splitTest$runTickScopes = new ArrayDeque<>();
        }
        this.splitTest$runTickScopes.push(ClientBoundary.enterPrimary((Minecraft)(Object)this));
    }

    @Inject(method = "runTick", at = @At("RETURN"))
    private void splitTest$exitPrimaryRunTick(boolean renderLevel, CallbackInfo ci) {
        if (this.splitTest$runTickScopes == null || this.splitTest$runTickScopes.isEmpty()) {
            throw new IllegalStateException("Primary runTick scope was not opened");
        }
        this.splitTest$runTickScopes.pop().close();
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void splitTest$tickSecondarySessions(CallbackInfo ci) {
        Minecraft minecraft = (Minecraft)(Object)this;
        LocalPlayers.INSTANCE.sessions().tickSecondarySessions(minecraft, LocalPlayers.INSTANCE);
    }

    @Inject(method = "handleKeybinds", at = @At("HEAD"), cancellable = true)
    private void splitTest$skipKeybindsUntilSlotGameplayReady(CallbackInfo ci) {
        if (!Client.gameplayReady()) {
            ci.cancel();
        }
    }

    @Inject(method = "wrapRunnable", at = @At("HEAD"), cancellable = true)
    private void splitTest$wrapRunnableWithSlot(Runnable runnable, CallbackInfoReturnable<Runnable> cir) {
        cir.setReturnValue(ClientBoundary.wrapScheduled(runnable));
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;tick()V"))
    private void splitTest$tickHudBySlot(Gui gui) {
        HudPass.tick(gui);
    }

    @Inject(method = "pauseGame", at = @At("HEAD"), cancellable = true)
    private void splitTest$makePauseScreenLocal(boolean pauseOnly, CallbackInfo ci) {
        if (!Screens.usesLocalPause()) {
            return;
        }
        Screens.openLocalPause((Minecraft)(Object)this, pauseOnly);
        ci.cancel();
    }

    @Inject(method = "isPaused", at = @At("HEAD"), cancellable = true)
    private void splitTest$neverFreezeForLocalMultiplayer(CallbackInfoReturnable<Boolean> cir) {
        if (Screens.usesLocalPause()) {
            cir.setReturnValue(false);
        }
    }

    @Redirect(
            method = {"runTick", "tick"},
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;pause:Z", opcode = Opcodes.GETFIELD)
    )
    private boolean splitTest$readPauseAsFalseForLocalMultiplayer(Minecraft minecraft) {
        if (Screens.usesLocalPause()) {
            return false;
        }
        return this.pause;
    }

    @Redirect(
            method = "runTick",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;pause:Z", opcode = Opcodes.PUTFIELD)
    )
    private void splitTest$writePauseAsFalseForLocalMultiplayer(Minecraft minecraft, boolean pause) {
        this.pause = Screens.usesLocalPause() ? false : pause;
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
        ClientBoundary.runPrimary(minecraft, () -> gui.setScreen(screen));
    }

    @Inject(method = "runTick", at = @At("TAIL"))
    private void splitTest$closeCompletedTestJoiningScreen(boolean renderLevel, CallbackInfo ci) {
        ScreenProbe.closeCompletedPrimaryJoiningScreen((Minecraft)(Object)this, Client.screen(0));
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void splitTest$tickSecondaryScreens(CallbackInfo ci) {
        Screens.tickSecondary();
    }

    @Redirect(
            method = "resizeGui",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/Screen;resize(II)V"
            )
    )
    private void splitTest$resizeScreensThroughLocalClients(Screen screen, int width, int height) {
        if (!Screens.slotUiPassOwnsScreens()) {
            screen.resize(width, height);
        }
    }

    @Inject(method = "resizeGui", at = @At("RETURN"))
    private void splitTest$refreshViewportsAfterResize(CallbackInfo ci) {
        LocalPlayers.INSTANCE.refreshWindow((Minecraft)(Object)this);
    }
}
