package net.jr.client.runtime.options;

import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.slot.LocalClientSlot;
import net.jr.client.runtime.state.OptionsState;
import net.jr.mixin.accessors.OptionsAccessor;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;

public final class OptionsLifecycleHandler {
    private OptionsLifecycleHandler() {
    }

    public static void initialize(OptionsAccessor options, Minecraft minecraft) {
        OptionsState.InitialValues initialValues = new OptionsState.InitialValues(
                options.javaReforged$getCameraType(),
                options.javaReforged$getFov().get(),
                options.javaReforged$getGamma().get(),
                options.javaReforged$getSensitivity().get(),
                options.javaReforged$getMouseWheelSensitivity().get(),
                options.javaReforged$getRawMouseInput().get(),
                options.javaReforged$getAllowCursorChanges().get(),
                options.javaReforged$getInvertXMouse().get(),
                options.javaReforged$getInvertYMouse().get(),
                options.javaReforged$getDiscreteMouseScroll().get(),
                options.javaReforged$getToggleCrouch().get(),
                options.javaReforged$getToggleSprint().get(),
                options.javaReforged$getToggleAttack().get(),
                options.javaReforged$getToggleUse().get(),
                options.javaReforged$getSprintWindow().get(),
                options.javaReforged$getAutoJump().get(),
                options.javaReforged$getOperatorItemsTab().get()
        );

        for (LocalClientSlot slot : ClientRuntime.INSTANCE.slots().all()) {
            slot.optionsState().initialize(initialValues);
        }
        ClientRuntime.INSTANCE.playerOptions().ensureLoaded(minecraft);
    }

    public static void preparePrimaryForSave(OptionsAccessor options) {
        OptionsState primary = ClientRuntime.INSTANCE.slots().primary().optionsState();
        options.javaReforged$setCameraType(primary.cameraType());
        options.javaReforged$getFov().set(primary.fov().get());
        options.javaReforged$getGamma().set(primary.brightness().get());
        options.javaReforged$getSensitivity().set(primary.sensitivity().get());
        options.javaReforged$getMouseWheelSensitivity().set(primary.mouseWheelSensitivity().get());
        options.javaReforged$getRawMouseInput().set(primary.rawMouseInput().get());
        options.javaReforged$getAllowCursorChanges().set(primary.allowCursorChanges().get());
        options.javaReforged$getInvertXMouse().set(primary.invertMouseX().get());
        options.javaReforged$getInvertYMouse().set(primary.invertMouseY().get());
        options.javaReforged$getDiscreteMouseScroll().set(primary.discreteMouseScroll().get());
        options.javaReforged$getToggleCrouch().set(primary.toggleCrouch().get());
        options.javaReforged$getToggleSprint().set(primary.toggleSprint().get());
        options.javaReforged$getToggleAttack().set(primary.toggleAttack().get());
        options.javaReforged$getToggleUse().set(primary.toggleUse().get());
        options.javaReforged$getSprintWindow().set(primary.sprintWindow().get());
        options.javaReforged$getAutoJump().set(primary.autoJump().get());
        options.javaReforged$getOperatorItemsTab().set(primary.operatorItemsTab().get());
    }

    public static void setCameraType(OptionsAccessor options, CameraType cameraType) {
        OptionsState state = OptionsStateResolver.current();
        state.setCameraType(cameraType);
        if (state == ClientRuntime.INSTANCE.slots().primary().optionsState()) {
            options.javaReforged$setCameraType(cameraType);
        }
    }
}
