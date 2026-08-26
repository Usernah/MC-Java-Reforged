package net.jr.client.input.runtime;

import net.jr.Java_reforged;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import net.jr.client.components.navigation.UiAction;
import net.jr.client.components.widgets.EditBoxElement;
import net.jr.client.components.widgets.recipe.RecipeSlotElement;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.context.SlotExecution;
import net.jr.client.runtime.context.SlotScope;
import net.jr.client.runtime.input.GamepadJoinTicker;
import net.jr.client.runtime.slot.LocalClientSlotRegistry;
import net.jr.client.runtime.context.LocalClientExecution;
import net.jr.client.input.InputApi;
import net.jr.client.input.binding.BindingContext;
import net.jr.client.input.binding.KeyboardMouseInputBindings;
import net.jr.client.input.binding.ModKeyBindings;
import net.jr.client.input.gamepad.GamepadDigitalInput;
import net.jr.client.sound.action.InputActionSounds;
import net.jr.client.sound.action.WidgetInputSounds;
import net.jr.client.ui.container.actions.ContainerSlotFocusController;
import net.jr.client.ui.container.actions.CreativeCatalogActionHost;
import net.jr.client.ui.container.slots.CustomSlotLayout;
import net.jr.client.ui.navigation.UiInputModeController;
import net.jr.client.ui.navigation.tabs.TabNavigationController;
import net.jr.client.ui.navigation.tabs.TabNavigationHost;
import net.jr.mixin.accessors.AbstractContainerScreenAccessor;
import net.jr.screens.controller.ControllerBindingsScreen;
import net.jr.screens.controller.ControllerMenuCaptureAware;
import net.jr.screens.crafting.AbstractJavaCraftingScreen;
import net.jr.screens.crafting.JavaCrafting2x2Screen;
import net.jr.screens.inventory.JavaCreativeInventoryScreen;
import net.minecraft.util.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
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
    private static final ActionState[] STATES = createStates();

    private MappedActionProcessor() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();

        GamepadJoinTicker.tick(minecraft);
        ClientRuntime runtime = ClientRuntime.INSTANCE;
        for (int slotId : runtime.viewports().presentedSlotIds()) {
            Runnable action = () -> processCurrentSlot(minecraft, InputApi.isGamepadConnected());
            if (runtime.clients().hasClient(slotId)) {
                LocalClientExecution.runForClient(slotId, action);
            } else {
                SlotExecution.runForSlot(minecraft, slotId, action);
            }
        }
    }

    public static boolean handleScreenMouseButton(Screen screen, int button) {
        return handleScreenMouseButton(
                screen,
                button,
                GamepadInputProcessor.cursorX(),
                GamepadInputProcessor.cursorY()
        );
    }

    public static boolean handleScreenMouseButton(
            Screen screen,
            int button,
            double mouseX,
            double mouseY
    ) {
        ActionState state = state();
        if (!(screen instanceof AbstractContainerScreen<?> containerScreen)
                || shouldBypassUiActionHandling(screen)) {
            return false;
        }
        return handleMouseContainerAction(state, containerScreen, button, mouseX, mouseY);
    }

    public static void playPhysicalMouseContainerActionFeedback(
            Screen screen,
            MouseButtonEvent event
    ) {
        if (!(screen instanceof AbstractContainerScreen<?> containerScreen)
                || shouldBypassUiActionHandling(screen)) {
            return;
        }

        ContainerAction action = ContainerAction.fromMouseButton(event.button());
        if (action == null || !action.usesVanillaPhysicalMouseButton(event.button())) {
            return;
        }

        playVanillaMouseContainerActionSound(
                containerScreen,
                action,
                event.x(),
                event.y()
        );
    }

    public static void rememberSuppressedScreenMouseButton(int button) {
        state().suppressedScreenMouseButtons.add(button);
    }

    public static void suppressNextScreenTransitionSound() {
        Screen screen = Minecraft.getInstance().gui.screen();
        if (screen != null) {
            state().suppressNextScreenTransitionSoundFrom = screen;
        }
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
            || isTextInputFocused(screen)) {
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
            || isTextInputFocused(screen)) {
            return false;
        }
        return handleMouseScrollContainerAction(state, containerScreen, scrollY);
    }

    public static boolean consumeHandledMouseRelease(Screen screen, int button) {
        return clearHandledMouseButton(state(), screen, button);
    }

    private static void processCurrentSlot(Minecraft minecraft, boolean gamepadInput) {
        ActionState state = state();
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
        syncInGameScreenSounds(minecraft, minecraft.gui.screen(), state);
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
        TabNavigationController tabNavigation = tabNavigationController(screen);
        if (tabNavigation != null) {
            KeyMapping handledMapping = tabNavigation.handleKey(
                    new net.minecraft.client.input.KeyEvent(keyCode, scanCode, 0)
            );
            if (handledMapping != null) {
                markHandledByInput(state, handledMapping);
                return true;
            }
        }

        if (matchesKey(ModKeyBindings.UI_BACK, keyCode, scanCode)) {
            if (GamepadInputProcessor.dispatchFocusedUiAction(screen, UiAction.BACK)) {
                GamepadInputProcessor.playFocusedUiActionFeedback(screen, UiAction.BACK);
            } else {
                closeScreenWithBackFeedback(state, screen);
            }
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
            boolean handledByFocusedElement = GamepadInputProcessor.activateFocusedElement(screen, 0);
            boolean handled = handledByFocusedElement;
            if (!handled && keyCode != GLFW.GLFW_KEY_ENTER) {
                state.dispatchingSyntheticKey = true;
                try {
                    handled = screen.keyPressed(new net.minecraft.client.input.KeyEvent(GLFW.GLFW_KEY_ENTER, 0, 0));
                } finally {
                    state.dispatchingSyntheticKey = false;
                }
            }
            if (handled) {
                if (handledByFocusedElement) {
                    GamepadInputProcessor.playFocusedUiActionFeedback(screen, UiAction.ACCEPT);
                }
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

    private static boolean handleMouseContainerAction(
            ActionState state,
            AbstractContainerScreen<?> screen,
            int button,
            double mouseX,
            double mouseY
    ) {
        if (isHandledMouseButton(state, screen, button)) {
            return true;
        }

        ContainerAction action = ContainerAction.fromMouseButton(button);
        if (action != null && action.usesVanillaPhysicalMouseButton(button)) {

            return false;
        }
        return action != null
            && rememberMouseButtonIfHandled(state, screen, button, executeContainerAction(state, screen, action));
    }

    private static void playVanillaMouseContainerActionSound(
            AbstractContainerScreen<?> screen,
            ContainerAction action,
            double mouseX,
            double mouseY
    ) {
        Slot cachedHoveredSlot =
                ((AbstractContainerScreenAccessor) screen)
                        .javaReforged$getHoveredSlot();
        Slot hoveredSlot = cachedHoveredSlot;
        if (screen instanceof CustomSlotLayout layout) {
            hoveredSlot = layout.slotLayer().getSlotAt(layout.slotGrid(), mouseX, mouseY);
        }

        boolean validVanillaSlot =
                hoveredSlot != null
                        && hoveredSlot.isActive()
                        && hoveredSlot.hasItem();

        if (action == ContainerAction.QUICK_MOVE) {
            if (validVanillaSlot) {
                InputActionSounds.playClick();
            } else {
                InputActionSounds.playBlock();
            }
            return;
        }

        if (screen instanceof AbstractJavaCraftingScreen<?> craftingScreen) {
            int mouseButton = action == ContainerAction.PRIMARY ? 0 : 1;
            RecipeSlotElement.ActivationFeedback feedback =
                    craftingScreen.recipeMouseActivationFeedbackAt(
                            mouseX,
                            mouseY,
                            mouseButton
                    );
            if (feedback != null) {
                playRecipeActivationFeedback(feedback);
                return;
            }
        }

        if (hoveredSlot != null && hoveredSlot.isActive()) {
            InputActionSounds.playClick();
        }
    }

    private static void playRecipeActivationFeedback(RecipeSlotElement.ActivationFeedback feedback) {
        switch (feedback) {
            case CRAFT -> InputActionSounds.playCraft();
            case BLOCK -> InputActionSounds.playBlock();
            case CLICK -> InputActionSounds.playClick();
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
            case QUICK_MOVE -> screen instanceof CreativeCatalogActionHost creativeHost
                    ? creativeHost.handleCreativeQuickMove()
                    : ContainerSlotFocusController.quickMoveFocusedOrHoveredSlot(screen);
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
        boolean controllerUiActive,
        @Nullable TabNavigationController tabNavigation
    ) {
        if (consumeTabNavigationMapping(tabNavigation, action.mapping)
                || consumeScreenHandledMapping(state, action.mapping)
                || !controllerUiActive) {
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
        TabNavigationController tabNavigation = tabNavigationController(screen);
        if (shouldBypassUiActionHandling(screen)) {
            if (tabNavigation != null) {
                tabNavigation.synchronize();
            }
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
            closeScreenWithBackFeedback(state, screen);
        }
        state.wasPauseButtonDown = pauseButtonDown;

        boolean controllerUiActive = controllerConnected && InputApi.isGamepadMode();
        boolean textInputFocused = isTextInputFocused(screen);

        if (tabNavigation != null
                && tabNavigation.process(controllerConnected, !textInputFocused)) {
            screen.afterKeyboardAction();
        }

        if (!consumeTabNavigationMapping(tabNavigation, ModKeyBindings.UI_BACK)
                && !consumeScreenHandledMapping(state, ModKeyBindings.UI_BACK)
                && controllerUiActive) {
            while (ModKeyBindings.UI_BACK.consumeClick()) {
                if (GamepadInputProcessor.dispatchFocusedUiAction(screen, UiAction.BACK)) {
                    GamepadInputProcessor.playFocusedUiActionFeedback(screen, UiAction.BACK);
                } else {
                    closeScreenWithBackFeedback(state, screen);
                }
            }
        }

        if (!textInputFocused) {
            handleNavigationPresses(state, screen, controllerUiActive, tabNavigation);
            tickNavigationRepeat(state, screen);

            if (screen instanceof AbstractContainerScreen<?> containerScreen) {
                refreshPointerSlotHover(screen);
                consumeMappedContainerAction(state, containerScreen, ContainerAction.TAKE_ALL, controllerUiActive, tabNavigation);
                consumeMappedContainerAction(state, containerScreen, ContainerAction.STORE_ALL, controllerUiActive, tabNavigation);
                consumeMappedContainerAction(state, containerScreen, ContainerAction.QUICK_MOVE, controllerUiActive, tabNavigation);
            }
        } else {
            clearNavigationRepeat(state);
        }

        handlePointerAction(state, screen, textInputFocused, state.primaryPointer, controllerUiActive, tabNavigation);
        handlePointerAction(state, screen, textInputFocused, state.alternatePointer, controllerUiActive, tabNavigation);
    }

    private static void handlePointerAction(
        ActionState state,
        Screen screen,
        boolean textInputFocused,
        UiPointerAction action,
        boolean controllerUiActive,
        @Nullable TabNavigationController tabNavigation
    ) {
        boolean down = action.mapping.isDown();
        if (consumeTabNavigationMapping(tabNavigation, action.mapping)
                || consumeScreenHandledMapping(state, action.mapping)
                || !shouldHandleSyntheticUiButtonActions(controllerUiActive)) {

            releaseActivePointerGesture(screen, action, true);
            action.wasDown = down;
            return;
        }

        if (down && !action.wasDown) {
            WidgetInputSounds.clearPointerClickFeedback();
            double mouseX = GamepadInputProcessor.cursorX();
            double mouseY = GamepadInputProcessor.cursorY();
            boolean handled = false;
            boolean handledByFocusedElement = false;
            action.skipNextRelease = false;
            action.hasPressPosition = false;
            action.containerQuickCraft = false;
            RecipeSlotElement.ActivationFeedback recipeFeedback = null;
            if (UiInputModeController.isFocusNavigationActive() && !textInputFocused) {
                boolean focusedContainerSlot =
                        hasFocusedContainerSlot(screen);

                GamepadInputProcessor.FocusedElementCenter focusCenter = null;

                if (focusedContainerSlot
                        && screen instanceof AbstractContainerScreen<?> containerScreen) {

                    ContainerSlotFocusController.FocusedSlotCenter slotCenter =
                            ContainerSlotFocusController.getFocusedSlotCenter(containerScreen);

                    if (slotCenter != null) {
                        mouseX = slotCenter.guiX();
                        mouseY = slotCenter.guiY();
                    }
                } else {
                    focusCenter =
                            GamepadInputProcessor.focusedElementCenter(screen);

                    if (focusCenter != null) {
                        mouseX = focusCenter.guiX();
                        mouseY = focusCenter.guiY();
                    }
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
                    boolean directFocusAction =
                        GamepadInputProcessor.focusedElementPrefersDirectAction(screen);

                    if (directFocusAction) {

                        if (screen instanceof AbstractJavaCraftingScreen<?> craftingScreen) {
                            UiAction uiAction = action.mouseButton == 0
                                    ? UiAction.ACCEPT
                                    : UiAction.ALTERNATE;
                            recipeFeedback = craftingScreen.focusedRecipeActivationFeedback(uiAction);
                        }
                        handledByFocusedElement = GamepadInputProcessor.activateFocusedElement(screen, action.mouseButton);
                        handled = handledByFocusedElement;
                    } else {

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

                        if (handled
                                && screen instanceof AbstractJavaCraftingScreen<?> craftingScreen) {
                            recipeFeedback = craftingScreen.recipeMouseActivationFeedbackAt(
                                    mouseX,
                                    mouseY,
                                    action.mouseButton
                            );
                        }
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
                if (handled
                        && screen instanceof AbstractJavaCraftingScreen<?> craftingScreen) {
                    recipeFeedback = craftingScreen.recipeMouseActivationFeedbackAt(
                            mouseX,
                            mouseY,
                            action.mouseButton
                    );
                }
            }

            if (handled) {

                state.suppressNextScreenTransitionSoundFrom = screen;
                action.pressX = mouseX;
                action.pressY = mouseY;

                action.dragX = mouseX;
                action.dragY = mouseY;

                action.hasPressPosition = true;

                action.draggingPointerWidget =
                        !handledByFocusedElement
                                && !UiInputModeController.isFocusNavigationActive();

                action.skipNextRelease = handledByFocusedElement;

                if (recipeFeedback != null) {
                    playRecipeActivationFeedback(recipeFeedback);
                } else if (handledByFocusedElement) {
                    GamepadInputProcessor.playFocusedUiActionFeedback(
                            screen,
                            action.mouseButton == 0 ? UiAction.ACCEPT : UiAction.ALTERNATE
                    );
                } else {
                    WidgetInputSounds.consumePointerClickFeedback()
                            .ifPresentOrElse(
                                    InputActionSounds::play,
                                    InputActionSounds::playClick
                            );
                }
            }
        } else if (down
                && action.wasDown
                && (action.containerQuickCraft
                || action.draggingPointerWidget)) {
            double dragX = GamepadInputProcessor.cursorX();
            double dragY = GamepadInputProcessor.cursorY();
            if (action.containerQuickCraft
                    && UiInputModeController.isFocusNavigationActive()) {

                GamepadInputProcessor.FocusedElementCenter focusCenter =
                        GamepadInputProcessor.focusedElementCenter(screen);

                if (focusCenter != null) {
                    dragX = focusCenter.guiX();
                    dragY = focusCenter.guiY();
                }
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
                double releaseX = action.containerQuickCraft || action.draggingPointerWidget
                    ? action.dragX
                    : action.hasPressPosition ? action.pressX : GamepadInputProcessor.cursorX();
                double releaseY = action.containerQuickCraft || action.draggingPointerWidget
                    ? action.dragY
                    : action.hasPressPosition ? action.pressY : GamepadInputProcessor.cursorY();
                screen.mouseReleased(mouseEvent(releaseX, releaseY, action.mouseButton));
            }
            action.hasPressPosition = false;
            action.containerQuickCraft = false;
            action.draggingPointerWidget = false;
        }
        action.wasDown = down;
    }

    private static void refreshPointerSlotHover(Screen screen) {
        if (!(screen instanceof CustomSlotLayout layout)
                || UiInputModeController.isFocusNavigationActive()) {
            return;
        }

        layout.slotLayer().updateHovered(
                layout.slotGrid(),
                GamepadInputProcessor.cursorX(),
                GamepadInputProcessor.cursorY()
        );
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
            // InputActionSounds.playHotbarFocus();
        }

        while (ModKeyBindings.GAMEPLAY_HOTBAR_NEXT.consumeClick()) {
            player.getInventory().setSelectedSlot((player.getInventory().getSelectedSlot() + 1) % 9);
            // InputActionSounds.playHotbarFocus();
        }

        while (ModKeyBindings.GAMEPLAY_CRAFTING.consumeClick()) {
            if (player.hasInfiniteMaterials()) {
                minecraft.gui.setScreen(new JavaCreativeInventoryScreen(
                        player,
                        Component.translatable("container.creative")
                ));
            } else {
                minecraft.gui.setScreen(new JavaCrafting2x2Screen(
                        player.inventoryMenu,
                        player.getInventory(),
                        Component.translatable("container.crafting")
                ));
            }
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
            && captureAware.javaReforged$isCapturingControllerBinding()) {
            return true;
        }
        return screen instanceof ControllerBindingsScreen bindingsScreen
            && bindingsScreen.selectedKey != null;
    }

    private static boolean isTextInputFocused(Screen screen) {
        if (screen == null) {
            return false;
        }
        Object focused = screen.getFocused();
        if (focused instanceof EditBox editBox) {
            return editBox.canConsumeInput();
        }
        return focused instanceof EditBoxElement editBox
            && editBox.canConsumeInput();
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

    private static void handleNavigationPresses(
            ActionState state,
            Screen screen,
            boolean controllerUiActive,
            @Nullable TabNavigationController tabNavigation
    ) {
        for (UiNavigationAction action : UiNavigationAction.values()) {
            if (consumeTabNavigationMapping(tabNavigation, action.mapping)
                    || consumeScreenHandledMapping(state, action.mapping)
                    || !controllerUiActive) {
                continue;
            }

            while (action.mapping.consumeClick()) {
                dispatchNavigationAction(state, screen, action, false, true);
            }
        }
    }

    private static void releasePointerBeforeFocusMove(Screen screen, UiPointerAction action) {
        if (!action.wasDown
            || !action.hasPressPosition
            || action.skipNextRelease) {
            return;
        }

        double releaseX = action.containerQuickCraft || action.draggingPointerWidget
            ? action.dragX
            : action.pressX;
        double releaseY = action.containerQuickCraft || action.draggingPointerWidget
            ? action.dragY
            : action.pressY;

        screen.mouseReleased(mouseEvent(releaseX, releaseY, action.mouseButton));

        action.hasPressPosition = false;
        action.containerQuickCraft = false;
        action.draggingPointerWidget = false;
        action.skipNextRelease = true;
    }

    private static void releaseActivePointerGesture(
        Screen screen,
        UiPointerAction action,
        boolean consumePhysicalRelease
    ) {
        if (!action.hasPressPosition || action.skipNextRelease) {
            return;
        }

        double releaseX = action.containerQuickCraft || action.draggingPointerWidget
            ? action.dragX
            : action.pressX;
        double releaseY = action.containerQuickCraft || action.draggingPointerWidget
            ? action.dragY
            : action.pressY;

        screen.mouseReleased(mouseEvent(releaseX, releaseY, action.mouseButton));
        action.hasPressPosition = false;
        action.containerQuickCraft = false;
        action.draggingPointerWidget = false;
        action.skipNextRelease = consumePhysicalRelease;
    }

    public static boolean hasActivePointerGesture() {
        ActionState state = state();
        return state.primaryPointer.hasActiveGesture()
            || state.alternatePointer.hasActiveGesture();
    }

    private static void dispatchNavigationAction(
        ActionState state,
        Screen screen,
        UiNavigationAction action,
        boolean keyboardHeld,
        boolean startRepeat
    ) {
        releasePointerBeforeFocusMove(screen, state.primaryPointer);
        releasePointerBeforeFocusMove(screen, state.alternatePointer);

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
        Integer slotId = SlotScope.idOrNull();
        return STATES[slotId != null ? slotId : 0];
    }

    private static void closeScreenWithBackFeedback(ActionState state, Screen screen) {
        state.suppressNextScreenTransitionSoundFrom = screen;
        screen.onClose();
        InputActionSounds.playBack();
    }

    @Nullable
    private static TabNavigationController tabNavigationController(Screen screen) {
        return screen instanceof TabNavigationHost host
                ? host.tabNavigationController()
                : null;
    }

    private static boolean consumeTabNavigationMapping(
            @Nullable TabNavigationController tabNavigation,
            KeyMapping mapping
    ) {
        if (tabNavigation == null || !tabNavigation.consumes(mapping)) {
            return false;
        }
        drainClickCounter(mapping);
        return true;
    }

    private static ActionState[] createStates() {
        ActionState[] states = new ActionState[LocalClientSlotRegistry.MAX_SLOTS];
        for (int slotId = 0; slotId < states.length; slotId++) {
            states[slotId] = new ActionState();
        }
        return states;
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
        private boolean draggingPointerWidget;
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
            this.draggingPointerWidget = false;
        }

        private boolean hasActiveGesture() {
            return this.hasPressPosition && !this.skipNextRelease;
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
