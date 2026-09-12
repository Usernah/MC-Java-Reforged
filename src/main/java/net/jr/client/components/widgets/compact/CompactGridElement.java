package net.jr.client.components.widgets.compact;

import net.jr.client.components.widgets.JavaBaseWidget;
import net.jr.client.components.widgets.item.AbstractItemGridElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public abstract class CompactGridElement<G, V> extends AbstractItemGridElement<CompactGridEntry<G, V>> {
    private final List<G> groups = new ArrayList<>();
    private G expandedGroup;
    private Consumer<V> primarySelection = ignored -> {};
    private Consumer<V> alternateSelection = ignored -> {};

    protected CompactGridElement(int x, int y, int width, int height, int visibleRows, int columns) {
        super(x, y, width, height, visibleRows, columns);
    }

    public final void onPrimarySelection(Consumer<V> selection) {
        this.primarySelection = selection == null ? ignored -> {} : selection;
    }

    public final void onAlternateSelection(Consumer<V> selection) {
        this.alternateSelection = selection == null ? ignored -> {} : selection;
    }

    public final void setGroups(List<G> values) {
        GridSelection previousSelection = this.captureSelection();
        Object expandedKey = this.expandedGroup == null ? null : this.groupKey(this.expandedGroup);

        this.groups.clear();
        if (values != null) {
            this.groups.addAll(values);
        }

        this.expandedGroup = expandedKey == null ? null : this.findGroup(expandedKey);
        if (this.expandedGroup != null && !this.hasVariants(this.expandedGroup)) {
            this.expandedGroup = null;
        }

        this.rebuildEntries(-1);
        this.restoreSelection(previousSelection);
    }

    public final boolean isExpanded(G group) {
        return this.expandedGroup == group;
    }

    public final boolean closeExpansion() {
        if (this.expandedGroup == null) {
            return false;
        }
        G closing = this.expandedGroup;
        int baseIndex = this.baseIndex(closing);
        this.expandedGroup = null;
        this.rebuildEntries(baseIndex);
        return true;
    }

    public final V previewedValue(boolean includeFocused) {
        CompactSlotElement<G, V> slot = this.hoveredOrFocusedSlot(includeFocused);
        return slot == null ? null : slot.displayedValue();
    }

    public final V previewedValue(double mouseX, double mouseY, boolean includeFocused) {
        CompactSlotElement<G, V> slot = this.previewedSlotAt(mouseX, mouseY, includeFocused);
        return slot == null ? null : slot.displayedValue();
    }

    @SuppressWarnings("unchecked")
    protected final CompactSlotElement<G, V> previewedSlotAt(
            double mouseX,
            double mouseY,
            boolean includeFocused
    ) {
        JavaBaseWidget cell = this.previewedCellAt(mouseX, mouseY, includeFocused);
        return cell instanceof CompactSlotElement<?, ?> slot
                ? (CompactSlotElement<G, V>)slot
                : null;
    }

    protected final CompactGridEntry<G, V> previewedEntry(boolean includeFocused) {
        CompactSlotElement<G, V> slot = this.hoveredOrFocusedSlot(includeFocused);
        return slot == null ? null : slot.entry();
    }

    protected abstract List<V> variants(G group);

    protected V displayedValue(G group) {
        List<V> values = this.variants(group);
        if (values.isEmpty()) {
            throw new IllegalStateException("A compact grid group must contain at least one value");
        }
        return values.getFirst();
    }

    protected List<V> expandedValues(G group) {
        return this.variants(group);
    }

    protected abstract Object groupKey(G group);

    protected abstract Object valueKey(V value);

    protected abstract CompactSlotElement<G, V> createCompactCell(
            CompactGridEntry<G, V> entry,
            int index,
            int width,
            int height,
            Consumer<CompactGridEntry<G, V>> primary,
            Consumer<CompactGridEntry<G, V>> alternate
    );

    @Override
    protected final CompactSlotElement<G, V> createCell(
            CompactGridEntry<G, V> entry,
            int index,
            int width,
            int height
    ) {
        return this.createCompactCell(
                entry,
                index,
                width,
                height,
                this::activatePrimary,
                this::activateAlternate
        );
    }

    private void activatePrimary(CompactGridEntry<G, V> entry) {
        if (entry.expandedValue() || !this.hasVariants(entry.group())) {
            this.primarySelection.accept(entry.displayedValue(this::displayedValue));
        } else {
            this.toggleExpansion(entry.group());
        }
    }

    private void activateAlternate(CompactGridEntry<G, V> entry) {
        this.alternateSelection.accept(entry.displayedValue(this::displayedValue));
    }

    private boolean hasVariants(G group) {
        return this.variants(group).size() > 1;
    }

    private void toggleExpansion(G group) {
        if (this.expandedGroup == group) {
            this.closeExpansion();
            return;
        }
        this.expandedGroup = group;
        this.rebuildEntries(this.baseIndex(group) + 1);
    }

    private int baseIndex(G target) {
        int index = 0;
        for (G group : this.groups) {
            if (group == target) {
                return index;
            }
            index++;
            if (group == this.expandedGroup) {
                index += this.expandedValues(group).size();
            }
        }
        return Math.max(0, index - 1);
    }

    private void rebuildEntries(int focusIndex) {
        List<CompactGridEntry<G, V>> flattened = new ArrayList<>();
        for (G group : this.groups) {
            flattened.add(CompactGridEntry.base(group));
            if (group == this.expandedGroup) {
                for (V value : this.expandedValues(group)) {
                    flattened.add(CompactGridEntry.expanded(group, value));
                }
            }
        }

        if (focusIndex >= 0) {
            this.setEntriesAndFocus(flattened, focusIndex);
        } else {
            this.setEntries(flattened);
        }
    }

    private GridSelection captureSelection() {
        int focusedIndex = this.focusedEntryIndex();
        List<CompactGridEntry<G, V>> currentEntries = this.entries();
        if (focusedIndex < 0 || focusedIndex >= currentEntries.size()) {
            return null;
        }

        CompactGridEntry<G, V> focusedEntry = currentEntries.get(focusedIndex);
        return new GridSelection(
                this.groupKey(focusedEntry.group()),
                focusedEntry.fixedValue() == null ? null : this.valueKey(focusedEntry.fixedValue()),
                focusedEntry.expandedValue()
        );
    }

    private void restoreSelection(GridSelection selection) {
        if (selection == null) {
            return;
        }

        List<CompactGridEntry<G, V>> currentEntries = this.entries();
        for (int index = 0; index < currentEntries.size(); index++) {
            CompactGridEntry<G, V> entry = currentEntries.get(index);
            if (!Objects.equals(this.groupKey(entry.group()), selection.groupKey())
                    || entry.expandedValue() != selection.expandedValue()) {
                continue;
            }

            Object entryValueKey = entry.fixedValue() == null ? null : this.valueKey(entry.fixedValue());
            if (Objects.equals(entryValueKey, selection.valueKey())) {
                this.focusEntry(index);
                return;
            }
        }

        G survivingGroup = this.findGroup(selection.groupKey());
        if (survivingGroup != null) {
            this.focusEntry(this.baseIndex(survivingGroup));
        }
    }

    private G findGroup(Object key) {
        for (G group : this.groups) {
            if (Objects.equals(this.groupKey(group), key)) {
                return group;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private CompactSlotElement<G, V> hoveredOrFocusedSlot(boolean includeFocused) {
        for (var child : this.childWidgets()) {
            if (child instanceof CompactSlotElement<?, ?> slot && child.isHovered()) {
                return (CompactSlotElement<G, V>) slot;
            }
        }
        if (includeFocused && this.getFocused() instanceof CompactSlotElement<?, ?> slot) {
            return (CompactSlotElement<G, V>) slot;
        }
        return null;
    }

    private record GridSelection(Object groupKey, Object valueKey, boolean expandedValue) {}
}
