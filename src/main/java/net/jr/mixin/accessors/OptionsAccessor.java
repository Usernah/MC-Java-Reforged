package net.jr.mixin.accessors;

import net.minecraft.client.CameraType;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.world.entity.HumanoidArm;

@Mixin(Options.class)
public interface OptionsAccessor {
    @Accessor("cameraType")
    CameraType javaReforged$getCameraType();

    @Accessor("cameraType")
    void javaReforged$setCameraType(CameraType cameraType);

    @Accessor("fov")
    OptionInstance<Integer> javaReforged$getFov();

    @Accessor("gamma")
    OptionInstance<Double> javaReforged$getGamma();

    @Accessor("sensitivity")
    OptionInstance<Double> javaReforged$getSensitivity();

    @Accessor("mouseWheelSensitivity")
    OptionInstance<Double> javaReforged$getMouseWheelSensitivity();

    @Accessor("rawMouseInput")
    OptionInstance<Boolean> javaReforged$getRawMouseInput();

    @Accessor("allowCursorChanges")
    OptionInstance<Boolean> javaReforged$getAllowCursorChanges();

    @Accessor("invertXMouse")
    OptionInstance<Boolean> javaReforged$getInvertXMouse();

    @Accessor("invertYMouse")
    OptionInstance<Boolean> javaReforged$getInvertYMouse();

    @Accessor("discreteMouseScroll")
    OptionInstance<Boolean> javaReforged$getDiscreteMouseScroll();

    @Accessor("toggleCrouch")
    OptionInstance<Boolean> javaReforged$getToggleCrouch();

    @Accessor("toggleSprint")
    OptionInstance<Boolean> javaReforged$getToggleSprint();

    @Accessor("toggleAttack")
    OptionInstance<Boolean> javaReforged$getToggleAttack();

    @Accessor("toggleUse")
    OptionInstance<Boolean> javaReforged$getToggleUse();

    @Accessor("sprintWindow")
    OptionInstance<Integer> javaReforged$getSprintWindow();

    @Accessor("autoJump")
    OptionInstance<Boolean> javaReforged$getAutoJump();

    @Accessor("operatorItemsTab")
    OptionInstance<Boolean> javaReforged$getOperatorItemsTab();

    @Accessor("attackIndicator")
    OptionInstance<AttackIndicatorStatus> javaReforged$getAttackIndicator();

    @Accessor("mainHand")
    OptionInstance<HumanoidArm> javaReforged$getMainHand();
}
