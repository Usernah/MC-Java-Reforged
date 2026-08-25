package net.jr.client.components.widgets.compact;

import java.util.function.Function;

public record CompactGridEntry<G, V>(G group, V fixedValue, boolean expandedValue) {
    public static <G, V> CompactGridEntry<G, V> base(G group) {
        return new CompactGridEntry<>(group, null, false);
    }

    public static <G, V> CompactGridEntry<G, V> expanded(G group, V value) {
        return new CompactGridEntry<>(group, value, true);
    }

    public V displayedValue(Function<? super G, ? extends V> displayedValue) {
        return this.fixedValue != null ? this.fixedValue : displayedValue.apply(this.group);
    }
}
