package net.jr.client.ui.dsl;

import net.jr.client.components.widgets.Button;
import net.jr.client.ui.container.slots.ItemSlotGrid;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;


public final class UiDslRegistry {
    private static final Map<String, UiDslKind<?>> TYPES = new LinkedHashMap<>();

    static {
        register(Button.DSL);
        register(ItemSlotGrid.DSL);
    }

    private UiDslRegistry() {
    }

    public static synchronized <E extends DslElement> UiDslKind<E> register(UiDslKind<E> type) {
        Objects.requireNonNull(type, "type");
        UiDslKind<?> previous = TYPES.putIfAbsent(type.name(), type);
        if (previous != null && previous != type) {
            throw new IllegalArgumentException("DSL element type '" + type.name() + "' is already registered");
        }
        return type;
    }

    public static synchronized Optional<UiDslKind<?>> find(String name) {
        return Optional.ofNullable(TYPES.get(name));
    }
}
