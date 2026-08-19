package net.jr.mixin.runtime;

import java.util.ArrayDeque;
import java.util.Deque;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.client.LocalClient;
import net.jr.client.runtime.client.LocalClientReadinessPolicy;
import net.jr.client.runtime.context.LocalClientExecution;
import net.jr.client.runtime.context.LocalClientScope;
import net.jr.client.runtime.context.SlotExecution;
import net.jr.client.runtime.context.SlotScope;
import net.jr.client.runtime.player.BedrockBridgePlacement;
import net.jr.client.runtime.render.pass.HudRenderPass;
import net.jr.client.runtime.render.pass.WorldExtractionPass;
import net.jr.client.runtime.session.LocalClientDisconnectHandler;
import net.jr.client.runtime.slot.LocalClientSlot;
import net.jr.client.runtime.ui.LocalScreenManager;
import net.jr.client.runtime.ui.LocalScreenTransitionHandler;
import net.minecraft.client.InputType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftSSMixin {
    @Unique
    private Deque<LocalClientExecution.Scope> splitTest$runTickScopes;

    @Shadow
    private boolean pause;

    @Inject(method = "getLastInputType", at = @At("HEAD"), cancellable = true)
    private void splitTest$getSlotLastInputType(CallbackInfoReturnable<InputType> cir) {
        cir.setReturnValue(this.splitTest$activeSlot().inputState().uiNavigation().lastInputType());
    }

    @Inject(method = "setLastInputType", at = @At("HEAD"), cancellable = true)
    private void splitTest$setSlotLastInputType(InputType inputType, CallbackInfo ci) {
        this.splitTest$activeSlot().inputState().uiNavigation().setLastInputType(inputType);
        ci.cancel();
    }

    @Redirect(
        method = {"tick", "handleKeybinds", "continueAttack", "startAttack"},
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;missTime:I", opcode = Opcodes.GETFIELD)
    )
    private int splitTest$readSlotMissTime(Minecraft minecraft) {
        return this.splitTest$activeSlot().gameplayState().missTime();
    }

    @Redirect(
        method = {"tick", "handleKeybinds", "continueAttack", "startAttack"},
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;missTime:I", opcode = Opcodes.PUTFIELD)
    )
    private void splitTest$writeSlotMissTime(Minecraft minecraft, int missTime) {
        this.splitTest$activeSlot().gameplayState().setMissTime(missTime);
    }

    @Redirect(
        method = {"tick", "handleKeybinds", "startUseItem"},
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;rightClickDelay:I", opcode = Opcodes.GETFIELD)
    )
    private int splitTest$readSlotRightClickDelay(Minecraft minecraft) {
        return this.splitTest$activeSlot().gameplayState().rightClickDelay();
    }

    @Redirect(
        method = {"tick", "handleKeybinds", "startUseItem"},
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;rightClickDelay:I", opcode = Opcodes.PUTFIELD)
    )
    private void splitTest$writeSlotRightClickDelay(Minecraft minecraft, int rightClickDelay) {
        this.splitTest$activeSlot().gameplayState().setRightClickDelay(rightClickDelay);
    }

    @Inject(method = "getConnection", at = @At("HEAD"), cancellable = true)
    private void splitTest$getActiveSlotConnection(CallbackInfoReturnable<ClientPacketListener> cir) {
        LocalClient client = LocalClientScope.currentClientOrNull();
        if (client == null) {
            Integer slotId = SlotScope.idOrNull();
            client = ClientRuntime.INSTANCE.clients().clientOrNull(slotId != null ? slotId : 0);
        }
        LocalPlayer player = client != null ? client.player() : null;
        cir.setReturnValue(player != null ? player.connection : null);
    }

    @Inject(method = "runTick", at = @At("HEAD"))
    private void splitTest$enterPrimaryRunTick(boolean renderLevel, CallbackInfo ci) {
        Minecraft minecraft = (Minecraft)(Object)this;
        ClientRuntime.INSTANCE.viewportResize().refreshWindow(minecraft);
        if (this.splitTest$runTickScopes == null) {
            this.splitTest$runTickScopes = new ArrayDeque<>();
        }
        this.splitTest$runTickScopes.push(LocalClientExecution.enterPrimary(minecraft));
    }

    @Inject(method = "runTick", at = @At("RETURN"))
    private void splitTest$exitPrimaryRunTick(boolean renderLevel, CallbackInfo ci) {
        if (this.splitTest$runTickScopes == null || this.splitTest$runTickScopes.isEmpty()) {
            throw new IllegalStateException("Primary runTick scope was not opened");
        }
        this.splitTest$runTickScopes.pop().close();
    }

    @Redirect(method = "renderFrame", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;pick(F)V"))
    private void splitTest$pickForVisiblePlayers(Minecraft minecraft, float partialTicks) {
        WorldExtractionPass.pickVisibleSlots(minecraft, partialTicks);
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void splitTest$tickSecondarySessions(CallbackInfo ci) {
        ClientRuntime.INSTANCE.clients().tickSecondarySessions((Minecraft)(Object)this);
    }

    @Inject(method = "handleKeybinds", at = @At("HEAD"), cancellable = true)
    private void splitTest$skipKeybindsUntilSlotGameplayReady(CallbackInfo ci) {
        LocalClient client = LocalClientScope.currentClientOrNull();
        if (client == null || !LocalClientReadinessPolicy.gameplayReady(client)) {
            ci.cancel();
        }
    }

    @Redirect(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MouseHandler;isMouseGrabbed()Z"))
    private boolean splitTest$usePerClientGameplayCapture(MouseHandler mouseHandler) {
        LocalClient client = LocalClientScope.currentClientOrNull();
        if (client != null && client.slotId() > 0 && ClientRuntime.INSTANCE.clients().count() > 1) {
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
        LocalScreenManager.openLocalPause((Minecraft)(Object)this, pauseOnly);
        ci.cancel();
    }

    @Inject(method = "isPaused", at = @At("HEAD"), cancellable = true)
    private void splitTest$neverFreezeForLocalMultiplayer(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    @Redirect(
        method = {"runTick", "tick"},
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;pause:Z", opcode = Opcodes.GETFIELD)
    )
    private boolean splitTest$readPauseAsFalseForLocalMultiplayer(Minecraft minecraft) {
        return false;
    }

    @Redirect(
        method = "runTick",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;pause:Z", opcode = Opcodes.PUTFIELD)
    )
    private void splitTest$writePauseAsFalseForLocalMultiplayer(Minecraft minecraft, boolean pause) {
        this.pause = false;
    }

    @Redirect(
        method = "<init>",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V")
    )
    private void splitTest$bootstrapPrimaryScreen(Gui gui, Screen screen) {
        Minecraft minecraft = (Minecraft)(Object)this;
        SlotExecution.runPrimary(minecraft, () -> gui.setScreen(screen));
    }

    @Inject(method = "runTick", at = @At("TAIL"))
    private void splitTest$closeCompletedTestJoiningScreen(boolean renderLevel, CallbackInfo ci) {
        LocalScreenTransitionHandler.closeCompletedPrimaryJoiningScreen(
            (Minecraft)(Object)this,
            ClientRuntime.INSTANCE.slots().primary().screenState().screen()
        );
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void splitTest$tickSecondaryScreens(CallbackInfo ci) {
        LocalScreenManager.tickSecondary();
    }

    @Redirect(method = "resizeGui", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;resize(II)V"))
    private void splitTest$resizeScreensThroughLocalClients(Screen screen, int width, int height) {
    }

    @Inject(method = "resizeGui", at = @At("RETURN"))
    private void splitTest$refreshViewportsAfterResize(CallbackInfo ci) {
        ClientRuntime.INSTANCE.viewportResize().refreshWindow((Minecraft)(Object)this);
    }

    @Unique
    private LocalClientSlot splitTest$activeSlot() {
        Integer slotId = SlotScope.idOrNull();
        return ClientRuntime.INSTANCE.slots().slot(slotId != null ? slotId : 0);
    }

    @Inject(
            method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;ZZ)V",
            at = @At("TAIL")
    )
    private void splitTest$resetLocalRuntimeAfterDisconnect(
            Screen nextScreen,
            boolean keepResourcePacks,
            boolean stopSound,
            CallbackInfo ci
    ) {
        LocalClientDisconnectHandler.returnToPrimaryOnly(
                (Minecraft)(Object)this
        );
    }

    @WrapOperation(
            method = "startUseItem",
            at = @At(
                    value = "INVOKE",
                    target =
                            "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;" +
                                    "useItem(" +
                                    "Lnet/minecraft/world/entity/player/Player;" +
                                    "Lnet/minecraft/world/InteractionHand;" +
                                    ")Lnet/minecraft/world/InteractionResult;"
            )
    )
    private InteractionResult splitTest$useBedrockBridge(
            MultiPlayerGameMode gameMode,
            Player player,
            InteractionHand hand,
            Operation<InteractionResult> original
    ) {
        if (player instanceof LocalPlayer localPlayer) {
            var candidate =
                    BedrockBridgePlacement.resolve(localPlayer, hand);

            if (candidate != null) {
                InteractionResult result = gameMode.useItemOn(
                        localPlayer,
                        hand,
                        candidate.hitResult()
                );

                // Igual que el bloque anterior de startUseItem():
                // Success/Fail terminan la interacción.
                if (result instanceof InteractionResult.Success
                        || result instanceof InteractionResult.Fail) {
                    return result;
                }
            }
        }

        return original.call(gameMode, player, hand);
    }
}
