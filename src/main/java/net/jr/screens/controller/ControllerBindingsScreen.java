package net.jr.screens.controller;

import com.mojang.blaze3d.platform.InputConstants;
import net.jr.client.input.InputApi;
import net.jr.client.input.binding.GamepadBindingRegistry;
import net.jr.client.input.binding.GamepadInputChord;
import net.jr.client.input.binding.KeyboardMouseInputBindings;
import net.jr.client.input.gamepad.GamepadDigitalInput;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.InputQuirks;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Set;

public class ControllerBindingsScreen extends OptionsSubScreen implements ControllerMenuCaptureAware {
    private static final Component TITLE = Component.translatable("controls.keybinds.title");
    private static final Component MODE_LABEL = Component.translatable("controls.java_reforged.mode");
    private static final Component CAPTURE_HINT = Component.translatable("controls.java_reforged.capture_hint");
    private static InputMode lastMode = InputMode.KEYBOARD;

    public @Nullable KeyMapping selectedKey;
    private @Nullable KeyMapping selectedControllerKey;
    public long lastKeySelection;
    private ControllerBindingsList keyBindsList;
    private Button resetButton;
    private final InputMode inputMode;

    private InputConstants.Key lastPressedKey = InputConstants.UNKNOWN;
    private InputConstants.Key lastPressedModifier = InputConstants.UNKNOWN;
    private boolean isLastKeyHeldDown;
    private boolean isLastModifierHeldDown;
    private final Set<GamepadDigitalInput> blockedControllerInputs =
        EnumSet.noneOf(GamepadDigitalInput.class);
    private final Set<GamepadDigitalInput> capturedControllerInputs =
        EnumSet.noneOf(GamepadDigitalInput.class);

    public ControllerBindingsScreen(Screen lastScreen, Options options) {
        this(lastScreen, options, lastMode);
    }

    public ControllerBindingsScreen(Screen lastScreen, Options options, InputMode inputMode) {
        super(lastScreen, options, TITLE);
        this.inputMode = inputMode;
        lastMode = inputMode;
    }

    @Override
    protected void addContents() {
        GamepadBindingRegistry.get().ensureLoaded(minecraft);
        keyBindsList = layout.addToContents(new ControllerBindingsList(this, minecraft));
    }

    @Override
    protected void addOptions() {
    }

    @Override
    protected void addFooter() {
        resetButton = Button.builder(Component.translatable("controls.resetAll"), button -> {
            if (isGamepadMode()) {
                GamepadBindingRegistry.get().resetAllToDefaults();
            } else {
                for (KeyMapping keyMapping : options.keyMappings) {
                    keyMapping.setToDefault();
                }
            }
            keyBindsList.resetMappingAndUpdateButtons();
        }).build();

        CycleButton<InputMode> modeButton = CycleButton.builder(InputMode::label, inputMode)
            .withValues(InputMode.values())
            .create(0, 0, 110, 20, MODE_LABEL, (button, value) -> {
                lastMode = value;
                minecraft.gui.setScreen(new ControllerBindingsScreen(lastScreen, options, value));
            });

        LinearLayout footer = layout.addToFooter(LinearLayout.horizontal().spacing(8));
        footer.addChild(modeButton);
        footer.addChild(resetButton);
        footer.addChild(Button.builder(CommonComponents.GUI_DONE, button -> onClose()).build());
    }

    @Override
    protected void repositionElements() {
        layout.arrangeElements();
        keyBindsList.updateSize(width, layout);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!isGamepadMode() && selectedKey != null) {
            assignSelectedKeyboardMouseKey(KeyboardMouseInputBindings.mouseButton(event.button()));
            return true;
        }
        if (isGamepadMode() && selectedControllerKey != null) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!isGamepadMode() && selectedKey != null && scrollY != 0.0D) {
            assignSelectedKeyboardMouseKey(KeyboardMouseInputBindings.mouseScroll(scrollY));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        int scanCode = event.scancode();
        if (!isGamepadMode() && selectedKey != null) {
            InputConstants.Key key = InputConstants.getKey(event);
            if (lastPressedModifier == InputConstants.UNKNOWN && KeyModifier.isKeyCodeModifier(key)) {
                lastPressedModifier = key;
                isLastModifierHeldDown = true;
            } else {
                lastPressedKey = key;
                isLastKeyHeldDown = true;
            }
            return true;
        }

        if (isGamepadMode() && selectedControllerKey != null) {
            if (event.isEscape()) {
                cancelControllerSelection();
                return true;
            }
            if (keyCode == 259 || keyCode == 261) {
                GamepadBindingRegistry.get().setBinding(selectedControllerKey, (GamepadInputChord) null);
                cancelControllerSelection();
                keyBindsList.resetMappingAndUpdateButtons();
            }
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        int keyCode = event.key();
        int scanCode = event.scancode();
        if (!isGamepadMode() && selectedKey != null && (!InputQuirks.ON_OSX || scanCode != 63)) {
            if (event.isEscape()) {
                selectedKey.setKeyModifierAndCode(KeyModifier.NONE, InputConstants.UNKNOWN);
                selectedKey.setKey(InputConstants.UNKNOWN);
                resetKeyboardSelectionState();
                keyBindsList.resetMappingAndUpdateButtons();
            } else {
                InputConstants.Key key = InputConstants.getKey(event);
                if (lastPressedKey.equals(key)) {
                    isLastKeyHeldDown = false;
                } else if (lastPressedModifier.equals(key)) {
                    isLastModifierHeldDown = false;
                }

                if (!isLastKeyHeldDown && !isLastModifierHeldDown) {
                    assignSelectedKeyboardMouseKey(
                        !lastPressedKey.equals(InputConstants.UNKNOWN) ? lastPressedKey : lastPressedModifier
                    );
                } else {
                    return true;
                }
            }
            return true;
        }
        return super.keyReleased(event);
    }

    @Override
    public void tick() {
        super.tick();
        if (!isGamepadMode() || selectedControllerKey == null || !InputApi.isGamepadConnected()) {
            return;
        }

        GamepadBindingRegistry registry = GamepadBindingRegistry.get();
        Set<GamepadDigitalInput> pressedInputs = EnumSet.noneOf(GamepadDigitalInput.class);
        pressedInputs.addAll(registry.currentlyPressedInputs());
        if (!capturedControllerInputs.isEmpty() && pressedInputs.isEmpty()) {
            registry.setBinding(
                selectedControllerKey,
                GamepadInputChord.of(new ArrayList<>(capturedControllerInputs))
            );
            cancelControllerSelection();
            keyBindsList.resetMappingAndUpdateButtons();
            return;
        }

        for (GamepadDigitalInput input : GamepadDigitalInput.values()) {
            if (!pressedInputs.contains(input)) {
                blockedControllerInputs.remove(input);
            } else if (!blockedControllerInputs.contains(input)) {
                capturedControllerInputs.add(input);
            }
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        resetButton.active = isGamepadMode()
            ? GamepadBindingRegistry.get().hasAnyCustomBindings()
            : hasKeyboardOverrides();
        if (selectedControllerKey != null) {
            graphics.text(
                font,
                CAPTURE_HINT,
                (width - font.width(CAPTURE_HINT)) / 2,
                layout.getHeaderHeight() - 8,
                0xFFFF55,
                false
            );
        }
    }

    @Override
    public void removed() {
        super.removed();
        GamepadBindingRegistry.get().save();
    }

    public boolean isGamepadMode() {
        return inputMode == InputMode.GAMEPAD;
    }

    public void beginKeyboardSelection(KeyMapping keyMapping) {
        selectedControllerKey = null;
        selectedKey = keyMapping;
        keyBindsList.resetMappingAndUpdateButtons();
    }

    public void beginGamepadSelection(KeyMapping keyMapping) {
        selectedKey = null;
        selectedControllerKey = keyMapping;
        blockedControllerInputs.clear();
        capturedControllerInputs.clear();
        blockedControllerInputs.addAll(GamepadBindingRegistry.get().currentlyPressedInputs());
        keyBindsList.resetMappingAndUpdateButtons();
    }

    @Nullable
    KeyMapping getSelectedControllerKey() {
        return selectedControllerKey;
    }

    @Override
    public boolean javareforged$isCapturingControllerBinding() {
        return selectedControllerKey != null;
    }

    private void cancelControllerSelection() {
        selectedControllerKey = null;
        blockedControllerInputs.clear();
        capturedControllerInputs.clear();
    }

    private void assignSelectedKeyboardMouseKey(InputConstants.Key key) {
        if (selectedKey == null) {
            return;
        }

        KeyModifier modifier = KeyModifier.getKeyModifier(lastPressedModifier);
        if (key.equals(lastPressedModifier)) {
            modifier = KeyModifier.NONE;
        }
        selectedKey.setKeyModifierAndCode(modifier, key);
        selectedKey.setKey(key);
        resetKeyboardSelectionState();
        keyBindsList.resetMappingAndUpdateButtons();
    }

    private void resetKeyboardSelectionState() {
        selectedKey = null;
        lastKeySelection = Util.getMillis();
        lastPressedKey = InputConstants.UNKNOWN;
        lastPressedModifier = InputConstants.UNKNOWN;
        isLastKeyHeldDown = false;
        isLastModifierHeldDown = false;
    }

    private boolean hasKeyboardOverrides() {
        for (KeyMapping keyMapping : options.keyMappings) {
            if (!keyMapping.isDefault()) {
                return true;
            }
        }
        return false;
    }

    public enum InputMode {
        KEYBOARD("controls.java_reforged.mode.keyboard"),
        GAMEPAD("controls.java_reforged.mode.gamepad");

        private final String translationKey;

        InputMode(String translationKey) {
            this.translationKey = translationKey;
        }

        public Component label() {
            return Component.translatable(translationKey);
        }
    }

}
