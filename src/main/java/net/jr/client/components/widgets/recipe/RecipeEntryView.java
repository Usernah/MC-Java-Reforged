package net.jr.client.components.widgets.recipe;

import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import java.util.List;

public record RecipeEntryView(
    RecipeCollection collection,
    List<RecipeVariantView> variants
) {
    public RecipeEntryView {
        variants = List.copyOf(variants);
    }

    public boolean hasVariants() { return this.variants.size() > 1; }

    public boolean hasCraftableVariant() {
        return this.variants.stream().anyMatch(RecipeVariantView::craftable);
    }

    public RecipeVariantView displayedVariant() {
        if (this.variants.isEmpty()) throw new IllegalStateException("A recipe entry must have at least one variant");
        int index = (int)((System.currentTimeMillis() / 1000L) % this.variants.size());
        return this.variants.get(index);
    }

    public List<RecipeVariantView> expandedOrder() {
        java.util.ArrayList<RecipeVariantView> ordered = new java.util.ArrayList<>(this.variants.size());
        this.variants.stream().filter(RecipeVariantView::craftable).forEach(ordered::add);
        this.variants.stream().filter(variant -> !variant.craftable()).forEach(ordered::add);
        return List.copyOf(ordered);
    }
}
