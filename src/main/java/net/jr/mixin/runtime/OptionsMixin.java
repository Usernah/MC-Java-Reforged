package net.jr.mixin.runtime;

import net.jr.client.runtime.options.OptionsLifecycleHandler;
import net.jr.client.runtime.options.OptionsStateResolver;
import net.jr.mixin.accessors.OptionsAccessor;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Options.class)
public abstract class OptionsMixin {
    @Inject(method = "getCameraType", at = @At("HEAD"), cancellable = true)
    private void javaReforged$getCameraType(CallbackInfoReturnable<CameraType> callback) {
        callback.setReturnValue(OptionsStateResolver.current().cameraType());
    }

    @Inject(method = "setCameraType", at = @At("HEAD"), cancellable = true)
    private void javaReforged$setCameraType(CameraType cameraType, CallbackInfo callback) {
        OptionsLifecycleHandler.setCameraType((OptionsAccessor) this, cameraType);
        callback.cancel();
    }

    @Inject(method = "fov", at = @At("HEAD"), cancellable = true)
    private void javaReforged$getFov(CallbackInfoReturnable<OptionInstance<Integer>> callback) {
        callback.setReturnValue(OptionsStateResolver.current().fov());
    }

    @Inject(method = "gamma", at = @At("HEAD"), cancellable = true)
    private void javaReforged$getBrightness(CallbackInfoReturnable<OptionInstance<Double>> callback) {
        callback.setReturnValue(OptionsStateResolver.current().brightness());
    }

    @Inject(method = "sensitivity", at = @At("HEAD"), cancellable = true)
    private void javaReforged$getSensitivity(CallbackInfoReturnable<OptionInstance<Double>> callback) {
        callback.setReturnValue(OptionsStateResolver.current().sensitivity());
    }

    @Inject(method = "mouseWheelSensitivity", at = @At("HEAD"), cancellable = true)
    private void javaReforged$getMouseWheelSensitivity(CallbackInfoReturnable<OptionInstance<Double>> callback) {
        callback.setReturnValue(OptionsStateResolver.current().mouseWheelSensitivity());
    }

    @Inject(method = "rawMouseInput", at = @At("HEAD"), cancellable = true)
    private void javaReforged$getRawMouseInput(CallbackInfoReturnable<OptionInstance<Boolean>> callback) {
        callback.setReturnValue(OptionsStateResolver.current().rawMouseInput());
    }

    @Inject(method = "allowCursorChanges", at = @At("HEAD"), cancellable = true)
    private void javaReforged$getAllowCursorChanges(CallbackInfoReturnable<OptionInstance<Boolean>> callback) {
        callback.setReturnValue(OptionsStateResolver.current().allowCursorChanges());
    }

    @Inject(method = "invertMouseX", at = @At("HEAD"), cancellable = true)
    private void javaReforged$getInvertMouseX(CallbackInfoReturnable<OptionInstance<Boolean>> callback) {
        callback.setReturnValue(OptionsStateResolver.current().invertMouseX());
    }

    @Inject(method = "invertMouseY", at = @At("HEAD"), cancellable = true)
    private void javaReforged$getInvertMouseY(CallbackInfoReturnable<OptionInstance<Boolean>> callback) {
        callback.setReturnValue(OptionsStateResolver.current().invertMouseY());
    }

    @Inject(method = "discreteMouseScroll", at = @At("HEAD"), cancellable = true)
    private void javaReforged$getDiscreteMouseScroll(CallbackInfoReturnable<OptionInstance<Boolean>> callback) {
        callback.setReturnValue(OptionsStateResolver.current().discreteMouseScroll());
    }

    @Inject(method = "toggleCrouch", at = @At("HEAD"), cancellable = true)
    private void javaReforged$getToggleCrouch(CallbackInfoReturnable<OptionInstance<Boolean>> callback) {
        callback.setReturnValue(OptionsStateResolver.current().toggleCrouch());
    }

    @Inject(method = "toggleSprint", at = @At("HEAD"), cancellable = true)
    private void javaReforged$getToggleSprint(CallbackInfoReturnable<OptionInstance<Boolean>> callback) {
        callback.setReturnValue(OptionsStateResolver.current().toggleSprint());
    }

    @Inject(method = "toggleAttack", at = @At("HEAD"), cancellable = true)
    private void javaReforged$getToggleAttack(CallbackInfoReturnable<OptionInstance<Boolean>> callback) {
        callback.setReturnValue(OptionsStateResolver.current().toggleAttack());
    }

    @Inject(method = "toggleUse", at = @At("HEAD"), cancellable = true)
    private void javaReforged$getToggleUse(CallbackInfoReturnable<OptionInstance<Boolean>> callback) {
        callback.setReturnValue(OptionsStateResolver.current().toggleUse());
    }

    @Inject(method = "sprintWindow", at = @At("HEAD"), cancellable = true)
    private void javaReforged$getSprintWindow(CallbackInfoReturnable<OptionInstance<Integer>> callback) {
        callback.setReturnValue(OptionsStateResolver.current().sprintWindow());
    }

    @Inject(method = "autoJump", at = @At("HEAD"), cancellable = true)
    private void javaReforged$getAutoJump(CallbackInfoReturnable<OptionInstance<Boolean>> callback) {
        callback.setReturnValue(OptionsStateResolver.current().autoJump());
    }

    @Inject(method = "operatorItemsTab", at = @At("HEAD"), cancellable = true)
    private void javaReforged$getOperatorItemsTab(CallbackInfoReturnable<OptionInstance<Boolean>> callback) {
        callback.setReturnValue(OptionsStateResolver.current().operatorItemsTab());
    }

    @Inject(method = "load", at = @At("TAIL"))
    private void javaReforged$initializeLocalOptions(CallbackInfo callback) {
        OptionsLifecycleHandler.initialize((OptionsAccessor) this, Minecraft.getInstance());
    }

    @Inject(method = "save", at = @At("HEAD"))
    private void javaReforged$preparePrimaryOptionsForSave(CallbackInfo callback) {
        OptionsLifecycleHandler.preparePrimaryForSave((OptionsAccessor) this);
    }
}
