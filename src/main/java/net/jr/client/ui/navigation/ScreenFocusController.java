package net.jr.client.ui.navigation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import net.jr.client.runtime.input.LocalGamepadInputHandler;
import net.jr.client.ui.container.actions.ContainerSlotFocusController;
import net.jr.client.ui.container.slots.CustomSlotLayout;
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
import org.jetbrains.annotations.Nullable;

final class ScreenFocusController {
    private ScreenFocusController() {
    }

    static void completeFocusedContainerPath(@Nullable GuiEventListener listener) {
        if (listener instanceof AbstractContainerScreen<?> containerScreen
                && ContainerSlotFocusController.isVanillaSlotFocusActive(containerScreen)) {
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
        if (screen instanceof CustomSlotLayout layout) {
            layout.clearFocusForPointer();
        }
        if (screen instanceof AbstractContainerScreen<?> containerScreen) {
            ContainerSlotFocusController.clearFocusedSlot(containerScreen);
        }
        clearNestedFocusState(screen);
        screen.clearFocus();
    }

    static boolean hasNavigationFocus(Screen screen) {
        return screen instanceof AbstractContainerScreen<?> containerScreen
                && ContainerSlotFocusController.isVanillaSlotFocusActive(containerScreen)
            || resolveFocusedListener(screen) != null;
    }

    static void initializeFocusFromPointer(Screen screen, Minecraft minecraft) {
        double mouseGuiX = resolveFocusSeedGuiX(minecraft);
        double mouseGuiY = resolveFocusSeedGuiY(minecraft);
        if (screen instanceof AbstractContainerScreen<?> containerScreen
            && ContainerSlotFocusController.initializeFocusFromPointer(
                containerScreen,
                mouseGuiX,
                mouseGuiY
            )) {
            return;
        }
        initializeListenerFocusFromPointer(screen, mouseGuiX, mouseGuiY);
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
        return LocalGamepadInputHandler.isJoystickCursorActive()
            ? LocalGamepadInputHandler.cursorX()
            : MouseCoordinates.rawMouseToGlobalGuiX(minecraft, minecraft.mouseHandler.xpos());
    }

    private static double resolveFocusSeedGuiY(Minecraft minecraft) {
        return LocalGamepadInputHandler.isJoystickCursorActive()
            ? LocalGamepadInputHandler.cursorY()
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
}
