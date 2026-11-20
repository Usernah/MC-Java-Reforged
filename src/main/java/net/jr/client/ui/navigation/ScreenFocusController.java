package net.jr.client.ui.navigation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

import net.jr.client.components.navigation.UiActionHandler;
import net.jr.client.runtime.input.GamepadInputHandler;
import net.jr.client.ui.container.actions.ContainerItemSlotFocusController;
import net.jr.client.ui.container.slots.CustomItemSlotLayout;
import net.jr.client.ui.cursor.MouseCoordinates;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenAxis;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

final class ScreenFocusController {
    private ScreenFocusController() {
    }

    static void completeFocusedContainerPath(@Nullable GuiEventListener listener) {
        if (listener instanceof AbstractContainerScreen<?> containerScreen
                && ContainerItemSlotFocusController.isVanillaItemSlotFocusActive(containerScreen)) {
            return;
        }

        GuiEventListener current = listener;
        while (current instanceof ContainerEventHandler container) {
            GuiEventListener focused = container.getFocused();
            if (focused == null) {
                ComponentPath initialPath = resolveInitialFocusPath(container);
                if (initialPath != null) {
                    initialPath.applyFocus(true);
                }
                return;
            }
            current = focused;
        }
    }

    static void clearFocusState(@Nullable Screen screen) {
        if (screen == null) {
            return;
        }
        CustomItemSlotLayout layout = CustomItemSlotLayout.active(screen);
        if (layout != null) {
            layout.clearFocusForPointer();
        }
        if (screen instanceof AbstractContainerScreen<?> containerScreen) {
            ContainerItemSlotFocusController.clearFocusedItemSlot(containerScreen);
        }
        clearNestedFocusState(screen);
        screen.clearFocus();
    }

    static boolean hasNavigationFocus(Screen screen) {
        return screen instanceof AbstractContainerScreen<?> containerScreen
                && ContainerItemSlotFocusController.isVanillaItemSlotFocusActive(containerScreen)
            || resolveFocusedListener(screen) != null;
    }

    static void initializeFocusFromPointer(Screen screen, Minecraft minecraft) {
        double mouseGuiX = resolveFocusSeedGuiX(minecraft);
        double mouseGuiY = resolveFocusSeedGuiY(minecraft);

        if (screen instanceof AbstractContainerScreen<?> containerScreen
            && CustomItemSlotLayout.active(screen) == null
            && initializeUnifiedContainerFocus(containerScreen, mouseGuiX, mouseGuiY)) {
            return;
        }

        if (screen instanceof AbstractContainerScreen<?> containerScreen
            && ContainerItemSlotFocusController.initializeFocusFromPointer(
                containerScreen,
                mouseGuiX,
                mouseGuiY
            )) {
            return;
        }
        initializeListenerFocusFromPointer(screen, mouseGuiX, mouseGuiY);
    }

    static boolean moveUnifiedContainerFocus(Screen screen, int keyCode) {
        if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) {
            return false;
        }

        if (!DirectionalFocusResolver.supportsKey(keyCode)) {
            return false;
        }
        List<FocusNavigationTarget> targets = resolveContainerFocusTargets(containerScreen);
        if (targets.isEmpty()) {
            return false;
        }

        FocusNavigationTarget origin = resolveFocusedTarget(containerScreen, targets);
        FocusNavigationTarget target = origin == null
            ? DirectionalFocusResolver.initialTarget(
                targets, FocusNavigationTarget::rectangle, keyCode
            )
            : DirectionalFocusResolver.nextTarget(
                targets, origin, FocusNavigationTarget::rectangle, keyCode
            );
        if (target == null && origin != null) {
            CustomItemSlotLayout layout = CustomItemSlotLayout.active(screen);
            if (origin.slot() != null && layout != null && layout.handleItemSlotNavigationBoundary(keyCode)) {
                return true;
            }
            List<FocusNavigationTarget> carouselTargets = screen instanceof FocusCarouselHost host
                ? host.focusCarouselTargets(origin, List.copyOf(targets), keyCode)
                : targets;
            target = DirectionalFocusResolver.wrapTarget(
                carouselTargets,
                origin,
                FocusNavigationTarget::rectangle,
                keyCode
            );
        }
        return target != null && applyContainerFocusTarget(containerScreen, target);
    }

    @Nullable
    static GuiEventListener resolveFocusedListener(Screen screen) {
        GuiEventListener focusedFromPath = extractFocusedLeaf(screen.getCurrentFocusPath());
        return focusedFromPath != null ? focusedFromPath : screen.getFocused();
    }

    private static void clearNestedFocusState(GuiEventListener listener) {
        if (!(listener instanceof ContainerEventHandler container)) {
            return;
        }
        for (GuiEventListener child : new ArrayList<>(container.children())) {
            clearNestedFocusState(child);
        }
        container.setFocused(null);
    }

    private static double resolveFocusSeedGuiX(Minecraft minecraft) {
        return GamepadInputHandler.isJoystickCursorActive()
            ? GamepadInputHandler.cursorX()
            : MouseCoordinates.rawMouseToGlobalGuiX(minecraft, minecraft.mouseHandler.xpos());
    }

    private static double resolveFocusSeedGuiY(Minecraft minecraft) {
        return GamepadInputHandler.isJoystickCursorActive()
            ? GamepadInputHandler.cursorY()
            : MouseCoordinates.rawMouseToGlobalGuiY(minecraft, minecraft.mouseHandler.ypos());
    }

    private static void initializeListenerFocusFromPointer(
        Screen screen,
        double mouseGuiX,
        double mouseGuiY
    ) {
        if (resolveFocusedListener(screen) != null) {
            return;
        }
        ComponentPath hoveredPath = findHoveredFocusPath(screen, mouseGuiX, mouseGuiY);
        if (hoveredPath != null) {
            applyFocusPath(screen, hoveredPath);
            return;
        }
        GuiEventListener target = findNearestListener(screen, mouseGuiX, mouseGuiY);
        if (target != null) {
            applyFocusPath(screen, buildInitialFocusPath(screen, target));
        }
    }

    private static boolean initializeUnifiedContainerFocus(
        AbstractContainerScreen<?> screen,
        double mouseGuiX,
        double mouseGuiY
    ) {
        List<FocusNavigationTarget> targets = resolveContainerFocusTargets(screen);
        FocusNavigationTarget nearest = targets.stream()
            .min(Comparator.comparingDouble(target -> squaredDistance(
                centerX(target.rectangle()), centerY(target.rectangle()), mouseGuiX, mouseGuiY
            )))
            .orElse(null);
        return nearest != null && applyContainerFocusTarget(screen, nearest);
    }

    private static List<FocusNavigationTarget> resolveContainerFocusTargets(
        AbstractContainerScreen<?> screen
    ) {
        List<FocusNavigationTarget> targets = new ArrayList<>();
        for (GuiEventListener listener : resolveTopLevelFocusableListeners(screen)) {
            ComponentPath childPath = resolveInitialFocusPath(listener);
            GuiEventListener focusedLeaf = extractFocusedLeaf(childPath);
            if (childPath == null || !isFocusableLeaf(focusedLeaf)) {
                continue;
            }
            ScreenRectangle rectangle = resolveListenerRectangle(focusedLeaf);
            if (!isRectangleEmpty(rectangle)) {
                targets.add(FocusNavigationTarget.widget(
                    focusedLeaf,
                    ComponentPath.path(screen, childPath),
                    rectangle
                ));
            }
        }
        for (ContainerItemSlotFocusController.ItemSlotFocusTarget slotTarget
                : ContainerItemSlotFocusController.focusTargets(screen)) {
            targets.add(FocusNavigationTarget.slot(slotTarget.slot(), slotTarget.rectangle()));
        }
        return targets;
    }

    @Nullable
    private static FocusNavigationTarget resolveFocusedTarget(
        AbstractContainerScreen<?> screen,
        List<FocusNavigationTarget> targets
    ) {
        Integer focusedSlot = ContainerItemSlotFocusController.getFocusedItemSlotIndex(screen);
        if (focusedSlot != null) {
            return targets.stream()
                .filter(target -> target.slot() != null && target.slot().index == focusedSlot)
                .findFirst()
                .orElse(null);
        }
        GuiEventListener focused = resolveFocusedListener(screen);
        return focused == null ? null : targets.stream()
            .filter(target -> target.widget() == focused)
            .findFirst()
            .orElse(null);
    }

    private static boolean applyContainerFocusTarget(
        AbstractContainerScreen<?> screen,
        FocusNavigationTarget target
    ) {
        if (target.slot() != null) {
            return ContainerItemSlotFocusController.focusItemSlot(screen, target.slot());
        }
        CustomItemSlotLayout layout = CustomItemSlotLayout.active(screen);
        if (layout != null) {
            ScreenRectangle rectangle = target.rectangle();
            double centerX = centerX(rectangle);
            double centerY = centerY(rectangle);
            if (layout.cursorMagnetTargetAt(centerX, centerY) != null
                    && layout.seedFocusFromPointer(centerX, centerY)) {
                return true;
            }
        }
        ContainerItemSlotFocusController.clearFocusedItemSlot(screen);
        applyFocusPath(screen, target.path());
        return true;
    }

    @Nullable
    private static ComponentPath findHoveredFocusPath(
        Screen screen,
        double mouseGuiX,
        double mouseGuiY
    ) {
        for (GuiEventListener listener : resolveTopLevelFocusableListeners(screen)) {
            ComponentPath childPath = findHoveredFocusPath(listener, mouseGuiX, mouseGuiY);
            if (childPath != null) {
                return ComponentPath.path(screen, childPath);
            }
        }
        return null;
    }

    @Nullable
    private static ComponentPath findHoveredFocusPath(
        GuiEventListener listener,
        double mouseGuiX,
        double mouseGuiY
    ) {
        if (!isFocusableListener(listener) || !listener.isMouseOver(mouseGuiX, mouseGuiY)) {
            return null;
        }
        if (listener instanceof ContainerEventHandler container) {
            var hoveredChild = container.getChildAt(mouseGuiX, mouseGuiY);
            if (hoveredChild.isPresent()) {
                ComponentPath childPath = findHoveredFocusPath(
                    hoveredChild.get(),
                    mouseGuiX,
                    mouseGuiY
                );
                if (childPath != null) {
                    return ComponentPath.path(container, childPath);
                }
            }
        }
        return resolveInitialFocusPath(listener);
    }

    @Nullable
    private static GuiEventListener findNearestListener(
        Screen screen,
        double mouseGuiX,
        double mouseGuiY
    ) {
        GuiEventListener bestListener = null;
        double bestDistance = Double.MAX_VALUE;
        for (GuiEventListener listener : resolveTopLevelFocusableListeners(screen)) {
            if (!isFocusableListener(listener)) {
                continue;
            }
            ScreenRectangle rectangle = resolveListenerRectangle(listener);
            if (isRectangleEmpty(rectangle)) {
                continue;
            }
            double centerX = rectangle.getCenterInAxis(ScreenAxis.HORIZONTAL);
            double centerY = rectangle.getCenterInAxis(ScreenAxis.VERTICAL);
            double distance = squaredDistance(centerX, centerY, mouseGuiX, mouseGuiY);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestListener = listener;
            }
        }
        return bestListener;
    }

    private static List<GuiEventListener> resolveTopLevelFocusableListeners(Screen screen) {
        LinkedHashSet<GuiEventListener> listeners = new LinkedHashSet<>();
        listeners.addAll(screen.children());
        return new ArrayList<>(listeners);
    }

    private static boolean isFocusableListener(GuiEventListener listener) {
        return resolveInitialFocusPath(listener) != null;
    }

    @Nullable
    private static ComponentPath resolveInitialFocusPath(GuiEventListener listener) {
        if (listener instanceof UiActionHandler handler
            && !handler.participatesInFocusNavigation()) {
            return null;
        }
        if (listener instanceof AbstractWidget widget && (!widget.visible || !widget.active)) {
            return null;
        }

        if (!(listener instanceof ContainerEventHandler container)) {
            ComponentPath initialPath = listener.nextFocusPath(
                new FocusNavigationEvent.InitialFocus()
            );
            return initialPath != null ? initialPath : ComponentPath.leaf(listener);
        }

        ComponentPath fallbackPath = null;
        for (GuiEventListener child : container.children()) {
            ComponentPath childPath = resolveInitialFocusPath(child);
            if (childPath != null) {
                fallbackPath = ComponentPath.path(container, childPath);
                break;
            }
        }
        if (fallbackPath == null) {
            return null;
        }

        ComponentPath initialPath = container.nextFocusPath(
            new FocusNavigationEvent.InitialFocus()
        );
        GuiEventListener focusedLeaf = extractFocusedLeaf(initialPath);
        return isFocusableLeaf(focusedLeaf) ? initialPath : fallbackPath;
    }

    private static boolean isFocusableLeaf(@Nullable GuiEventListener listener) {
        if (listener == null || listener instanceof ContainerEventHandler) {
            return false;
        }
        if (listener instanceof UiActionHandler handler
            && !handler.participatesInFocusNavigation()) {
            return false;
        }
        return !(listener instanceof AbstractWidget widget) || (widget.visible && widget.active);
    }

    private static ScreenRectangle resolveListenerRectangle(GuiEventListener listener) {
        if (listener instanceof AbstractWidget widget) {
            return new ScreenRectangle(
                widget.getX(),
                widget.getY(),
                widget.getWidth(),
                widget.getHeight()
            );
        }
        return listener.getRectangle();
    }

    private static boolean isRectangleEmpty(ScreenRectangle rectangle) {
        return rectangle.width() <= 0 || rectangle.height() <= 0;
    }

    @Nullable
    private static GuiEventListener extractFocusedLeaf(@Nullable ComponentPath path) {
        if (path == null) {
            return null;
        }
        if (path instanceof ComponentPath.Leaf leaf) {
            return leaf.component();
        }
        if (path instanceof ComponentPath.Path branch) {
            return extractFocusedLeaf(branch.childPath());
        }
        return path.component();
    }

    private static void applyFocusPath(Screen screen, @Nullable ComponentPath path) {
        if (path != null) {
            screen.clearFocus();
            path.applyFocus(true);
        }
    }

    @Nullable
    private static ComponentPath buildInitialFocusPath(
        Screen screen,
        GuiEventListener listener
    ) {
        ComponentPath childPath = resolveInitialFocusPath(listener);
        return childPath == null ? null : ComponentPath.path(screen, childPath);
    }

    private static double squaredDistance(double x1, double y1, double x2, double y2) {
        double deltaX = x1 - x2;
        double deltaY = y1 - y2;
        return deltaX * deltaX + deltaY * deltaY;
    }

    private static double centerX(ScreenRectangle rectangle) {
        return rectangle.getCenterInAxis(ScreenAxis.HORIZONTAL);
    }

    private static double centerY(ScreenRectangle rectangle) {
        return rectangle.getCenterInAxis(ScreenAxis.VERTICAL);
    }

}
