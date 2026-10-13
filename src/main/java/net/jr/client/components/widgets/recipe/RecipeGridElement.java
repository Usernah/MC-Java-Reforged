package net.jr.client.components.widgets.recipe;

import net.jr.client.components.navigation.UiAction;
import net.jr.client.components.widgets.JavaBaseWidget;
import net.jr.client.components.widgets.compact.CompactGridElement;
import net.jr.client.components.widgets.compact.CompactGridEntry;
import net.jr.client.components.widgets.compact.CompactSlotElement;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class RecipeGridElement extends CompactGridElement<RecipeEntryView, RecipeVariantView> {
    private Consumer<RecipeVariantView> craft = ignored -> {};

    public RecipeGridElement(int x, int y, int width, int height, int visibleRows, int columns) {
        super(x, y, width, height, visibleRows, columns);
    }
    public void onCraft(Consumer<RecipeVariantView> craft) {
        this.craft = craft == null ? ignored -> {} : craft;
        this.onPrimarySelection(this.craft);
        this.onAlternateSelection(this.craft);
    }
    public void setRecipeGroups(List<RecipeEntryView> values) {
        List<RecipeEntryView> ordered = new ArrayList<>();
        if (values != null) {

            values.stream()
                    .filter(RecipeEntryView::hasCraftableVariant)
                    .forEach(ordered::add);
            values.stream()
                    .filter(group -> !group.hasCraftableVariant())
                    .forEach(ordered::add);
        }
        this.setGroups(ordered);
    }

    public RecipeVariantView previewedRecipe(boolean includeFocused) {
        return this.previewedValue(includeFocused);
    }

    public RecipeVariantView previewedRecipe(double mouseX, double mouseY, boolean includeFocused) {
        return this.previewedValue(mouseX, mouseY, includeFocused);
    }

    public RecipeSlotElement previewedRecipeSlot(
            double mouseX,
            double mouseY,
            boolean includeFocused
    ) {
        CompactSlotElement<RecipeEntryView, RecipeVariantView> slot =
                this.previewedSlotAt(mouseX, mouseY, includeFocused);
        return slot instanceof RecipeSlotElement recipeSlot ? recipeSlot : null;
    }

    @Override
    protected List<RecipeVariantView> variants(RecipeEntryView group) {
        return group.variants();
    }

    @Override
    protected RecipeVariantView displayedValue(RecipeEntryView group) {
        return group.displayedVariant();
    }

    @Override
    protected List<RecipeVariantView> expandedValues(RecipeEntryView group) {
        return group.expandedOrder();
    }

    @Override
    protected List<Object> groupKey(RecipeEntryView group) {
        return group.variants().stream()
                .flatMap(variant -> variant.alternativeRecipes().stream())
                .map(recipe -> (Object)recipe.id())
                .toList();
    }

    @Override
    protected Object valueKey(RecipeVariantView value) {
        return value.alternativeRecipes().stream()
                .map(RecipeDisplayEntry::id)
                .toList();
    }

    @Override
    protected CompactSlotElement<RecipeEntryView, RecipeVariantView> createCompactCell(
            CompactGridEntry<RecipeEntryView, RecipeVariantView> entry,
            int index,
            int width,
            int height,
            Consumer<CompactGridEntry<RecipeEntryView, RecipeVariantView>> primary,
            Consumer<CompactGridEntry<RecipeEntryView, RecipeVariantView>> alternate
    ) {
        return new RecipeSlotElement(
                width,
                height,
                entry,
                () -> this.isExpanded(entry.group()),
                primary,
                alternate,
                this::displayedValue
        );
    }

    public boolean previewedRecipeIsCraftable(boolean includeFocused) {
        for (JavaBaseWidget child : this.childWidgets()) {
            if (child instanceof RecipeSlotElement recipeSlot && child.isHovered()) {
                return recipeSlot.isCraftable();
            }
        }

        return includeFocused
                && this.getFocused() instanceof RecipeSlotElement recipeSlot
                && recipeSlot.isCraftable();
    }

    public RecipeSlotElement.ActivationFeedback mouseActivationFeedbackAt(
            double mouseX,
            double mouseY,
            int button
    ) {
        this.prepareInputLayout();

        if (!this.visible() || !this.active || !this.isMouseOver(mouseX, mouseY)) {
            return null;
        }

        for (JavaBaseWidget child : this.childWidgets()) {
            if (child instanceof RecipeSlotElement recipeSlot
                    && child.visible()
                    && child.isActive()
                    && child.isMouseOver(mouseX, mouseY)) {
                return recipeSlot.mouseActivationFeedback(button);
            }
        }
        return null;
    }

    public RecipeSlotElement.ActivationFeedback focusedActivationFeedback(UiAction action) {
        return this.getFocused() instanceof RecipeSlotElement recipeSlot
                ? recipeSlot.uiActivationFeedback(action)
                : null;
    }
}
