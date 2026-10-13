package net.jr.client.components.widgets.recipe;

import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * One distinct recipe result shown by the recipe grid.
 *
 * <p>A vanilla {@link RecipeCollection} may contain either actual result
 * variants (for example, the different wooden stairs) or several recipes for
 * the same result (for example, torches made with coal or charcoal). Those
 * alternative recipes belong to this view, rather than becoming visual
 * variants of their own.</p>
 */
public final class RecipeVariantView {
    private final List<Alternative> alternatives;

    public RecipeVariantView(
            RecipeCollection collection,
            RecipeDisplayEntry recipe,
            ItemStack icon,
            boolean craftable
    ) {
        this(List.of(new Alternative(collection, recipe, icon, craftable)));
    }

    private RecipeVariantView(List<Alternative> alternatives) {
        if (alternatives.isEmpty()) {
            throw new IllegalArgumentException("A recipe result must have at least one recipe");
        }
        this.alternatives = List.copyOf(alternatives);
    }

    public RecipeVariantView withAlternativesFrom(RecipeVariantView other) {
        ArrayList<Alternative> merged = new ArrayList<>(this.alternatives.size() + other.alternatives.size());
        merged.addAll(this.alternatives);
        merged.addAll(other.alternatives);
        return new RecipeVariantView(merged);
    }

    public boolean hasSameResult(ItemStack result) {
        return !result.isEmpty() && ItemStack.isSameItemSameComponents(this.icon(), result);
    }

    public RecipeCollection collection() {
        return this.selectedAlternative().collection();
    }

    public RecipeDisplayEntry recipe() {
        return this.selectedAlternative().recipe();
    }

    public ItemStack icon() {
        return this.selectedAlternative().icon();
    }

    public boolean craftable() {
        return this.alternatives.stream().anyMatch(Alternative::craftable);
    }

    public List<RecipeDisplayEntry> alternativeRecipes() {
        return this.alternatives.stream().map(Alternative::recipe).toList();
    }

    private Alternative selectedAlternative() {
        return this.alternatives.stream()
                .filter(Alternative::craftable)
                .findFirst()
                .orElse(this.alternatives.getFirst());
    }

    private record Alternative(
            RecipeCollection collection,
            RecipeDisplayEntry recipe,
            ItemStack icon,
            boolean craftable
    ) {
        private Alternative {
            icon = icon == null ? ItemStack.EMPTY : icon;
        }
    }
}
