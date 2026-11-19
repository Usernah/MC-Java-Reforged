package net.jr.mixin.runtime;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.client.ReadinessPolicy;
import net.jr.client.runtime.context.*;
import net.jr.client.runtime.input.ScreenInputHandler;
import net.jr.client.runtime.network.ClientPacketHandler;
import net.jr.client.runtime.network.PacketExecutionRouter;
import net.jr.client.runtime.player.BedrockBridgeUseHandler;
import net.jr.client.runtime.player.LegacyGameplayModes;
import net.jr.client.runtime.render.pass.HudPass;
import net.jr.client.runtime.render.pass.WorldExtractionPass;
import net.jr.client.runtime.render.experimental.ExperimentalRenderController;
import net.jr.client.runtime.session.DisconnectHandler;
import net.jr.client.runtime.ui.SlotScreenManager;
import net.jr.client.runtime.ui.SlotScreenTransitionHandler;
import net.jr.experimental_features.pre_join.PreJoinWorldLoad;
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

    @WrapOperation(
            method = "runTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Minecraft;tick()V"
            )
    )
    private void javaReforged$tickWithOwner(
            Minecraft minecraft,
            Operation<Void> original
    ) {
        int ownerSlotId = ClientRuntime.INSTANCE.clients().resolveTickOwnerSlot();

        if (ownerSlotId <= 0) {
            original.call(minecraft);
            return;
        }

        try (InstanceExecution.Scope ignored =
                     InstanceExecution.enterForClient(minecraft, ownerSlotId)) {
            original.call(minecraft);
        }
    }

    @Redirect(method = "renderFrame", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;pick(F)V"))
    private void javaReforged$pickForVisiblePlayers(Minecraft minecraft, float partialTicks) {
        WorldExtractionPass.pickVisibleSlots(minecraft, partialTicks);
    }

    @Inject(method = "renderFrame", at = @At("HEAD"))
    private void javaReforged$beginExperimentalRenderFrame(boolean advanceGameTime, CallbackInfo ci) {
        ExperimentalRenderController.beginFrame();
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void javaReforged$tickSecondarySessions(CallbackInfo ci) {
        ClientRuntime.INSTANCE.clients().tickSecondarySessions((Minecraft)(Object)this);
    }

    @Inject(method = "handleKeybinds", at = @At("HEAD"), cancellable = true)
    private void javaReforged$skipKeybindsUntilSlotGameplayReady(CallbackInfo ci) {
        if (!ReadinessPolicy.currentGameplayReady()) {
            ci.cancel();
        }
    }

    @Inject(method = "handleKeybinds", at = @At("HEAD"))
    private void javaReforged$prepareLegacyConstructionRepeat(CallbackInfo ci) {
        LegacyGameplayModes.prepareConstructionRepeat((Minecraft)(Object)this);
    }

    @Redirect(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MouseHandler;isMouseGrabbed()Z"))
    private boolean javaReforged$usePerClientGameplayCapture(MouseHandler mouseHandler) {
        return ScreenInputHandler.isGameplayCaptured((Minecraft) (Object) this, mouseHandler);
    }

    @Inject(method = "wrapRunnable", at = @At("HEAD"), cancellable = true)
    private void javaReforged$wrapRunnableWithSlot(Runnable runnable, CallbackInfoReturnable<Runnable> cir) {
        cir.setReturnValue(PacketExecutionRouter.wrapScheduled(runnable));
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;tick()V"))
    private void javaReforged$tickHudBySlot(Gui gui) {
        HudPass.tick(gui);
    }

    @Inject(method = "pauseGame", at = @At("HEAD"), cancellable = true)
    private void javaReforged$makePauseScreenLocal(boolean pauseOnly, CallbackInfo ci) {
        SlotScreenManager.openLocalPause((Minecraft)(Object)this, pauseOnly);
        ci.cancel();
    }

    @Inject(method = "isPaused", at = @At("HEAD"), cancellable = true)
    private void javaReforged$neverFreezeForLocalMultiplayer(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(PreJoinWorldLoad.shouldForcePause());
    }

    @Redirect(
        method = {"runTick", "tick"},
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;pause:Z", opcode = Opcodes.GETFIELD)
    )
    private boolean javaReforged$readPauseAsFalseForLocalMultiplayer(Minecraft minecraft) {
        return PreJoinWorldLoad.shouldForcePause();
    }

    @Redirect(
        method = "runTick",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;pause:Z", opcode = Opcodes.PUTFIELD)
    )
    private void javaReforged$writePauseAsFalseForLocalMultiplayer(Minecraft minecraft, boolean pause) {
        this.pause = pause && PreJoinWorldLoad.shouldForcePause();
    }

    @Redirect(
        method = "<init>",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V")
    )
    private void javaReforged$bootstrapPrimaryScreen(Gui gui, Screen screen) {
        SlotScreenManager.bootstrapPrimary((Minecraft) (Object) this, screen);
    }

    @WrapOperation(
        method = "setScreenAndShow",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;renderFrame(Z)V")
    )
    private void javaReforged$skipBlackForcedFrameDuringLocalWorld(
        Minecraft minecraft,
        boolean advanceGameTime,
        Operation<Void> original
    ) {


        boolean localWorldVisible = ClientRuntime.INSTANCE.viewports().presentedCount() > 1
            || ClientRuntime.INSTANCE.clients().all().stream()
                .anyMatch(ReadinessPolicy::worldReady);
        if (!localWorldVisible) {
            original.call(minecraft, advanceGameTime);
        }
    }

    @Inject(method = "runTick", at = @At("TAIL"))
    private void javaReforged$closeCompletedTestJoiningScreen(boolean renderLevel, CallbackInfo ci) {
        Minecraft minecraft = (Minecraft) (Object) this;
        PreJoinWorldLoad.tick(minecraft);
        SlotScreenTransitionHandler.closeCompletedPrimaryJoiningScreen(minecraft);
        SlotScreenTransitionHandler.closeCompletedSecondaryJoiningScreens(minecraft);
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void javaReforged$tickSecondaryScreens(CallbackInfo ci) {
        SlotScreenManager.tickSecondary();
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
        Integer slotId = SlotScope.idOrNull();
        if (slotId != null && slotId != 0) {
            return;
        }
        PreJoinWorldLoad.onMinecraftDisconnect();
        DisconnectHandler.returnToPrimaryOnly(
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

    @WrapOperation(
            method = "startUseItem",
            at = @At(
                    value = "INVOKE",
                    target =
                            "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;" +
                                    "useItemOn(" +
                                    "Lnet/minecraft/client/player/LocalPlayer;" +
                                    "Lnet/minecraft/world/InteractionHand;" +
                                    "Lnet/minecraft/world/phys/BlockHitResult;" +
                                    ")Lnet/minecraft/world/InteractionResult;"
            )
    )
    private InteractionResult javaReforged$trackLegacyCreativePlacement(
            MultiPlayerGameMode gameMode,
            net.minecraft.client.player.LocalPlayer player,
            InteractionHand hand,
            net.minecraft.world.phys.BlockHitResult hitResult,
            Operation<InteractionResult> original
    ) {
        boolean placingBlock = player.getItemInHand(hand).getItem()
                instanceof net.minecraft.world.item.BlockItem;
        InteractionResult result = original.call(gameMode, player, hand, hitResult);
        if (placingBlock && result instanceof InteractionResult.Success) {
            LegacyGameplayModes.recordSuccessfulPlacement(player);
        }
        return result;
    }
}
