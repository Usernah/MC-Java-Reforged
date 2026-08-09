package net.jr.client.input.runtime;

import net.jr.Java_reforged;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.jr.client.input.InputApi;
import net.jr.client.input.binding.BindingContext;
import net.jr.client.input.binding.KeyboardMouseInputBindings;
import net.jr.client.input.binding.ModKeyBindings;
import net.jr.client.input.gamepad.GamepadDigitalInput;
import net.jr.client.sound.action.InputActionSounds;
import net.jr.client.ui.container.actions.ContainerSlotFocusController;
import net.jr.client.ui.navigation.UiInputModeController;
import net.jr.mixin.controlhints.AbstractContainerScreenAccessor;
import net.jr.screens.controller.ControllerBindingsScreen;
import net.jr.screens.controller.ControllerMenuCaptureAware;
import net.minecraft.util.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = Java_reforged.MODID, value = Dist.CLIENT)
public final class MappedActionProcessor {
    private static final long UI_NAV_REPEAT_DELAY_MS = 250L;
    private static final long UI_NAV_REPEAT_INTERVAL_MS = 100L;
    private static final long UI_DOUBLE_CLICK_INTERVAL_MS = 250L;
    private static final ActionState STATE = new ActionState();

    private MappedActionProcessor() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();

        InputApi.tickGamepadJoin(minecraft);
        processCurrentClient(minecraft, InputApi.isGamepadConnected());
    }


    public static boolean handleScreenMouseButton(Screen screen, int button) {
        ActionState state = state();
        if (!(screen instanceof AbstractContainerScreen<?> containerScreen)
                || shouldBypassUiActionHandling(screen)
                || screen.getFocused() instanceof EditBox) {
            return false;
        }
        return handleMouseContainerAction(state, containerScreen, button);
    }


    public static void rememberSuppressedScreenMouseButton(int button) {
        state().suppressedScreenMouseButtons.add(button);
    }

    public static boolean suppressConsumedScreenMouseButton(@Nullable Screen screen, int button, int action) {
        ActionState state = state();
        if (!state.suppressedScreenMouseButtons.contains(button)) {
            return false;
        }

        if (action == GLFW.GLFW_RELEASE) {
            state.suppressedScreenMouseButtons.remove(button);
            clearHandledMouseButton(state, screen, button);
        }
        return true;
    }

    public static boolean handleScreenKey(Screen screen, int keyCode, int scanCode) {
        return handleScreenKey(screen, keyCode, scanCode, GLFW.GLFW_PRESS);
    }

    public static boolean handleScreenKey(Screen screen, int keyCode, int scanCode, int action) {
        ActionState state = state();
        if (state.dispatchingSyntheticKey
            || shouldBypassUiActionHandling(screen)
            || screen.getFocused() instanceof EditBox) {
            return false;
        }

        return handleKeyboardScreenAction(state, screen, keyCode, scanCode, action);
    }

    public static void rememberSuppressedScreenKey(int keyCode, int scanCode) {
        state().suppressedScreenKeys.add(keyCode);
    }

    public static boolean suppressConsumedScreenKey(int keyCode, int scanCode, int action) {
        ActionState state = state();
        if (!state.suppressedScreenKeys.contains(keyCode)) {
            return false;
        }

        if (action == GLFW.GLFW_RELEASE) {
            releaseNavigationRepeatKey(state, keyCode, scanCode);
            state.suppressedScreenKeys.remove(keyCode);
        }
        return true;
    }

    public static boolean handleScreenMouseScroll(Screen screen, double scrollY) {
        ActionState state = state();
        if (!(screen instanceof AbstractContainerScreen<?> containerScreen)
            || shouldBypassUiActionHandling(screen)
            || screen.getFocused() instanceof EditBox) {
            return false;
        }
        return handleMouseScrollContainerAction(state, containerScreen, scrollY);
    }

    public static boolean consumeHandledMouseRelease(Screen screen, int button) {
        return clearHandledMouseButton(state(), screen, button);
    }

    private static void processCurrentClient(Minecraft minecraft, boolean gamepadInput) {
        ActionState state = STATE;
        Screen currentScreen = minecraft.gui.screen();

        if (gamepadInput) {
            InputApi.ensureBindingsLoaded(minecraft);
            BindingContext actionContext = resolveActionContext(currentScreen);
            if (actionContext != state.lastActionContext) {
                InputApi.releaseBindings();
                drainUiActionClickCounters();
                InputApi.suppressHeldBindings();
                syncUiButtonLatches(state);
                state.lastActionContext = actionContext;
            }
            InputApi.applyBindings(actionContext);
        } else {
            InputApi.releaseBindings();
            state.lastActionContext = BindingContext.GAMEPLAY;
        }

        boolean hasScreen = currentScreen != null;
        if (hasScreen != state.lastHadScreen) {
            syncUiButtonLatches(state);
            state.lastHadScreen = hasScreen;
        }

        LocalPlayer player = minecraft.player;
        if (player != null) {
            updateGameplayActionSounds(minecraft, player, state);
        }

        if (currentScreen != null) {
            handleMenuActions(minecraft, currentScreen, state, gamepadInput);
        } else if (player != null) {
            handleGameplayActions(minecraft, player, state, gamepadInput);
        }
        syncInGameScreenSounds(minecraft, currentScreen, state);
    }

    private static boolean clearHandledMouseButton(ActionState state, @Nullable Screen screen, int button) {
        if (screen == null) {
            return false;
        }

        Set<Integer> buttons = state.screenHandledMouseButtons.get(screen);
        if (buttons == null || !buttons.remove(button)) {
            return false;
        }

        if (buttons.isEmpty()) {
            state.screenHandledMouseButtons.remove(screen);
        }
        return true;
    }

    private static boolean handleKeyboardScreenAction(ActionState state, Screen screen, int keyCode, int scanCode, int action) {
        if (matchesKey(ModKeyBindings.UI_BACK, keyCode, scanCode)) {
            screen.onClose();
            markHandledByInput(state, ModKeyBindings.UI_BACK);
            return true;
        }

        UiNavigationAction navigationAction = UiNavigationAction.fromKey(keyCode, scanCode);
        if (navigationAction != null) {
            if (action == GLFW.GLFW_PRESS) {
                dispatchNavigationAction(state, screen, navigationAction, true, true);
            } else {
                startNavigationRepeat(state, screen, navigationAction, true);
            }
            markHandledByInput(state, navigationAction.mapping);
            return true;
        }

        if (screen instanceof AbstractContainerScreen<?> containerScreen
            && handleKeyboardContainerAction(state, containerScreen, keyCode, scanCode)) {
            return true;
        }

        if (matchesKey(ModKeyBindings.UI_CONFIRM, keyCode, scanCode)) {
            boolean handled = GamepadInputProcessor.activateFocusedElement(screen, 0);
            if (!handled && keyCode != GLFW.GLFW_KEY_ENTER) {
                state.dispatchingSyntheticKey = true;
                try {
                    handled = screen.keyPressed(new net.minecraft.client.input.KeyEvent(GLFW.GLFW_KEY_ENTER, 0, 0));
                } finally {
                    state.dispatchingSyntheticKey = false;
                }
            }
            if (handled) {
                state.suppressNextScreenTransitionSoundFrom = screen;
                screen.afterKeyboardAction();
                markHandledByInput(state, ModKeyBindings.UI_CONFIRM);
                return true;
            }
        }

        return false;
    }

    private static boolean handleKeyboardContainerAction(ActionState state, AbstractContainerScreen<?> screen, int keyCode, int scanCode) {
        ContainerAction action = ContainerAction.fromKey(keyCode, scanCode);
        return action != null && executeContainerAction(state, screen, action);
    }

    private static boolean handleMouseContainerAction(ActionState state, AbstractContainerScreen<?> screen, int button) {
        if (isHandledMouseButton(state, screen, button)) {
            return true;
        }

        ContainerAction action = ContainerAction.fromMouseButton(button);
        if (action != null && action.usesVanillaPhysicalMouseButton(button)) {
            playVanillaMouseContainerActionSound(screen, action);
            return false;
        }
        return action != null
            && rememberMouseButtonIfHandled(state, screen, button, executeContainerAction(state, screen, action));
    }

    private static void playVanillaMouseContainerActionSound(
        AbstractContainerScreen<?> screen,
        ContainerAction action
    ) {
        Slot hoveredSlot = ((AbstractContainerScreenAccessor) screen).javareforged$getHoveredSlot();
        if (hoveredSlot != null && hoveredSlot.isActive()) {
            if (action != ContainerAction.QUICK_MOVE || hoveredSlot.hasItem()) {
                InputActionSounds.playClick();
            } else {
                InputActionSounds.playBlock();
            }
        } else if (action == ContainerAction.QUICK_MOVE) {
            InputActionSounds.playBlock();
        }
    }

    private static boolean handleMouseScrollContainerAction(ActionState state, AbstractContainerScreen<?> screen, double scrollY) {
        ContainerAction action = ContainerAction.fromMouseScroll(scrollY);
        return action != null && executeContainerAction(state, screen, action);
    }

    private static boolean executeContainerAction(ActionState state, AbstractContainerScreen<?> screen, ContainerAction action) {
        if (action.isBulkTransfer() && !isBulkTransferContainer(screen)) {
            return false;
        }

        boolean handled = switch (action) {
            case TAKE_ALL -> ContainerSlotFocusController.takeAllFromContainer(screen);
            case STORE_ALL -> ContainerSlotFocusController.storeAllInContainer(screen);
            case QUICK_MOVE -> ContainerSlotFocusController.quickMoveFocusedOrHoveredSlot(screen);
            case PRIMARY -> ContainerSlotFocusController.pickupFocusedOrHoveredSlot(screen, 0);
            case ALTERNATE -> ContainerSlotFocusController.pickupFocusedOrHoveredSlot(screen, 1);
        };

        if (handled) {
            InputActionSounds.playClick();
            markHandledByInput(state, action.mapping);
        } else if (action.consumesFailure) {
            InputActionSounds.playBlock();
            markHandledByInput(state, action.mapping);
        }
        return handled || action.consumesFailure;
    }

    private static void consumeMappedContainerAction(
        ActionState state,
        AbstractContainerScreen<?> screen,
        ContainerAction action,
        boolean controllerUiActive
    ) {
        if (consumeScreenHandledMapping(state, action.mapping) || !controllerUiActive) {
            return;
        }

        while (action.mapping.consumeClick()) {
            executeContainerAction(state, screen, action);
        }
    }

    private static boolean isHandledMouseButton(ActionState state, Screen screen, int button) {
        Set<Integer> buttons = state.screenHandledMouseButtons.get(screen);
        return buttons != null && buttons.contains(button);
    }

    private static boolean rememberMouseButtonIfHandled(ActionState state, Screen screen, int button, boolean handled) {
        if (!handled) {
            return false;
        }

        state.screenHandledMouseButtons
            .computeIfAbsent(screen, ignored -> new java.util.HashSet<>())
            .add(button);
        return true;
    }

    private static boolean matchesKey(KeyMapping mapping, int keyCode, int scanCode) {
        return !mapping.isUnbound()
            && mapping.matches(new net.minecraft.client.input.KeyEvent(keyCode, scanCode, 0));
    }

    private static boolean isBulkTransferContainer(AbstractContainerScreen<?> screen) {
        return screen.getMenu() instanceof ChestMenu || screen.getMenu() instanceof ShulkerBoxMenu;
    }

    private static void drainClickCounter(KeyMapping mapping) {
        while (mapping.consumeClick()) {
            // The screen event already handled this press, so prevent a later tick duplicate.
        }
    }

    private static void drainUiActionClickCounters() {
        drainClickCounter(ModKeyBindings.UI_CONFIRM);
        drainClickCounter(ModKeyBindings.UI_BACK);
        drainClickCounter(ModKeyBindings.UI_ALTERNATE);
        drainClickCounter(ModKeyBindings.UI_NAV_UP);
        drainClickCounter(ModKeyBindings.UI_NAV_DOWN);
        drainClickCounter(ModKeyBindings.UI_NAV_LEFT);
        drainClickCounter(ModKeyBindings.UI_NAV_RIGHT);
        drainClickCounter(ModKeyBindings.UI_QUICK_MOVE);
        drainClickCounter(ModKeyBindings.UI_TAKE_ALL);
        drainClickCounter(ModKeyBindings.UI_STORE_ALL);
    }

    private static void markHandledByInput(ActionState state, KeyMapping mapping) {
        state.screenHandledMappings.add(mapping);
        drainClickCounter(mapping);
    }

    private static boolean consumeScreenHandledMapping(ActionState state, KeyMapping mapping) {
        if (!state.screenHandledMappings.contains(mapping)) {
            return false;
        }

        drainClickCounter(mapping);
        if (!mapping.isDown()) {
            state.screenHandledMappings.remove(mapping);
        }
        return true;
    }

    private static void handleMenuActions(Minecraft minecraft, Screen screen, ActionState state, boolean gamepadInput) {
        if (shouldBypassUiActionHandling(screen)) {
            clearNavigationRepeat(state);
            syncUiButtonLatches(state);
            return;
        }

        boolean controllerConnected = gamepadInput && InputApi.isGamepadConnected();
        boolean pauseButtonDown = controllerConnected
            && InputApi.isPressed(GamepadDigitalInput.BUTTON_START);
        if (pauseButtonDown) {
            InputApi.markGamepadInput();
        }
        if (pauseButtonDown && !state.wasPauseButtonDown && screen instanceof PauseScreen) {
            screen.onClose();
        }
        state.wasPauseButtonDown = pauseButtonDown;

        boolean controllerUiActive = controllerConnected && InputApi.isGamepadMode();

        if (!consumeScreenHandledMapping(state, ModKeyBindings.UI_BACK) && controllerUiActive) {
            while (ModKeyBindings.UI_BACK.consumeClick()) {
                screen.onClose();
            }
        }

        boolean textInputFocused = screen.getFocused() instanceof EditBox;
        if (!textInputFocused) {
            handleNavigationPresses(state, screen, controllerUiActive);
            tickNavigationRepeat(state, screen);

            if (screen instanceof AbstractContainerScreen<?> containerScreen) {
                consumeMappedContainerAction(state, containerScreen, ContainerAction.TAKE_ALL, controllerUiActive);
                consumeMappedContainerAction(state, containerScreen, ContainerAction.STORE_ALL, controllerUiActive);
                consumeMappedContainerAction(state, containerScreen, ContainerAction.QUICK_MOVE, controllerUiActive);
            }
        } else {
            clearNavigationRepeat(state);
        }

        handlePointerAction(state, screen, textInputFocused, state.primaryPointer, controllerUiActive);
        handlePointerAction(state, screen, textInputFocused, state.alternatePointer, controllerUiActive);
    }

    private static void handlePointerAction(
        ActionState state,
        Screen screen,
        boolean textInputFocused,
        UiPointerAction action,
        boolean controllerUiActive
    ) {
        boolean down = action.mapping.isDown();
        if (consumeScreenHandledMapping(state, action.mapping) || !shouldHandleSyntheticUiButtonActions(controllerUiActive)) {
            action.wasDown = down;
            return;
        }

        if (down && !action.wasDown && !textInputFocused) {
            double mouseX = GamepadInputProcessor.cursorX();
            double mouseY = GamepadInputProcessor.cursorY();
            boolean handled = false;
            boolean handledByFocusedElement = false;
            action.skipNextRelease = false;
            action.hasPressPosition = false;
            action.containerQuickCraft = false;
            if (UiInputModeController.isFocusNavigationActive()) {
                GamepadInputProcessor.FocusedElementCenter focusCenter =
                    GamepadInputProcessor.focusedElementCenter(screen);
                if (focusCenter != null) {
                    mouseX = focusCenter.guiX();
                    mouseY = focusCenter.guiY();
                }

                if (shouldBeginContainerQuickCraft(screen, true)) {
                    handled = clickScreen(screen, action, mouseX, mouseY);
                    if (handled) {
                        screen.mouseDragged(mouseEvent(mouseX, mouseY, action.mouseButton), 0.0D, 0.0D);
                        action.containerQuickCraft = true;
                        action.dragX = mouseX;
                        action.dragY = mouseY;
                    }
                } else if (hasFocusedContainerSlot(screen)) {
                    handled = clickScreen(screen, action, mouseX, mouseY);
                } else {
                    // Spatial widgets must receive the same complete pointer
                    // lifecycle as mouse/joystick-pointer input. Dispatching
                    // UiAction first executes the callback but bypasses their
                    // pressed state and matching release.
                    if (focusCenter != null) {
                        handled = clickScreen(screen, action, mouseX, mouseY);
                    }
                    if (!handled) {
                        handledByFocusedElement = GamepadInputProcessor.activateFocusedElement(screen, action.mouseButton);
                        handled = handledByFocusedElement;
                    }
                    if (!handled && focusCenter == null) {
                        handled = clickScreen(screen, action, mouseX, mouseY);
                    }
                }
                if (!handled && action.enterFallback) {
                    handled = screen.keyPressed(new net.minecraft.client.input.KeyEvent(GLFW.GLFW_KEY_ENTER, 0, 0));
                }
                screen.afterKeyboardAction();
            } else {
                boolean beginQuickCraft = shouldBeginContainerQuickCraft(screen, false);
                handled = clickScreen(screen, action, mouseX, mouseY);
                if (handled && beginQuickCraft) {
                    screen.mouseDragged(mouseEvent(mouseX, mouseY, action.mouseButton), 0.0D, 0.0D);
                    action.containerQuickCraft = true;
                    action.dragX = mouseX;
                    action.dragY = mouseY;
                }
            }

            if (handled) {
                action.pressX = mouseX;
                action.pressY = mouseY;
                action.hasPressPosition = true;
                action.skipNextRelease = handledByFocusedElement;
                InputActionSounds.playClick();
            }
        } else if (down && action.wasDown && action.containerQuickCraft) {
            double dragX = GamepadInputProcessor.cursorX();
            double dragY = GamepadInputProcessor.cursorY();
            GamepadInputProcessor.FocusedElementCenter focusCenter =
                GamepadInputProcessor.focusedElementCenter(screen);
            if (focusCenter != null) {
                dragX = focusCenter.guiX();
                dragY = focusCenter.guiY();
            }
            if (dragX != action.dragX || dragY != action.dragY) {
                screen.mouseDragged(
                    mouseEvent(dragX, dragY, action.mouseButton),
                    dragX - action.dragX,
                    dragY - action.dragY
                );
                action.dragX = dragX;
                action.dragY = dragY;
            }
        } else if (!down && action.wasDown) {
            if (action.skipNextRelease) {
                action.skipNextRelease = false;
            } else {
                double releaseX = action.containerQuickCraft
                    ? action.dragX
                    : action.hasPressPosition ? action.pressX : GamepadInputProcessor.cursorX();
                double releaseY = action.containerQuickCraft
                    ? action.dragY
                    : action.hasPressPosition ? action.pressY : GamepadInputProcessor.cursorY();
                screen.mouseReleased(mouseEvent(releaseX, releaseY, action.mouseButton));
            }
            action.hasPressPosition = false;
            action.containerQuickCraft = false;
        }
        action.wasDown = down;
    }

    private static boolean shouldBeginContainerQuickCraft(Screen screen, boolean requireFocusedSlot) {
        return screen instanceof AbstractContainerScreen<?> containerScreen
            && (!requireFocusedSlot || ContainerSlotFocusController.isVanillaSlotFocusActive(containerScreen))
            && !containerScreen.getMenu().getCarried().isEmpty();
    }

    private static boolean hasFocusedContainerSlot(Screen screen) {
        return screen instanceof AbstractContainerScreen<?> containerScreen
            && ContainerSlotFocusController.isVanillaSlotFocusActive(containerScreen);
    }

    private static boolean clickScreen(
        Screen screen,
        UiPointerAction action,
        double mouseX,
        double mouseY
    ) {
        long now = Util.getMillis();
        boolean doubleClick = action.lastClickScreen == screen
            && now - action.lastClickTimeMs < UI_DOUBLE_CLICK_INTERVAL_MS;
        boolean handled = screen.mouseClicked(mouseEvent(mouseX, mouseY, action.mouseButton), doubleClick);
        if (handled) {
            action.lastClickScreen = screen;
            action.lastClickTimeMs = now;
        }
        return handled;
    }

    private static void handleGameplayActions(Minecraft minecraft, LocalPlayer player, ActionState state, boolean gamepadInput) {
        boolean pauseButtonDown = gamepadInput
            && InputApi.isGamepadConnected()
            && InputApi.isPressed(GamepadDigitalInput.BUTTON_START);
        if (pauseButtonDown) {
            InputApi.markGamepadInput();
        }
        if (pauseButtonDown && !state.wasPauseButtonDown) {
            minecraft.pauseGame(false);
        }
        state.wasPauseButtonDown = pauseButtonDown;

        if (minecraft.gui.screen() != null) {
            return;
        }

        while (ModKeyBindings.GAMEPLAY_HOTBAR_PREV.consumeClick()) {
           player.getInventory().setSelectedSlot((player.getInventory().getSelectedSlot() - 1 + 9) % 9);
            //InputActionSounds.playHotbarFocus();
        }

        while (ModKeyBindings.GAMEPLAY_HOTBAR_NEXT.consumeClick()) {
            player.getInventory().setSelectedSlot((player.getInventory().getSelectedSlot() + 1) % 9);
            //InputActionSounds.playHotbarFocus();
        }
    }

    private static void updateGameplayActionSounds(Minecraft minecraft, LocalPlayer player, ActionState state) {
        boolean dropDown = minecraft.options.keyDrop.isDown();
        if (dropDown && !state.wasDropDown) {
            if (!player.getInventory().getSelectedItem().isEmpty()) {
                InputActionSounds.playGameplayItemPop();
            }
        }
        state.wasDropDown = dropDown;
    }

    private static boolean shouldBypassUiActionHandling(Screen screen) {
        if (screen instanceof ControllerMenuCaptureAware captureAware
            && captureAware.javareforged$isCapturingControllerBinding()) {
            return true;
        }
        return screen instanceof ControllerBindingsScreen bindingsScreen
            && bindingsScreen.selectedKey != null;
    }

    private static void syncUiButtonLatches(ActionState state) {
        clearNavigationRepeat(state);
        state.primaryPointer.sync();
        state.alternatePointer.sync();
        state.wasPauseButtonDown = InputApi.isPressed(GamepadDigitalInput.BUTTON_START);
    }

    private static boolean shouldHandleSyntheticUiButtonActions(boolean controllerUiActive) {
        return controllerUiActive;
    }

    private static BindingContext resolveActionContext(@Nullable Screen screen) {
        if (screen == null) {
            return BindingContext.GAMEPLAY;
        }

        if (screen instanceof AbstractContainerScreen<?>) {
            return BindingContext.CONTAINER;
        }

        return BindingContext.UI;
    }

    private static void syncInGameScreenSounds(Minecraft minecraft, @Nullable Screen currentScreen, ActionState state) {
        if (minecraft.level == null) {
            state.lastTrackedInGameScreen = null;
            return;
        }

        Screen previousScreen = state.lastTrackedInGameScreen;
        if (currentScreen == previousScreen) {
            if (state.suppressNextScreenTransitionSoundFrom == currentScreen) {
                state.suppressNextScreenTransitionSoundFrom = null;
            }
            return;
        }

        if (previousScreen == null && currentScreen != null) {
            playScreenTransitionSound(state, previousScreen, ScreenTransitionSound.CLICK);
        } else if (previousScreen != null && currentScreen == null) {
            playScreenTransitionSound(state, previousScreen, ScreenTransitionSound.BACK);
        } else if (previousScreen != null && currentScreen != null) {
            if (state.screenParents.get(previousScreen) == currentScreen) {
                playScreenTransitionSound(state, previousScreen, ScreenTransitionSound.BACK);
            } else {
                playScreenTransitionSound(state, previousScreen, ScreenTransitionSound.CLICK);
                state.screenParents.put(currentScreen, previousScreen);
            }
        }

        state.lastTrackedInGameScreen = currentScreen;
    }

    private static void playScreenTransitionSound(ActionState state, @Nullable Screen previousScreen, ScreenTransitionSound sound) {
        if (state.suppressNextScreenTransitionSoundFrom != null
            && state.suppressNextScreenTransitionSoundFrom == previousScreen) {
            state.suppressNextScreenTransitionSoundFrom = null;
            return;
        }

        switch (sound) {
            case CLICK -> InputActionSounds.playClick();
            case BACK -> InputActionSounds.playBack();
        }
    }

    private static void handleNavigationPresses(ActionState state, Screen screen, boolean controllerUiActive) {
        for (UiNavigationAction action : UiNavigationAction.values()) {
            if (consumeScreenHandledMapping(state, action.mapping) || !controllerUiActive) {
                continue;
            }

            while (action.mapping.consumeClick()) {
                dispatchNavigationAction(state, screen, action, false, true);
            }
        }
    }

    private static void dispatchNavigationAction(
        ActionState state,
        Screen screen,
        UiNavigationAction action,
        boolean keyboardHeld,
        boolean startRepeat
    ) {
        GamepadInputProcessor.dispatchFocusNavigation(screen, action.keyCode);
        if (startRepeat) {
            startNavigationRepeat(state, screen, action, keyboardHeld);
        }
    }

    private static void startNavigationRepeat(ActionState state, Screen screen, UiNavigationAction action, boolean keyboardHeld) {
        long now = Util.getMillis();
        if (state.navigationRepeatScreen != screen || state.navigationRepeatAction != action) {
            state.navigationRepeatScreen = screen;
            state.navigationRepeatAction = action;
            state.navigationRepeatStartedMs = now;
            state.navigationRepeatLastDispatchMs = now;
            state.navigationRepeatKeyboardHeld = keyboardHeld;
            return;
        }

        state.navigationRepeatKeyboardHeld |= keyboardHeld;
    }

    private static void tickNavigationRepeat(ActionState state, Screen screen) {
        if (state.navigationRepeatAction == null || state.navigationRepeatScreen != screen) {
            clearNavigationRepeat(state);
            return;
        }

        if (!isNavigationRepeatHeld(state)) {
            clearNavigationRepeat(state);
            return;
        }

        long now = Util.getMillis();
        if (now - state.navigationRepeatStartedMs < UI_NAV_REPEAT_DELAY_MS
            || now - state.navigationRepeatLastDispatchMs < UI_NAV_REPEAT_INTERVAL_MS) {
            return;
        }

        GamepadInputProcessor.dispatchFocusNavigation(screen, state.navigationRepeatAction.keyCode);
        state.navigationRepeatLastDispatchMs = now;
    }

    private static boolean isNavigationRepeatHeld(ActionState state) {
        return state.navigationRepeatAction != null
            && (state.navigationRepeatKeyboardHeld || state.navigationRepeatAction.mapping.isDown());
    }

    private static void releaseNavigationRepeatKey(ActionState state, int keyCode, int scanCode) {
        if (state.navigationRepeatAction == null || !state.navigationRepeatAction.matches(keyCode, scanCode)) {
            return;
        }

        state.navigationRepeatKeyboardHeld = false;
        if (!state.navigationRepeatAction.mapping.isDown()) {
            clearNavigationRepeat(state);
        }
    }

    private static void clearNavigationRepeat(ActionState state) {
        state.navigationRepeatScreen = null;
        state.navigationRepeatAction = null;
        state.navigationRepeatKeyboardHeld = false;
        state.navigationRepeatStartedMs = 0L;
        state.navigationRepeatLastDispatchMs = 0L;
    }

    private static ActionState state() {
        return STATE;
    }

    private enum ScreenTransitionSound {
        CLICK,
        BACK
    }

    private enum ContainerAction {
        TAKE_ALL(ModKeyBindings.UI_TAKE_ALL, true),
        STORE_ALL(ModKeyBindings.UI_STORE_ALL, true),
        QUICK_MOVE(ModKeyBindings.UI_QUICK_MOVE, true),
        PRIMARY(ModKeyBindings.UI_CONFIRM, false),
        ALTERNATE(ModKeyBindings.UI_ALTERNATE, false);

        private final KeyMapping mapping;
        private final boolean consumesFailure;

        ContainerAction(KeyMapping mapping, boolean consumesFailure) {
            this.mapping = mapping;
            this.consumesFailure = consumesFailure;
        }

        private boolean isBulkTransfer() {
            return this == TAKE_ALL || this == STORE_ALL;
        }

        @Nullable
        private static ContainerAction fromKey(int keyCode, int scanCode) {
            for (ContainerAction action : values()) {
                if (matchesKey(action.mapping, keyCode, scanCode)) {
                    return action;
                }
            }
            return null;
        }

        @Nullable
        private static ContainerAction fromMouseButton(int button) {
            for (ContainerAction action : values()) {
                if (KeyboardMouseInputBindings.matchesMouseButton(action.mapping, button)) {
                    return action;
                }
            }
            return null;
        }

        @Nullable
        private static ContainerAction fromMouseScroll(double scrollY) {
            for (ContainerAction action : values()) {
                if (KeyboardMouseInputBindings.matchesMouseScroll(action.mapping, scrollY)) {
                    return action;
                }
            }
            return null;
        }

        private boolean usesVanillaPhysicalMouseButton(int button) {
            return (this == PRIMARY && button == GLFW.GLFW_MOUSE_BUTTON_LEFT)
                || (this == ALTERNATE && button == GLFW.GLFW_MOUSE_BUTTON_RIGHT)
                || (this == QUICK_MOVE && button == GLFW.GLFW_MOUSE_BUTTON_LEFT);
        }
    }

    private static net.minecraft.client.input.MouseButtonEvent mouseEvent(double x, double y, int button) {
        return new net.minecraft.client.input.MouseButtonEvent(
            x, y, new net.minecraft.client.input.MouseButtonInfo(button, 0)
        );
    }

    private static final class UiPointerAction {
        private final KeyMapping mapping;
        private final int mouseButton;
        private final boolean enterFallback;
        private boolean wasDown;
        private boolean skipNextRelease;
        private boolean hasPressPosition;
        private boolean containerQuickCraft;
        private double pressX;
        private double pressY;
        private double dragX;
        private double dragY;
        private long lastClickTimeMs;
        @Nullable
        private Screen lastClickScreen;

        private UiPointerAction(KeyMapping mapping, int mouseButton, boolean enterFallback) {
            this.mapping = mapping;
            this.mouseButton = mouseButton;
            this.enterFallback = enterFallback;
        }

        private void sync() {
            this.wasDown = this.mapping.isDown();
            this.skipNextRelease = false;
            this.hasPressPosition = false;
            this.containerQuickCraft = false;
        }
    }

    private enum UiNavigationAction {
        UP(ModKeyBindings.UI_NAV_UP, GLFW.GLFW_KEY_UP),
        DOWN(ModKeyBindings.UI_NAV_DOWN, GLFW.GLFW_KEY_DOWN),
        LEFT(ModKeyBindings.UI_NAV_LEFT, GLFW.GLFW_KEY_LEFT),
        RIGHT(ModKeyBindings.UI_NAV_RIGHT, GLFW.GLFW_KEY_RIGHT);

        private final KeyMapping mapping;
        private final int keyCode;

        UiNavigationAction(KeyMapping mapping, int keyCode) {
            this.mapping = mapping;
            this.keyCode = keyCode;
        }

        private boolean matches(int keyCode, int scanCode) {
            return matchesKey(this.mapping, keyCode, scanCode);
        }

        @Nullable
        private static UiNavigationAction fromKey(int keyCode, int scanCode) {
            for (UiNavigationAction action : values()) {
                if (action.matches(keyCode, scanCode)) {
                    return action;
                }
            }
            return null;
        }
    }

    private static final class ActionState {
        private final UiPointerAction primaryPointer = new UiPointerAction(ModKeyBindings.UI_CONFIRM, 0, true);
        private final UiPointerAction alternatePointer = new UiPointerAction(ModKeyBindings.UI_ALTERNATE, 1, false);
        private final Map<Screen, Screen> screenParents = new WeakHashMap<>();
        private final Set<KeyMapping> screenHandledMappings = Collections.newSetFromMap(new IdentityHashMap<>());
        private final Map<Screen, Set<Integer>> screenHandledMouseButtons = new WeakHashMap<>();
        private final Set<Integer> suppressedScreenKeys = new HashSet<>();
        private final Set<Integer> suppressedScreenMouseButtons = new HashSet<>();
        private boolean wasDropDown;
        private boolean wasPauseButtonDown;
        private boolean dispatchingSyntheticKey;
        private boolean lastHadScreen;
        @Nullable
        private Screen suppressNextScreenTransitionSoundFrom;
        @Nullable
        private Screen lastTrackedInGameScreen;
        private BindingContext lastActionContext = BindingContext.GAMEPLAY;
        @Nullable
        private Screen navigationRepeatScreen;
        @Nullable
        private UiNavigationAction navigationRepeatAction;
        private boolean navigationRepeatKeyboardHeld;
        private long navigationRepeatStartedMs;
        private long navigationRepeatLastDispatchMs;
    }
}
