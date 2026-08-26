package net.jr.client.ui.navigation;

import net.jr.Java_reforged;
import net.jr.client.sound.action.InputActionSounds;
import net.jr.client.ui.container.actions.ContainerSlotFocusController;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.jetbrains.annotations.Nullable;

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
            UiInputModeController.applyKeyboardNavigationInputType(event.getKeyCode());
            UiInputModeController.activateFocusNavigation();
        }
        if (UiInputModeController.isContainerSlotNavigationKey(event.getKeyCode())
            && screen instanceof AbstractContainerScreen<?>) {
            FocusSnapshot focusBefore = FocusSnapshot.capture(screen);
            boolean handled = UiInputModeController.applyContainerSlotFocusFallback(
                screen,
                event.getKeyCode()
            );
            event.setCanceled(true);
            if (handled && focusBefore.hasChanged(screen)) {
                InputActionSounds.playFocus();
            }
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
        if (UiInputModeController.applyContainerSlotFocusFallback(screen, event.getKeyCode())) {
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

    private record FocusSnapshot(
        @Nullable GuiEventListener focusedLeaf,
        @Nullable Integer focusedSlot
    ) {
        private static FocusSnapshot capture(Screen screen) {
            GuiEventListener focused = ScreenFocusController.resolveFocusedListener(screen);
            Integer slot = screen instanceof AbstractContainerScreen<?> containerScreen
                ? ContainerSlotFocusController.getFocusedSlotIndex(containerScreen)
                : null;
            return new FocusSnapshot(focused, slot);
        }

        private boolean hasChanged(Screen screen) {
            FocusSnapshot current = capture(screen);
            return focusedLeaf != current.focusedLeaf
                || !java.util.Objects.equals(focusedSlot, current.focusedSlot);
        }
    }
}
