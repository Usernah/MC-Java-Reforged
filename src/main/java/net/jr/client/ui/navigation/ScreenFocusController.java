package net.jr.client.ui.navigation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

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

        Direction direction = Direction.fromKey(keyCode);
        if (direction == null) {
            return false;
        }

        // Custom layouts may own transitions between logical regions (recipes, catalog,
        // inventory, etc.). Preserve those transitions while a slot owns focus, then use
        // the unified target list when the slot navigator reaches a real boundary.
        if (CustomItemSlotLayout.active(screen) != null
            && ContainerItemSlotFocusController.hasFocusedItemSlot(containerScreen)
            && ContainerItemSlotFocusController.moveFocus(containerScreen, keyCode)) {
            return true;
        }

        List<FocusTarget> targets = resolveContainerFocusTargets(containerScreen);
        if (targets.isEmpty()) {
            return false;
        }

        FocusTarget origin = resolveFocusedTarget(containerScreen, targets);
        FocusTarget target = origin == null
            ? initialTarget(targets, direction)
            : nextTarget(targets, origin, direction);
        if (target == null) {
            return false;
        }

        return applyContainerFocusTarget(containerScreen, target);
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
        List<FocusTarget> targets = resolveContainerFocusTargets(screen);
        FocusTarget nearest = targets.stream()
            .min(Comparator.comparingDouble(target -> squaredDistance(
                centerX(target.rectangle()), centerY(target.rectangle()), mouseGuiX, mouseGuiY
            )))
            .orElse(null);
        return nearest != null && applyContainerFocusTarget(screen, nearest);
    }

    private static List<FocusTarget> resolveContainerFocusTargets(
        AbstractContainerScreen<?> screen
    ) {
        List<FocusTarget> targets = new ArrayList<>();
        for (GuiEventListener listener : resolveTopLevelFocusableListeners(screen)) {
            ComponentPath childPath = resolveInitialFocusPath(listener);
            GuiEventListener focusedLeaf = extractFocusedLeaf(childPath);
            if (childPath == null || !isFocusableLeaf(focusedLeaf)) {
                continue;
            }
            ScreenRectangle rectangle = resolveListenerRectangle(focusedLeaf);
            if (!isRectangleEmpty(rectangle)) {
                targets.add(FocusTarget.widget(
                    focusedLeaf,
                    ComponentPath.path(screen, childPath),
                    rectangle
                ));
            }
        }
        for (ContainerItemSlotFocusController.ItemSlotFocusTarget slotTarget
                : ContainerItemSlotFocusController.focusTargets(screen)) {
            targets.add(FocusTarget.slot(slotTarget.slot(), slotTarget.rectangle()));
        }
        return targets;
    }

    @Nullable
    private static FocusTarget resolveFocusedTarget(
        AbstractContainerScreen<?> screen,
        List<FocusTarget> targets
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
            .filter(target -> target.listener() == focused)
            .findFirst()
            .orElse(null);
    }

    @Nullable
    private static FocusTarget initialTarget(List<FocusTarget> targets, Direction direction) {
        return targets.stream().min(Comparator
            .comparingDouble((FocusTarget target) -> direction.initialPrimary(target.rectangle()))
            .thenComparingDouble(target -> direction.initialSecondary(target.rectangle()))
        ).orElse(null);
    }

    @Nullable
    private static FocusTarget nextTarget(
        List<FocusTarget> targets,
        FocusTarget origin,
        Direction direction
    ) {
        Comparator<FocusTarget> straightComparator = Comparator
            .comparingDouble((FocusTarget target) -> direction.primaryDistance(
                target.rectangle(), origin.rectangle()
            ))
            .thenComparingDouble(target -> direction.secondaryDistance(
                target.rectangle(), origin.rectangle()
            ));

        FocusTarget straight = targets.stream()
            .filter(target -> target != origin
                && direction.accepts(target.rectangle(), origin.rectangle())
                && direction.overlapsAxis(target.rectangle(), origin.rectangle()))
            .min(straightComparator)
            .orElse(null);
        if (straight != null) {
            return straight;
        }

        return targets.stream()
            .filter(target -> target != origin
                && direction.accepts(target.rectangle(), origin.rectangle())
                && direction.isInsideCone(target.rectangle(), origin.rectangle()))
            .min(Comparator
                .comparingDouble((FocusTarget target) -> direction.distanceSquared(
                    target.rectangle(), origin.rectangle()
                ))
                .thenComparingDouble(target -> direction.angularDeviation(
                    target.rectangle(), origin.rectangle()
                ))
                .thenComparing(straightComparator))
            .orElse(null);
    }

    private static boolean applyContainerFocusTarget(
        AbstractContainerScreen<?> screen,
        FocusTarget target
    ) {
        if (target.slot() != null) {
            return ContainerItemSlotFocusController.focusItemSlot(screen, target.slot());
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

    private static double centerX(ScreenRectangle rectangle) {
        return rectangle.getCenterInAxis(ScreenAxis.HORIZONTAL);
    }

    private static double centerY(ScreenRectangle rectangle) {
        return rectangle.getCenterInAxis(ScreenAxis.VERTICAL);
    }

    private record FocusTarget(
        @Nullable Slot slot,
        @Nullable GuiEventListener listener,
        @Nullable ComponentPath path,
        ScreenRectangle rectangle
    ) {
        static FocusTarget slot(Slot slot, ScreenRectangle rectangle) {
            return new FocusTarget(slot, null, null, rectangle);
        }

        static FocusTarget widget(
            GuiEventListener listener,
            ComponentPath path,
            ScreenRectangle rectangle
        ) {
            return new FocusTarget(null, listener, path, rectangle);
        }
    }

    private enum Direction {
        UP,
        DOWN,
        LEFT,
        RIGHT;

        @Nullable
        static Direction fromKey(int keyCode) {
            return switch (keyCode) {
                case org.lwjgl.glfw.GLFW.GLFW_KEY_UP -> UP;
                case org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN -> DOWN;
                case org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT -> LEFT;
                case org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT -> RIGHT;
                default -> null;
            };
        }

        double initialPrimary(ScreenRectangle rectangle) {
            return switch (this) {
                case UP -> -centerY(rectangle);
                case DOWN -> centerY(rectangle);
                case LEFT -> -centerX(rectangle);
                case RIGHT -> centerX(rectangle);
            };
        }

        double initialSecondary(ScreenRectangle rectangle) {
            return isHorizontal() ? centerY(rectangle) : centerX(rectangle);
        }

        boolean accepts(ScreenRectangle candidate, ScreenRectangle origin) {
            return switch (this) {
                case UP -> centerY(candidate) < centerY(origin);
                case DOWN -> centerY(candidate) > centerY(origin);
                case LEFT -> centerX(candidate) < centerX(origin);
                case RIGHT -> centerX(candidate) > centerX(origin);
            };
        }

        boolean overlapsAxis(ScreenRectangle candidate, ScreenRectangle origin) {
            if (isHorizontal()) {
                return rangesOverlap(candidate.top(), candidate.top() + candidate.height(),
                    origin.top(), origin.top() + origin.height());
            }
            return rangesOverlap(candidate.left(), candidate.left() + candidate.width(),
                origin.left(), origin.left() + origin.width());
        }

        boolean isInsideCone(ScreenRectangle candidate, ScreenRectangle origin) {
            return secondaryDistance(candidate, origin) <= primaryDistance(candidate, origin);
        }

        double primaryDistance(ScreenRectangle candidate, ScreenRectangle origin) {
            return switch (this) {
                case UP -> centerY(origin) - centerY(candidate);
                case DOWN -> centerY(candidate) - centerY(origin);
                case LEFT -> centerX(origin) - centerX(candidate);
                case RIGHT -> centerX(candidate) - centerX(origin);
            };
        }

        double secondaryDistance(ScreenRectangle candidate, ScreenRectangle origin) {
            return isHorizontal()
                ? Math.abs(centerY(candidate) - centerY(origin))
                : Math.abs(centerX(candidate) - centerX(origin));
        }

        double distanceSquared(ScreenRectangle candidate, ScreenRectangle origin) {
            double primary = primaryDistance(candidate, origin);
            double secondary = secondaryDistance(candidate, origin);
            return primary * primary + secondary * secondary;
        }

        double angularDeviation(ScreenRectangle candidate, ScreenRectangle origin) {
            return secondaryDistance(candidate, origin) / primaryDistance(candidate, origin);
        }

        boolean isHorizontal() {
            return this == LEFT || this == RIGHT;
        }
    }

    private static boolean rangesOverlap(int aMin, int aMax, int bMin, int bMax) {
        return aMax > bMin && bMax > aMin;
    }
}
