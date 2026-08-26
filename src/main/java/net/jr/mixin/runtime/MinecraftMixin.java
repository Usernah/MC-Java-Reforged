package net.jr.mixin.runtime;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.client.LocalClientReadinessPolicy;
import net.jr.client.runtime.context.PrimaryTickScope;
import net.jr.client.runtime.context.SlotResolver;
import net.jr.client.runtime.input.LocalScreenInputHandler;
import net.jr.client.runtime.network.ClientPacketHandler;
import net.jr.client.runtime.network.PacketExecutionRouter;
import net.jr.client.runtime.player.BedrockBridgeUseHandler;
import net.jr.client.runtime.render.pass.HudRenderPass;
import net.jr.client.runtime.render.pass.WorldExtractionPass;
import net.jr.client.runtime.session.LocalClientDisconnectHandler;
import net.jr.client.runtime.ui.LocalScreenManager;
import net.jr.client.runtime.ui.LocalScreenTransitionHandler;
import net.minecraft.client.InputType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Shadow
    private boolean pause;

    @Inject(method = "getLastInputType", at = @At("HEAD"), cancellable = true)
    private void javaReforged$getSlotLastInputType(CallbackInfoReturnable<InputType> cir) {
        cir.setReturnValue(SlotResolver.requireActiveOrPrimary().inputState().uiNavigation().lastInputType());
    }

    @Inject(method = "setLastInputType", at = @At("HEAD"), cancellable = true)
    private void javaReforged$setSlotLastInputType(InputType inputType, CallbackInfo ci) {
        SlotResolver.requireActiveOrPrimary().inputState().uiNavigation().setLastInputType(inputType);
        ci.cancel();
    }

    @Redirect(
        method = {"tick", "handleKeybinds", "continueAttack", "startAttack"},
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;missTime:I", opcode = Opcodes.GETFIELD)
    )
    private int javaReforged$readSlotMissTime(Minecraft minecraft) {
        return SlotResolver.requireActiveOrPrimary().gameplayState().missTime();
    }

    @Redirect(
        method = {"tick", "handleKeybinds", "continueAttack", "startAttack"},
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;missTime:I", opcode = Opcodes.PUTFIELD)
    )
    private void javaReforged$writeSlotMissTime(Minecraft minecraft, int missTime) {
        SlotResolver.requireActiveOrPrimary().gameplayState().setMissTime(missTime);
    }

    @Redirect(
        method = {"tick", "handleKeybinds", "startUseItem"},
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;rightClickDelay:I", opcode = Opcodes.GETFIELD)
    )
    private int javaReforged$readSlotRightClickDelay(Minecraft minecraft) {
        return SlotResolver.requireActiveOrPrimary().gameplayState().rightClickDelay();
    }

    @Redirect(
        method = {"tick", "handleKeybinds", "startUseItem"},
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;rightClickDelay:I", opcode = Opcodes.PUTFIELD)
    )
    private void javaReforged$writeSlotRightClickDelay(Minecraft minecraft, int rightClickDelay) {
        SlotResolver.requireActiveOrPrimary().gameplayState().setRightClickDelay(rightClickDelay);
    }

    @Inject(method = "getConnection", at = @At("HEAD"), cancellable = true)
    private void javaReforged$getActiveSlotConnection(CallbackInfoReturnable<ClientPacketListener> cir) {
        cir.setReturnValue(ClientPacketHandler.activeConnection());
    }

    @Inject(method = "runTick", at = @At("HEAD"))
    private void javaReforged$enterPrimaryRunTick(boolean renderLevel, CallbackInfo ci) {
        Minecraft minecraft = (Minecraft)(Object)this;
        PrimaryTickScope.begin(minecraft);
    }

    @Inject(method = "runTick", at = @At("RETURN"))
    private void javaReforged$exitPrimaryRunTick(boolean renderLevel, CallbackInfo ci) {
        PrimaryTickScope.end();
    }

    @Redirect(method = "renderFrame", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;pick(F)V"))
    private void javaReforged$pickForVisiblePlayers(Minecraft minecraft, float partialTicks) {
        WorldExtractionPass.pickVisibleSlots(minecraft, partialTicks);
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void javaReforged$tickSecondarySessions(CallbackInfo ci) {
        ClientRuntime.INSTANCE.clients().tickSecondarySessions((Minecraft)(Object)this);
    }

    @Inject(method = "handleKeybinds", at = @At("HEAD"), cancellable = true)
    private void javaReforged$skipKeybindsUntilSlotGameplayReady(CallbackInfo ci) {
        if (!LocalClientReadinessPolicy.currentGameplayReady()) {
            ci.cancel();
        }
    }

    @Redirect(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MouseHandler;isMouseGrabbed()Z"))
    private boolean javaReforged$usePerClientGameplayCapture(MouseHandler mouseHandler) {
        return LocalScreenInputHandler.isGameplayCaptured((Minecraft) (Object) this, mouseHandler);
    }

    @Inject(method = "wrapRunnable", at = @At("HEAD"), cancellable = true)
    private void javaReforged$wrapRunnableWithSlot(Runnable runnable, CallbackInfoReturnable<Runnable> cir) {
        cir.setReturnValue(PacketExecutionRouter.wrapScheduled(runnable));
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;tick()V"))
    private void javaReforged$tickHudBySlot(Gui gui) {
        HudRenderPass.tick(gui);
    }

    @Inject(method = "pauseGame", at = @At("HEAD"), cancellable = true)
    private void javaReforged$makePauseScreenLocal(boolean pauseOnly, CallbackInfo ci) {
        LocalScreenManager.openLocalPause((Minecraft)(Object)this, pauseOnly);
        ci.cancel();
    }

    @Inject(method = "isPaused", at = @At("HEAD"), cancellable = true)
    private void javaReforged$neverFreezeForLocalMultiplayer(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    @Redirect(
        method = {"runTick", "tick"},
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;pause:Z", opcode = Opcodes.GETFIELD)
    )
    private boolean javaReforged$readPauseAsFalseForLocalMultiplayer(Minecraft minecraft) {
        return false;
    }

    @Redirect(
        method = "runTick",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;pause:Z", opcode = Opcodes.PUTFIELD)
    )
    private void javaReforged$writePauseAsFalseForLocalMultiplayer(Minecraft minecraft, boolean pause) {
        this.pause = false;
    }

    @Redirect(
        method = "<init>",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V")
    )
    private void javaReforged$bootstrapPrimaryScreen(Gui gui, Screen screen) {
        LocalScreenManager.bootstrapPrimary((Minecraft) (Object) this, screen);
    }

    @Inject(method = "runTick", at = @At("TAIL"))
    private void javaReforged$closeCompletedTestJoiningScreen(boolean renderLevel, CallbackInfo ci) {
        LocalScreenTransitionHandler.closeCompletedPrimaryJoiningScreen((Minecraft) (Object) this);
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void javaReforged$tickSecondaryScreens(CallbackInfo ci) {
        LocalScreenManager.tickSecondary();
    }

    @Redirect(method = "resizeGui", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;resize(II)V"))
    private void javaReforged$resizeScreensThroughLocalClients(Screen screen, int width, int height) {
    }

    @Inject(method = "resizeGui", at = @At("RETURN"))
    private void javaReforged$refreshViewportsAfterResize(CallbackInfo ci) {
        ClientRuntime.INSTANCE.viewportResize().refreshWindow((Minecraft)(Object)this);
    }

    @Inject(
            method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;ZZ)V",
            at = @At("HEAD")
    )
    private void javaReforged$resetLocalRuntimeAfterDisconnect(
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
    private InteractionResult javaReforged$useBedrockBridge(
            MultiPlayerGameMode gameMode,
            Player player,
            InteractionHand hand,
            Operation<InteractionResult> original
    ) {
        InteractionResult result = BedrockBridgeUseHandler.tryUse(gameMode, player, hand);
        if (result != null) {
            return result;
        }

        return original.call(gameMode, player, hand);
    }
}
