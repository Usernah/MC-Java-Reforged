package net.jr.client.runtime.state;

import com.mojang.serialization.Codec;
import java.util.Locale;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.viewport.GuiScaleCalculator;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;

public final class OptionsState {
    private final int slotId;

    private final OptionInstance<Integer> guiScale;
    private final OptionInstance<Integer> fov;
    private final OptionInstance<Double> brightness;
    private final OptionInstance<Double> sensitivity;
    private final OptionInstance<Double> mouseWheelSensitivity;
    private final OptionInstance<Boolean> rawMouseInput;
    private final OptionInstance<Boolean> allowCursorChanges;
    private final OptionInstance<Boolean> invertMouseX;
    private final OptionInstance<Boolean> invertMouseY;
    private final OptionInstance<Boolean> discreteMouseScroll;
    private final OptionInstance<Boolean> toggleCrouch;
    private final OptionInstance<Boolean> toggleSprint;
    private final OptionInstance<Boolean> toggleAttack;
    private final OptionInstance<Boolean> toggleUse;
    private final OptionInstance<Integer> sprintWindow;
    private final OptionInstance<Boolean> autoJump;
    private final OptionInstance<Boolean> operatorItemsTab;

    private CameraType cameraType = CameraType.FIRST_PERSON;

    public OptionsState(int slotId) {
        this.slotId = slotId;

        this.guiScale = new OptionInstance<>(
                "options.guiScale",
                OptionInstance.noTooltip(),
                (caption, value) -> value == 0
                        ? Component.translatable("options.guiScale.auto")
                        : Component.literal(Integer.toString(value)),
                new OptionInstance.ClampingLazyMaxIntRange(
                        0,
                        this::maximumGuiScale,
                        Integer.MAX_VALUE - 1
                ),
                0,
                value -> {
                    Minecraft minecraft = Minecraft.getInstance();
                    if (minecraft.isRunning()) {
                        ClientRuntime.INSTANCE
                                .viewportResize()
                                .refreshViewportOptions(minecraft);
                    }
                    this.save();
                }
        );

        this.fov = new OptionInstance<>(
                "options.fov",
                OptionInstance.noTooltip(),
                (caption, value) -> switch (value) {
                    case 70 -> Options.genericValueLabel(
                            caption,
                            Component.translatable("options.fov.min")
                    );
                    case 110 -> Options.genericValueLabel(
                            caption,
                            Component.translatable("options.fov.max")
                    );
                    default -> Options.genericValueLabel(caption, value);
                },
                new OptionInstance.IntRange(30, 110),
                70,
                value -> {
                    this.save();
                }
        );

        this.brightness = new OptionInstance<>(
                "options.gamma",
                OptionInstance.noTooltip(),
                (caption, value) -> Options.genericValueLabel(
                        caption,
                        Component.literal(Math.round(value * 100.0D) + "%")
                ),
                OptionInstance.UnitDouble.INSTANCE,
                0.5D,
                value -> this.save()
        );

        this.sensitivity = new OptionInstance<>(
                "options.sensitivity",
                OptionInstance.noTooltip(),
                (caption, value) -> {
                    if (value == 0.0D) {
                        return Options.genericValueLabel(
                                caption,
                                Component.translatable("options.sensitivity.min")
                        );
                    }
                    if (value == 1.0D) {
                        return Options.genericValueLabel(
                                caption,
                                Component.translatable("options.sensitivity.max")
                        );
                    }
                    return Component.translatable(
                            "options.percent_value",
                            caption,
                            (int) (value * 200.0D)
                    );
                },
                OptionInstance.UnitDouble.INSTANCE,
                0.5D,
                value -> this.save()
        );

        this.mouseWheelSensitivity = new OptionInstance<>(
                "options.mouseWheelSensitivity",
                OptionInstance.noTooltip(),
                (caption, value) -> Options.genericValueLabel(
                        caption,
                        Component.literal(String.format(Locale.ROOT, "%.2f", value))
                ),
                new OptionInstance.IntRange(-200, 100).xmap(
                        OptionsState::logMouse,
                        OptionsState::unlogMouse,
                        false
                ),
                Codec.doubleRange(logMouse(-200), logMouse(100)),
                logMouse(0),
                value -> this.save()
        );

        this.rawMouseInput = OptionInstance.createBoolean(
                "options.rawMouseInput",
                true,
                this::applyRawMouseInput
        );
        this.allowCursorChanges = OptionInstance.createBoolean(
                "options.allowCursorChanges",
                true,
                this::applyAllowCursorChanges
        );
        this.invertMouseX = OptionInstance.createBoolean(
                "options.invertMouseX",
                false,
                value -> this.save()
        );
        this.invertMouseY = OptionInstance.createBoolean(
                "options.invertMouseY",
                false,
                value -> this.save()
        );
        this.discreteMouseScroll = OptionInstance.createBoolean(
                "options.discrete_mouse_scroll",
                false,
                value -> this.save()
        );

        this.toggleCrouch = this.toggleOption(
                "key.sneak",
                false
        );
        this.toggleSprint = this.toggleOption(
                "key.sprint",
                false
        );
        this.toggleAttack = this.toggleOption(
                "key.attack",
                false
        );
        this.toggleUse = this.toggleOption(
                "key.use",
                false
        );
        this.sprintWindow = new OptionInstance<>(
                "options.sprintWindow",
                OptionInstance.noTooltip(),
                (caption, value) -> value == 0
                        ? Options.genericValueLabel(
                                caption,
                                Component.translatable("options.off")
                        )
                        : Options.genericValueLabel(
                                caption,
                                Component.translatable("options.value", value)
                        ),
                new OptionInstance.IntRange(0, 10),
                7,
                value -> this.save()
        );
        this.autoJump = OptionInstance.createBoolean(
                "options.autoJump",
                false,
                value -> this.save()
        );
        this.operatorItemsTab = OptionInstance.createBoolean(
                "options.operatorItemsTab",
                false,
                value -> this.save()
        );
    }

    public OptionInstance<Integer> guiScale() {
        return this.guiScale;
    }

    public int requestedGuiScale() {
        return this.guiScale.get();
    }

    public CameraType cameraType() {
        return this.cameraType;
    }

    public void setCameraType(CameraType cameraType) {
        this.cameraType = cameraType;
        this.save();
    }

    public OptionInstance<Integer> fov() {
        return this.fov;
    }

    public OptionInstance<Double> brightness() {
        return this.brightness;
    }

    public OptionInstance<Double> sensitivity() {
        return this.sensitivity;
    }

    public OptionInstance<Double> mouseWheelSensitivity() {
        return this.mouseWheelSensitivity;
    }

    public OptionInstance<Boolean> rawMouseInput() {
        return this.rawMouseInput;
    }

    public OptionInstance<Boolean> allowCursorChanges() {
        return this.allowCursorChanges;
    }

    public OptionInstance<Boolean> invertMouseX() {
        return this.invertMouseX;
    }

    public OptionInstance<Boolean> invertMouseY() {
        return this.invertMouseY;
    }

    public OptionInstance<Boolean> discreteMouseScroll() {
        return this.discreteMouseScroll;
    }

    public OptionInstance<Boolean> toggleCrouch() {
        return this.toggleCrouch;
    }

    public OptionInstance<Boolean> toggleSprint() {
        return this.toggleSprint;
    }

    public OptionInstance<Boolean> toggleAttack() {
        return this.toggleAttack;
    }

    public OptionInstance<Boolean> toggleUse() {
        return this.toggleUse;
    }

    public OptionInstance<Integer> sprintWindow() {
        return this.sprintWindow;
    }

    public OptionInstance<Boolean> autoJump() {
        return this.autoJump;
    }

    public OptionInstance<Boolean> operatorItemsTab() {
        return this.operatorItemsTab;
    }

    public void setGuiScale(int guiScale) {
        this.guiScale.set(guiScale);
    }

    public void setFov(int fov) {
        this.fov.set(fov);
    }

    public void setBrightness(double brightness) {
        this.brightness.set(brightness);
    }

    public void setSensitivity(double sensitivity) {
        this.sensitivity.set(sensitivity);
    }

    public void setMouseWheelSensitivity(double mouseWheelSensitivity) {
        this.mouseWheelSensitivity.set(mouseWheelSensitivity);
    }

    public void setRawMouseInput(boolean rawMouseInput) {
        this.rawMouseInput.set(rawMouseInput);
    }

    public void setAllowCursorChanges(boolean allowCursorChanges) {
        this.allowCursorChanges.set(allowCursorChanges);
    }

    public void setInvertMouseX(boolean invertMouseX) {
        this.invertMouseX.set(invertMouseX);
    }

    public void setInvertMouseY(boolean invertMouseY) {
        this.invertMouseY.set(invertMouseY);
    }

    public void setDiscreteMouseScroll(boolean discreteMouseScroll) {
        this.discreteMouseScroll.set(discreteMouseScroll);
    }

    public void setToggleCrouch(boolean toggleCrouch) {
        this.toggleCrouch.set(toggleCrouch);
    }

    public void setToggleSprint(boolean toggleSprint) {
        this.toggleSprint.set(toggleSprint);
    }

    public void setToggleAttack(boolean toggleAttack) {
        this.toggleAttack.set(toggleAttack);
    }

    public void setToggleUse(boolean toggleUse) {
        this.toggleUse.set(toggleUse);
    }

    public void setSprintWindow(int sprintWindow) {
        this.sprintWindow.set(sprintWindow);
    }

    public void setAutoJump(boolean autoJump) {
        this.autoJump.set(autoJump);
    }

    public void setOperatorItemsTab(boolean operatorItemsTab) {
        this.operatorItemsTab.set(operatorItemsTab);
    }

    public void initialize(InitialValues values) {
        this.cameraType = values.cameraType();
        this.fov.set(values.fov());
        this.brightness.set(values.brightness());
        this.sensitivity.set(values.sensitivity());
        this.mouseWheelSensitivity.set(values.mouseWheelSensitivity());
        this.rawMouseInput.set(values.rawMouseInput());
        this.allowCursorChanges.set(values.allowCursorChanges());
        this.invertMouseX.set(values.invertMouseX());
        this.invertMouseY.set(values.invertMouseY());
        this.discreteMouseScroll.set(values.discreteMouseScroll());
        this.toggleCrouch.set(values.toggleCrouch());
        this.toggleSprint.set(values.toggleSprint());
        this.toggleAttack.set(values.toggleAttack());
        this.toggleUse.set(values.toggleUse());
        this.sprintWindow.set(values.sprintWindow());
        this.autoJump.set(values.autoJump());
        this.operatorItemsTab.set(values.operatorItemsTab());
    }

    public record InitialValues(
            CameraType cameraType,
            int fov,
            double brightness,
            double sensitivity,
            double mouseWheelSensitivity,
            boolean rawMouseInput,
            boolean allowCursorChanges,
            boolean invertMouseX,
            boolean invertMouseY,
            boolean discreteMouseScroll,
            boolean toggleCrouch,
            boolean toggleSprint,
            boolean toggleAttack,
            boolean toggleUse,
            int sprintWindow,
            boolean autoJump,
            boolean operatorItemsTab
    ) {
    }

    private void save() {
        ClientRuntime.INSTANCE.playerOptions().save();
    }

    private void applyRawMouseInput(boolean enabled) {
        if (this.slotId == 0) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.getWindow() != null) {
                minecraft.getWindow().updateRawMouseInput(enabled);
            }
        }
        this.save();
    }

    private void applyAllowCursorChanges(boolean enabled) {
        if (this.slotId == 0) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.getWindow() != null) {
                minecraft.getWindow().setAllowCursorChanges(enabled);
            }
        }
        this.save();
    }

    private OptionInstance<Boolean> toggleOption(String key, boolean defaultValue) {
        return new OptionInstance<>(
                key,
                OptionInstance.noTooltip(),
                (caption, value) -> Component.translatable(
                        value ? "options.key.toggle" : "options.key.hold"
                ),
                OptionInstance.BOOLEAN_VALUES,
                defaultValue,
                value -> this.save()
        );
    }

    private static double logMouse(int value) {
        return Math.pow(10.0D, value / 100.0D);
    }

    private static int unlogMouse(double value) {
        return (int) Math.floor(Math.log10(value) * 100.0D);
    }

    private int maximumGuiScale() {
        Minecraft minecraft = Minecraft.getInstance();

        if (!minecraft.isRunning()) {
            return Integer.MAX_VALUE - 1;
        }

        return GuiScaleCalculator.maximumForSlot(
                this.slotId,
                minecraft.isEnforceUnicode()
        );
    }
}
