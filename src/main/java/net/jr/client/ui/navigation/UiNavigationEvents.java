package net.jr.client.ui.navigation;

import net.jr.Java_reforged;
import net.jr.client.ui.container.slots.CustomItemSlotLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = Java_reforged.MODID, value = Dist.CLIENT)
public final class UiNavigationEvents {
    private UiNavigationEvents() {
    }

    @SubscribeEvent
    public static void onScreenKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (!UiInputModeController.hasCurrentSlot()) {
            return;
        }
        Screen screen = UiInputModeController.currentEventScreen(event.getScreen());
        if (screen == null) {
            return;
        }
        if (UiInputModeController.isFocusNavigationKey(event.getKeyCode())) {
            if (UiInputModeController.prepareForFocusNavigation(event.getKeyCode())) {
                event.setCanceled(true);
                screen.afterKeyboardAction();
                return;
            }
        }
        if (UiInputModeController.isContainerSlotNavigationKey(event.getKeyCode())
            && screen instanceof AbstractContainerScreen<?>) {
            boolean handled = UiInputModeController.applyUnifiedContainerFocusNavigation(
                screen, event.getKeyCode()
            );
            if (!handled && CustomItemSlotLayout.active(screen) != null) {
                handled = UiInputModeController.applyContainerItemSlotFocusFallback(
                    screen, event.getKeyCode()
                );
            }
            event.setCanceled(true);
            if (handled) {
                screen.afterKeyboardAction();
            }
        }
    }

    @SubscribeEvent
    public static void onScreenKeyPressedPost(ScreenEvent.KeyPressed.Post event) {
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
        if (UiInputModeController.applyContainerItemSlotFocusFallback(screen, event.getKeyCode())) {
            screen.afterKeyboardAction();
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
