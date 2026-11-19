package net.jr.client.ui.navigation;

import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

public record FocusNavigationTarget(
    @Nullable Slot slot,
    @Nullable GuiEventListener widget,
    @Nullable ComponentPath path,
    ScreenRectangle rectangle
) {
    public static FocusNavigationTarget slot(Slot slot, ScreenRectangle rectangle) {
        return new FocusNavigationTarget(slot, null, null, rectangle);
    }

    public static FocusNavigationTarget widget(
        GuiEventListener widget,
        ComponentPath path,
        ScreenRectangle rectangle
    ) {
        return new FocusNavigationTarget(null, widget, path, rectangle);
    }
}
