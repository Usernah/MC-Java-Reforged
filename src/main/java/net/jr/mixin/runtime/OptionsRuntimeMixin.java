package net.jr.mixin.runtime;

import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.context.SlotScope;
import net.jr.client.runtime.slot.LocalClientSlot;
import net.jr.client.runtime.state.OptionsState;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Options.class)
public abstract class OptionsRuntimeMixin {
    @Shadow
    private CameraType cameraType;

    @Shadow
    @Final
    private OptionInstance<Integer> fov;

    @Shadow
    @Final
    private OptionInstance<Double> gamma;

    @Shadow
    @Final
    private OptionInstance<Double> sensitivity;

    @Shadow
    @Final
    private OptionInstance<Double> mouseWheelSensitivity;

    @Shadow
    @Final
    private OptionInstance<Boolean> rawMouseInput;

    @Shadow
    @Final
    private OptionInstance<Boolean> allowCursorChanges;

    @Shadow
    @Final
    private OptionInstance<Boolean> invertXMouse;

    @Shadow
    @Final
    private OptionInstance<Boolean> invertYMouse;

    @Shadow
    @Final
    private OptionInstance<Boolean> discreteMouseScroll;

    @Shadow
    @Final
    private OptionInstance<Boolean> toggleCrouch;

    @Shadow
    @Final
    private OptionInstance<Boolean> toggleSprint;

    @Shadow
    @Final
    private OptionInstance<Boolean> toggleAttack;

    @Shadow
    @Final
    private OptionInstance<Boolean> toggleUse;

    @Shadow
    @Final
    private OptionInstance<Integer> sprintWindow;

    @Shadow
    @Final
    private OptionInstance<Boolean> autoJump;

    @Shadow
    @Final
    private OptionInstance<Boolean> operatorItemsTab;

    @Inject(
            method = "getCameraType",
            at = @At("HEAD"),
            cancellable = true
    )
    private void splitTest$getSlotCameraType(
            CallbackInfoReturnable<CameraType> cir
    ) {
        cir.setReturnValue(
                this.splitTest$optionsState().cameraType()
        );
    }

    @Inject(
            method = "setCameraType",
            at = @At("HEAD"),
            cancellable = true
    )
    private void splitTest$setSlotCameraType(
            CameraType cameraType,
            CallbackInfo ci
    ) {
        int slotId = this.splitTest$slotId();

        ClientRuntime.INSTANCE
                .slots()
                .slot(slotId)
                .optionsState()
                .setCameraType(cameraType);

        if (slotId == 0) {
            this.cameraType = cameraType;
        }

        ci.cancel();
    }

    @Inject(
            method = "fov",
            at = @At("HEAD"),
            cancellable = true
    )
    private void splitTest$getSlotFov(
            CallbackInfoReturnable<OptionInstance<Integer>> cir
    ) {
        cir.setReturnValue(
                this.splitTest$optionsState().fov()
        );
    }

    @Inject(
            method = "gamma",
            at = @At("HEAD"),
            cancellable = true
    )
    private void splitTest$getSlotBrightness(
            CallbackInfoReturnable<OptionInstance<Double>> cir
    ) {
        cir.setReturnValue(
                this.splitTest$optionsState().brightness()
        );
    }

    @Inject(
            method = "sensitivity",
            at = @At("HEAD"),
            cancellable = true
    )
    private void splitTest$getSlotSensitivity(
            CallbackInfoReturnable<OptionInstance<Double>> cir
    ) {
        cir.setReturnValue(
                this.splitTest$optionsState().sensitivity()
        );
    }

    @Inject(
            method = "mouseWheelSensitivity",
            at = @At("HEAD"),
            cancellable = true
    )
    private void splitTest$getSlotMouseWheelSensitivity(
            CallbackInfoReturnable<OptionInstance<Double>> cir
    ) {
        cir.setReturnValue(
                this.splitTest$optionsState().mouseWheelSensitivity()
        );
    }

    @Inject(
            method = "rawMouseInput",
            at = @At("HEAD"),
            cancellable = true
    )
    private void splitTest$getSlotRawMouseInput(
            CallbackInfoReturnable<OptionInstance<Boolean>> cir
    ) {
        cir.setReturnValue(
                this.splitTest$optionsState().rawMouseInput()
        );
    }

    @Inject(
            method = "allowCursorChanges",
            at = @At("HEAD"),
            cancellable = true
    )
    private void splitTest$getSlotAllowCursorChanges(
            CallbackInfoReturnable<OptionInstance<Boolean>> cir
    ) {
        cir.setReturnValue(
                this.splitTest$optionsState().allowCursorChanges()
        );
    }

    @Inject(
            method = "invertMouseX",
            at = @At("HEAD"),
            cancellable = true
    )
    private void splitTest$getSlotInvertMouseX(
            CallbackInfoReturnable<OptionInstance<Boolean>> cir
    ) {
        cir.setReturnValue(
                this.splitTest$optionsState().invertMouseX()
        );
    }

    @Inject(
            method = "invertMouseY",
            at = @At("HEAD"),
            cancellable = true
    )
    private void splitTest$getSlotInvertMouseY(
            CallbackInfoReturnable<OptionInstance<Boolean>> cir
    ) {
        cir.setReturnValue(
                this.splitTest$optionsState().invertMouseY()
        );
    }

    @Inject(
            method = "discreteMouseScroll",
            at = @At("HEAD"),
            cancellable = true
    )
    private void splitTest$getSlotDiscreteMouseScroll(
            CallbackInfoReturnable<OptionInstance<Boolean>> cir
    ) {
        cir.setReturnValue(
                this.splitTest$optionsState().discreteMouseScroll()
        );
    }

    @Inject(
            method = "toggleCrouch",
            at = @At("HEAD"),
            cancellable = true
    )
    private void splitTest$getSlotToggleCrouch(
            CallbackInfoReturnable<OptionInstance<Boolean>> cir
    ) {
        cir.setReturnValue(
                this.splitTest$optionsState().toggleCrouch()
        );
    }

    @Inject(
            method = "toggleSprint",
            at = @At("HEAD"),
            cancellable = true
    )
    private void splitTest$getSlotToggleSprint(
            CallbackInfoReturnable<OptionInstance<Boolean>> cir
    ) {
        cir.setReturnValue(
                this.splitTest$optionsState().toggleSprint()
        );
    }

    @Inject(
            method = "toggleAttack",
            at = @At("HEAD"),
            cancellable = true
    )
    private void splitTest$getSlotToggleAttack(
            CallbackInfoReturnable<OptionInstance<Boolean>> cir
    ) {
        cir.setReturnValue(
                this.splitTest$optionsState().toggleAttack()
        );
    }

    @Inject(
            method = "toggleUse",
            at = @At("HEAD"),
            cancellable = true
    )
    private void splitTest$getSlotToggleUse(
            CallbackInfoReturnable<OptionInstance<Boolean>> cir
    ) {
        cir.setReturnValue(
                this.splitTest$optionsState().toggleUse()
        );
    }

    @Inject(
            method = "sprintWindow",
            at = @At("HEAD"),
            cancellable = true
    )
    private void splitTest$getSlotSprintWindow(
            CallbackInfoReturnable<OptionInstance<Integer>> cir
    ) {
        cir.setReturnValue(
                this.splitTest$optionsState().sprintWindow()
        );
    }

    @Inject(
            method = "autoJump",
            at = @At("HEAD"),
            cancellable = true
    )
    private void splitTest$getSlotAutoJump(
            CallbackInfoReturnable<OptionInstance<Boolean>> cir
    ) {
        cir.setReturnValue(
                this.splitTest$optionsState().autoJump()
        );
    }

    @Inject(
            method = "operatorItemsTab",
            at = @At("HEAD"),
            cancellable = true
    )
    private void splitTest$getSlotOperatorItemsTab(
            CallbackInfoReturnable<OptionInstance<Boolean>> cir
    ) {
        cir.setReturnValue(
                this.splitTest$optionsState().operatorItemsTab()
        );
    }

    @Inject(
            method = "load",
            at = @At("TAIL")
    )
    private void splitTest$initializeLocalVisualOptions(
            CallbackInfo ci
    ) {
        OptionsState.InitialValues initialValues = new OptionsState.InitialValues(
                this.cameraType,
                this.fov.get(),
                this.gamma.get(),
                this.sensitivity.get(),
                this.mouseWheelSensitivity.get(),
                this.rawMouseInput.get(),
                this.allowCursorChanges.get(),
                this.invertXMouse.get(),
                this.invertYMouse.get(),
                this.discreteMouseScroll.get(),
                this.toggleCrouch.get(),
                this.toggleSprint.get(),
                this.toggleAttack.get(),
                this.toggleUse.get(),
                this.sprintWindow.get(),
                this.autoJump.get(),
                this.operatorItemsTab.get()
        );

        for (LocalClientSlot slot : ClientRuntime.INSTANCE.slots().all()) {
            slot.optionsState().initialize(initialValues);
        }

        ClientRuntime.INSTANCE.playerOptions().ensureLoaded(Minecraft.getInstance());
    }

    @Inject(
            method = "save",
            at = @At("HEAD")
    )
    private void splitTest$syncPrimaryVisualOptionsBeforeSave(
            CallbackInfo ci
    ) {
        OptionsState primary =
                ClientRuntime.INSTANCE.slots().primary().optionsState();

        this.cameraType = primary.cameraType();
        this.fov.set(primary.fov().get());
        this.gamma.set(primary.brightness().get());
        this.sensitivity.set(primary.sensitivity().get());
        this.mouseWheelSensitivity.set(primary.mouseWheelSensitivity().get());
        this.rawMouseInput.set(primary.rawMouseInput().get());
        this.allowCursorChanges.set(primary.allowCursorChanges().get());
        this.invertXMouse.set(primary.invertMouseX().get());
        this.invertYMouse.set(primary.invertMouseY().get());
        this.discreteMouseScroll.set(primary.discreteMouseScroll().get());
        this.toggleCrouch.set(primary.toggleCrouch().get());
        this.toggleSprint.set(primary.toggleSprint().get());
        this.toggleAttack.set(primary.toggleAttack().get());
        this.toggleUse.set(primary.toggleUse().get());
        this.sprintWindow.set(primary.sprintWindow().get());
        this.autoJump.set(primary.autoJump().get());
        this.operatorItemsTab.set(primary.operatorItemsTab().get());
    }

    @Unique
    private OptionsState splitTest$optionsState() {
        return ClientRuntime.INSTANCE
                .slots()
                .slot(this.splitTest$slotId())
                .optionsState();
    }

    @Unique
    private int splitTest$slotId() {
        Integer slotId = SlotScope.idOrNull();
        return slotId != null ? slotId : 0;
    }
}
