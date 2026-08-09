package net.jr.client.ui.hint.provider;

import net.jr.client.ui.hint.ControlHintContext;
import net.jr.client.ui.hint.model.ControlHintRequest;
import net.jr.client.input.binding.ModKeyBindings;
import net.jr.client.ui.container.actions.ContainerSlotFocusController;
import net.jr.client.ui.navigation.UiInputModeController;
import net.jr.mixin.controlhints.AbstractContainerScreenAccessor;
import net.jr.client.ui.container.actions.CreativeContainerActions;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class ContainerControlHintProvider implements ControlHintProvider {
    @Override
    public boolean supports(ControlHintContext context) {
        return context.containerScreen() != null;
    }

    @Override
    public List<ControlHintRequest> buildHints(ControlHintContext context) {
        AbstractContainerScreen<?> screen = context.containerScreen();
        if (screen == null) {
            return List.of();
        }

        List<ControlHintRequest> hints = new ArrayList<>();
        if (screen.getMenu() instanceof ChestMenu || screen.getMenu() instanceof ShulkerBoxMenu) {
            hints.add(new ControlHintRequest(ModKeyBindings.UI_TAKE_ALL, Component.literal("Tomar todo")));
            hints.add(new ControlHintRequest(ModKeyBindings.UI_STORE_ALL, Component.literal("Guardar todo")));
        }

        Slot targetSlot = targetSlot(screen);
        ItemStack carried = screen.getMenu().getCarried();
        if (!carried.isEmpty() && targetSlot != null) {
            boolean sameStack = targetSlot.getItem().isEmpty()
                || ItemStack.isSameItemSameComponents(carried, targetSlot.getItem());
            Component primaryLabel = Component.literal(sameStack ? "Colocar todo" : "Intercambiar");
            Component alternateLabel = Component.literal(sameStack ? "Colocar" : "Intercambiar");
            hints.add(new ControlHintRequest(ModKeyBindings.UI_CONFIRM, primaryLabel));
            hints.add(new ControlHintRequest(ModKeyBindings.UI_ALTERNATE, alternateLabel));
        } else if (targetSlot != null && targetSlot.hasItem()) {
            if (screen instanceof CreativeModeInventoryScreen) {
                if (CreativeContainerActions.isCreativePickerSlot(screen, targetSlot)
                        || CreativeContainerActions.isCreativeHotbarSlot(screen, targetSlot)) {
                    Component label = CreativeContainerActions.isCreativeHotbarSlot(screen, targetSlot)
                        ? Component.literal("Eliminar")
                        : Component.literal("Mover rapido");
                    hints.add(new ControlHintRequest(ModKeyBindings.UI_QUICK_MOVE, label));
                }
            } else {
                hints.add(new ControlHintRequest(ModKeyBindings.UI_CONFIRM, Component.literal("Tomar")));
                hints.add(new ControlHintRequest(ModKeyBindings.UI_ALTERNATE, Component.literal("Mitad")));
                hints.add(new ControlHintRequest(ModKeyBindings.UI_QUICK_MOVE, Component.literal("Mover rapido")));
            }
        }

        hints.add(new ControlHintRequest(ModKeyBindings.UI_BACK, Component.literal("Cerrar")));
        return hints;
    }

    private static Slot targetSlot(AbstractContainerScreen<?> screen) {
        if (UiInputModeController.isFocusNavigationActive()) {
            Integer focusedIndex = ContainerSlotFocusController.getFocusedSlotIndex(screen);
            if (focusedIndex != null && focusedIndex >= 0 && focusedIndex < screen.getMenu().slots.size()) {
                return screen.getMenu().slots.get(focusedIndex);
            }
        }
        return ((AbstractContainerScreenAccessor) screen).javareforged$getHoveredSlot();
    }
}
