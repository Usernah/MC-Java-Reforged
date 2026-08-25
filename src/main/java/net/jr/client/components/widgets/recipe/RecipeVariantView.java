package net.jr.client.components.widgets.recipe;

import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;

public record RecipeVariantView(
        RecipeCollection collection,
        RecipeDisplayEntry recipe,
        ItemStack icon,
        boolean craftable
) {
    public RecipeVariantView {
        icon = icon == null ? ItemStack.EMPTY : icon;
    }
}
