package net.jr.client.runtime.state;

import com.mojang.serialization.Codec;
import java.util.Locale;
import java.util.Arrays;
import net.minecraft.client.AttackIndicatorStatus;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.context.SlotExecution;
import net.jr.client.runtime.viewport.GuiScaleCalculator;
import net.jr.client.runtime.viewport.HudScaleCalculator;
import net.jr.client.ui.presentation.UiSizeMode;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.HumanoidArm;

public final class OptionsState {
    private static final double DEFAULT_CONTROLLER_CURSOR_SENSITIVITY = 8.0D;
    private static final double DEFAULT_CONTROLLER_CAMERA_SENSITIVITY = 24.0D;
    private static final double DEFAULT_CONTROLLER_SPYGLASS_SENSITIVITY = 6.0D;
    private static final int DEFAULT_BOTTOM_HUD_SCALE = 1;

    private final int slotId;

    private final OptionInstance<Integer> guiScale;
    private final OptionInstance<UiSizeMode> interfaceMode;
    private final OptionInstance<Integer> bottomHudScale;
    private final OptionInstance<Integer> splitBottomHudScale;
    private final OptionInstance<Integer> fov;
    private final OptionInstance<Double> brightness;
    private final OptionInstance<Double> legacyGamma;
    private final OptionInstance<Double> hudTransparency;
    private final OptionInstance<Boolean> showPlayerModelHud;
    private final OptionInstance<Boolean> showPlayerNameHud;
    private final OptionInstance<Boolean> showControlHints;
    private final OptionInstance<Boolean> showLocatorBar;
    private final OptionInstance<Boolean> showWorldPlayerNames;
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
    private final OptionInstance<AttackIndicatorStatus> attackIndicator;
    private final OptionInstance<HumanoidArm> mainHand;
    private final OptionInstance<CraftingMode> craftingMode;
    private final OptionInstance<LegacyBehaviorMode> flightMode;
    private final OptionInstance<LegacyBehaviorMode> constructionMode;
    private final OptionInstance<Double> controllerCursorSensitivity;
    private final OptionInstance<Double> controllerCameraSensitivity;
    private final OptionInstance<Double> controllerSpyglassSensitivity;
    private final OptionInstance<Boolean> invertControllerMovementY;
    private final OptionInstance<Boolean> invertControllerCameraX;
    private final OptionInstance<Boolean> invertControllerCameraY;

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

        this.interfaceMode = new OptionInstance<>(
                "options.java_reforged.interface_mode",
                OptionInstance.cachedConstantTooltip(
                        Component.translatable("options.java_reforged.interface_mode.description")
                ),
                (caption, value) -> value.caption(),
                new OptionInstance.Enum<>(
                        Arrays.asList(UiSizeMode.values()),
                        Codec.STRING.xmap(UiSizeMode::byName, UiSizeMode::serializedName)
                ),
                UiSizeMode.NORMAL,
                this::applyInterfaceMode
        );

        this.bottomHudScale = new OptionInstance<>(
                "options.java_reforged.hud_scale",
                OptionInstance.cachedConstantTooltip(
                        Component.translatable("options.java_reforged.hud_scale.description")
                ),
                (caption, value) -> Options.genericValueLabel(
                    caption,
                    Component.literal(Integer.toString(value))
                ),
                new OptionInstance.IntRange(
                    HudScaleCalculator.MINIMUM_LEVEL,
                    HudScaleCalculator.MAXIMUM_LEVEL
                ),
                DEFAULT_BOTTOM_HUD_SCALE,
                value -> this.save()
        );

        this.splitBottomHudScale = new OptionInstance<>(
                "options.java_reforged.split_hud_scale",
                OptionInstance.cachedConstantTooltip(
                        Component.translatable("options.java_reforged.split_hud_scale.description")
                ),
                (caption, value) -> Options.genericValueLabel(
                    caption,
                    Component.literal(Integer.toString(value))
                ),
                new OptionInstance.IntRange(
                    HudScaleCalculator.MINIMUM_LEVEL,
                    HudScaleCalculator.MAXIMUM_LEVEL
                ),
                DEFAULT_BOTTOM_HUD_SCALE,
                value -> this.save()
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

        this.legacyGamma = new OptionInstance<>(
                "options.java_reforged.legacy_gamma",
                OptionInstance.noTooltip(),
                (caption, value) -> Options.genericValueLabel(
                        caption,
                        Component.literal(Math.round(value * 100.0D) + "%")
                ),
                OptionInstance.UnitDouble.INSTANCE,
                0.5D,
                value -> this.save()
        );

        this.hudTransparency = new OptionInstance<>(
                "options.java_reforged.hud_transparency",
                OptionInstance.noTooltip(),
                (caption, value) -> Options.genericValueLabel(
                        caption,
                        Component.literal(Math.round(value * 100.0D) + "%")
                ),
                OptionInstance.UnitDouble.INSTANCE,
                0.7D,
                value -> this.save()
        );

        this.showPlayerModelHud = this.visibilityOption("options.java_reforged.player_model_hud", true);
        this.showPlayerNameHud = this.visibilityOption("options.java_reforged.player_name_hud", true);
        this.showControlHints = this.visibilityOption("options.java_reforged.control_hints", true);
        this.showLocatorBar = this.visibilityOption("options.java_reforged.locator_bar", true);
        this.showWorldPlayerNames = this.visibilityOption("options.java_reforged.world_player_names", true);

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
        this.attackIndicator = new OptionInstance<>(
                "options.attackIndicator",
                OptionInstance.noTooltip(),
                (caption, value) -> value.caption(),
                new OptionInstance.Enum<>(Arrays.asList(AttackIndicatorStatus.values()), AttackIndicatorStatus.LEGACY_CODEC),
                AttackIndicatorStatus.CROSSHAIR,
                value -> this.save()
        );
        this.mainHand = new OptionInstance<>(
                "options.mainHand",
                OptionInstance.noTooltip(),
                (caption, value) -> value.caption(),
                new OptionInstance.Enum<>(Arrays.asList(HumanoidArm.values()), HumanoidArm.CODEC),
                HumanoidArm.RIGHT,
                value -> {
                    this.save();
                    var client = ClientRuntime.INSTANCE.clients().clientOrNull(this.slotId);
                    if (client != null && client.player() != null) {
                        SlotExecution.runForSlot(this.slotId, Minecraft.getInstance().options::broadcastOptions);
                    }
                }
        );
        this.craftingMode = new OptionInstance<>(
                "options.java_reforged.crafting_mode",
                OptionInstance.noTooltip(),
                (caption, value) -> value.caption(),
                new OptionInstance.Enum<>(Arrays.asList(CraftingMode.values()), Codec.STRING.xmap(CraftingMode::byName, CraftingMode::serializedName)),
                CraftingMode.LEGACY,
                value -> this.save()
        );
        this.flightMode = this.legacyBehaviorMode("options.java_reforged.flight_mode");
        this.constructionMode = this.legacyBehaviorMode("options.java_reforged.construction_mode");
        this.controllerCursorSensitivity = this.controllerSensitivity(
                "controls.java_reforged.cursor_sensitivity",
                DEFAULT_CONTROLLER_CURSOR_SENSITIVITY
        );
        this.controllerCameraSensitivity = this.controllerSensitivity(
                "controls.java_reforged.camera_sensitivity",
                DEFAULT_CONTROLLER_CAMERA_SENSITIVITY
        );
        this.controllerSpyglassSensitivity = this.boundedControllerSensitivity(
                "controls.java_reforged.spyglass_sensitivity",
                DEFAULT_CONTROLLER_SPYGLASS_SENSITIVITY,
                50
        );
        this.invertControllerMovementY = OptionInstance.createBoolean(
                "controls.java_reforged.invert_movement_y",
                false,
                value -> this.save()
        );
        this.invertControllerCameraX = OptionInstance.createBoolean(
                "controls.java_reforged.invert_camera_x",
                false,
                value -> this.save()
        );
        this.invertControllerCameraY = OptionInstance.createBoolean(
                "controls.java_reforged.invert_camera_y",
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

    public OptionInstance<UiSizeMode> interfaceMode() {
        return this.interfaceMode;
    }

    public OptionInstance<Integer> bottomHudScale() {
        return this.bottomHudScale;
    }

    public OptionInstance<Integer> splitBottomHudScale() {
        return this.splitBottomHudScale;
    }

    public float bottomHudScaleMultiplier(boolean splitScreen) {
        return splitScreen
            ? HudScaleCalculator.splitMultiplier(this.splitBottomHudScale.get())
            : HudScaleCalculator.singlePlayerMultiplier(this.bottomHudScale.get());
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

    public OptionInstance<Double> legacyGamma() {
        return this.legacyGamma;
    }

    public OptionInstance<Double> hudTransparency() {
        return this.hudTransparency;
    }

    public OptionInstance<Boolean> showPlayerModelHud() {
        return this.showPlayerModelHud;
    }

    public OptionInstance<Boolean> showPlayerNameHud() { return this.showPlayerNameHud; }

    public OptionInstance<Boolean> showControlHints() { return this.showControlHints; }

    public OptionInstance<Boolean> showLocatorBar() { return this.showLocatorBar; }

    public OptionInstance<Boolean> showWorldPlayerNames() { return this.showWorldPlayerNames; }

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

    public OptionInstance<AttackIndicatorStatus> attackIndicator() { return this.attackIndicator; }

    public OptionInstance<HumanoidArm> mainHand() { return this.mainHand; }

    public OptionInstance<CraftingMode> craftingMode() { return this.craftingMode; }

    public OptionInstance<LegacyBehaviorMode> flightMode() { return this.flightMode; }

    public OptionInstance<LegacyBehaviorMode> constructionMode() { return this.constructionMode; }

    public OptionInstance<Double> controllerCursorSensitivity() {
        return this.controllerCursorSensitivity;
    }

    public OptionInstance<Double> controllerCameraSensitivity() {
        return this.controllerCameraSensitivity;
    }

    public OptionInstance<Double> controllerSpyglassSensitivity() {
        return this.controllerSpyglassSensitivity;
    }

    public OptionInstance<Boolean> invertControllerMovementY() {
        return this.invertControllerMovementY;
    }

    public OptionInstance<Boolean> invertControllerCameraX() {
        return this.invertControllerCameraX;
    }

    public OptionInstance<Boolean> invertControllerCameraY() {
        return this.invertControllerCameraY;
    }

    public void setGuiScale(int guiScale) {
        this.guiScale.set(guiScale);
    }

    public void setInterfaceMode(UiSizeMode interfaceMode) {
        this.interfaceMode.set(interfaceMode);
    }

    public void setBottomHudScale(int bottomHudScale) {
        this.bottomHudScale.set(HudScaleCalculator.resolveLevel(bottomHudScale));
    }

    public void setSplitBottomHudScale(int splitBottomHudScale) {
        this.splitBottomHudScale.set(HudScaleCalculator.resolveLevel(splitBottomHudScale));
    }

    public void setFov(int fov) {
        this.fov.set(fov);
    }

    public void setBrightness(double brightness) {
        this.brightness.set(brightness);
    }

    public void setLegacyGamma(double legacyGamma) {
        this.legacyGamma.set(legacyGamma);
    }

    public void setHudTransparency(double hudTransparency) {
        this.hudTransparency.set(hudTransparency);
    }

    public void setShowPlayerModelHud(boolean showPlayerModelHud) {
        this.showPlayerModelHud.set(showPlayerModelHud);
    }

    public void setShowPlayerNameHud(boolean value) { this.showPlayerNameHud.set(value); }

    public void setShowControlHints(boolean value) { this.showControlHints.set(value); }

    public void setShowLocatorBar(boolean value) { this.showLocatorBar.set(value); }

    public void setShowWorldPlayerNames(boolean value) { this.showWorldPlayerNames.set(value); }

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

    public void setAttackIndicator(AttackIndicatorStatus value) { this.attackIndicator.set(value); }

    public void setMainHand(HumanoidArm value) { this.mainHand.set(value); }

    public void setCraftingMode(CraftingMode value) { this.craftingMode.set(value); }

    public void setFlightMode(LegacyBehaviorMode value) { this.flightMode.set(value); }

    public void setConstructionMode(LegacyBehaviorMode value) { this.constructionMode.set(value); }

    public void setControllerCursorSensitivity(double controllerCursorSensitivity) {
        this.controllerCursorSensitivity.set(clampControllerSensitivity(
                controllerCursorSensitivity,
                DEFAULT_CONTROLLER_CURSOR_SENSITIVITY
        ));
    }

    public void setControllerCameraSensitivity(double controllerCameraSensitivity) {
        this.controllerCameraSensitivity.set(clampControllerSensitivity(
                controllerCameraSensitivity,
                DEFAULT_CONTROLLER_CAMERA_SENSITIVITY
        ));
    }

    public void setControllerSpyglassSensitivity(double controllerSpyglassSensitivity) {
        this.controllerSpyglassSensitivity.set(controllerSpyglassSensitivity);
    }

    public void setInvertControllerMovementY(boolean invertControllerMovementY) {
        this.invertControllerMovementY.set(invertControllerMovementY);
    }

    public void setInvertControllerCameraX(boolean invertControllerCameraX) {
        this.invertControllerCameraX.set(invertControllerCameraX);
    }

    public void setInvertControllerCameraY(boolean invertControllerCameraY) {
        this.invertControllerCameraY.set(invertControllerCameraY);
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
        this.attackIndicator.set(values.attackIndicator());
        this.mainHand.set(values.mainHand());
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
            boolean operatorItemsTab,
            AttackIndicatorStatus attackIndicator,
            HumanoidArm mainHand
    ) {
    }

    private void save() {
        ClientRuntime.INSTANCE.playerOptions().save();
    }

    private OptionInstance<LegacyBehaviorMode> legacyBehaviorMode(String captionKey) {
        return new OptionInstance<>(
                captionKey,
                OptionInstance.noTooltip(),
                (caption, value) -> value.caption(),
                new OptionInstance.Enum<>(
                        Arrays.asList(LegacyBehaviorMode.values()),
                        Codec.STRING.xmap(LegacyBehaviorMode::byName, LegacyBehaviorMode::serializedName)
                ),
                LegacyBehaviorMode.LEGACY,
                value -> this.save()
        );
    }

    private void applyInterfaceMode(UiSizeMode mode) {
        this.save();
        if (this.slotId != 0) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.isRunning()) {
            minecraft.execute(() -> ClientRuntime.INSTANCE
                    .viewportResize()
                    .applyLayoutTransition(minecraft));
        }
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

    private OptionInstance<Boolean> visibilityOption(String key, boolean defaultValue) {
        return new OptionInstance<>(
                key,
                OptionInstance.noTooltip(),
                (caption, value) -> Component.translatable(value
                        ? "options.java_reforged.show"
                        : "options.java_reforged.hide"),
                OptionInstance.BOOLEAN_VALUES,
                defaultValue,
                value -> this.save()
        );
    }

    private OptionInstance<Double> controllerSensitivity(String key, double baseValue) {
        return new OptionInstance<>(
                key,
                OptionInstance.noTooltip(),
                (caption, value) -> Component.translatable(
                        "options.percent_value",
                        caption,
                        (int) Math.round(value * 100.0D / baseValue)
                ),
                new OptionInstance.IntRange(0, 200).xmap(
                        percent -> baseValue * percent / 100.0D,
                        value -> Math.max(0, Math.min(200, (int) Math.round(value * 100.0D / baseValue))),
                        true
                ),
                Codec.doubleRange(0.0D, baseValue * 2.0D),
                baseValue,
                value -> this.save()
        );
    }

    private static double clampControllerSensitivity(double value, double baseValue) {
        if (!Double.isFinite(value)) {
            return baseValue;
        }
        return Math.max(0.0D, Math.min(baseValue * 2.0D, value));
    }

    private OptionInstance<Double> boundedControllerSensitivity(
            String key,
            double defaultValue,
            int maximum
    ) {
        return new OptionInstance<>(
                key,
                OptionInstance.noTooltip(),
                (caption, value) -> Component.translatable(
                        "options.percent_value",
                        caption,
                        (int) Math.round(value * 100.0D / maximum)
                ),
                new OptionInstance.IntRange(0, 100).xmap(
                        percent -> percent * maximum / 100.0D,
                        value -> (int) Math.round(value * 100.0D / maximum),
                        true
                ),
                Codec.doubleRange(0.0D, maximum),
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
