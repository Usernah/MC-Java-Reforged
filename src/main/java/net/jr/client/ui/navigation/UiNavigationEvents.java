package net.jr.client.ui.navigation;

import net.jr.Java_reforged;
import net.jr.client.sound.action.InputActionSounds;
import net.jr.client.ui.container.actions.ContainerSlotFocusController;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = Java_reforged.MODID, value = Dist.CLIENT)
public final class UiNavigationEvents {
    private static final ThreadLocal<InputActionSounds.FocusSnapshot> KEY_FOCUS = new ThreadLocal<>();

    private UiNavigationEvents() {
    }

    @SubscribeEvent
    public static void onScreenKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        KEY_FOCUS.remove();
        if (!UiInputModeController.hasCurrentSlot()) {
            return;
        }
        Screen screen = UiInputModeController.currentEventScreen(event.getScreen());
        if (screen == null) {
            return;
        }
        if (UiInputModeController.isFocusNavigationKey(event.getKeyCode())) {
            UiInputModeController.applyKeyboardNavigationInputType(event.getKeyCode());
            UiInputModeController.activateFocusNavigation();
            KEY_FOCUS.set(InputActionSounds.captureFocus(screen));
        }
        if (UiInputModeController.isContainerSlotNavigationKey(event.getKeyCode())
            && screen instanceof AbstractContainerScreen<?>) {
            InputActionSounds.FocusSnapshot focusBefore = KEY_FOCUS.get();
            KEY_FOCUS.remove();
            boolean handled = UiInputModeController.applyContainerSlotFocusFallback(
                screen,
                event.getKeyCode()
            );
            event.setCanceled(true);
            if (handled) {
                if (focusBefore != null) {
                    focusBefore.playIfChanged(screen);
                }
                screen.afterKeyboardAction();
            }
        }
    }

    @SubscribeEvent
    public static void onScreenKeyPressedPost(ScreenEvent.KeyPressed.Post event) {
        InputActionSounds.FocusSnapshot focusBefore = KEY_FOCUS.get();
        KEY_FOCUS.remove();
        if (!UiInputModeController.hasCurrentSlot()
            || !UiInputModeController.isFocusNavigationKey(event.getKeyCode())) {
            return;
        }

        Screen screen = UiInputModeController.currentEventScreen(event.getScreen());
        if (screen == null) {
            return;
        }
        if (screen instanceof AbstractContainerScreen<?>
            && UiInputModeController.isContainerSlotNavigationKey(event.getKeyCode())) {
            return;
        }
        if (UiInputModeController.applyContainerSlotFocusFallback(screen, event.getKeyCode())) {
            screen.afterKeyboardAction();
        }
        if (focusBefore != null) {
            focusBefore.playIfChanged(screen);
        }
    }

    @SubscribeEvent
    public static void onMouseButtonPressed(ScreenEvent.MouseButtonPressed.Pre event) {
        activatePointerModeFromPhysicalMouse();
    }

    @SubscribeEvent
    public static void onMouseButtonReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        activatePointerModeFromPhysicalMouse();
    }

    @SubscribeEvent
    public static void onMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        activatePointerModeFromPhysicalMouse();
    }

    private static void activatePointerModeFromPhysicalMouse() {
        if (UiInputModeController.hasCurrentSlot()
            && UiInputModeController.canPhysicalMouseDriveCurrentSlot()
            && UiInputModeController.isFocusNavigationActive()) {
            UiInputModeController.activatePointerModeFromMouseInteraction();
        }
    }

}
