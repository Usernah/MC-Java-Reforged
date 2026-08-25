package net.jr.client.components.widgets.compact;

import net.jr.client.components.navigation.UiAction;
import net.jr.client.components.widgets.JavaBaseWidget;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class CompactSlotElement<G, V> extends JavaBaseWidget {
    private final CompactGridEntry<G, V> entry;
    private final BooleanSupplier expanded;
    private final Consumer<CompactGridEntry<G, V>> primary;
    private final Consumer<CompactGridEntry<G, V>> alternate;
    private final Function<? super G, ? extends V> displayedValue;

    protected CompactSlotElement(
            int width,
            int height,
            CompactGridEntry<G, V> entry,
            BooleanSupplier expanded,
            Consumer<CompactGridEntry<G, V>> primary,
            Consumer<CompactGridEntry<G, V>> alternate,
            Function<? super G, ? extends V> displayedValue
    ) {
        super(0, 0, width, height, Component.empty());
        this.entry = entry;
        this.expanded = expanded;
        this.primary = primary;
        this.alternate = alternate;
        this.displayedValue = displayedValue;
    }

    protected final CompactGridEntry<G, V> entry() {
        return this.entry;
    }

    protected final V displayedValue() {
        return this.entry.displayedValue(this.displayedValue);
    }

    protected final boolean isGroupExpanded() {
        return this.expanded.getAsBoolean();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        boolean inside = event.x() >= this.getX()
                && event.y() >= this.getY()
                && event.x() < this.getRight()
                && event.y() < this.getBottom();
        if (!this.active || !this.visible || !inside || (event.button() != 0 && event.button() != 1)) {
            return false;
        }

        this.setPressed(true);
        if (event.button() == 0) {
            this.primary.accept(this.entry);
        } else {
            this.alternate.accept(this.entry);
        }
        return true;
    }

    @Override
    public boolean handleUiAction(UiAction action) {
        if (!this.active || !this.visible) {
            return false;
        }
        if (action == UiAction.ACCEPT) {
            this.primary.accept(this.entry);
            return true;
        }
        if (action == UiAction.ALTERNATE) {
            this.alternate.accept(this.entry);
            return true;
        }
        return false;
    }

    @Override
    public boolean prefersDirectFocusAction() {
        return true;
    }
}
