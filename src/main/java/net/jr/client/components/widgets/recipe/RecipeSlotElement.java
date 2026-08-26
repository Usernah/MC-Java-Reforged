package net.jr.client.components.widgets.recipe;

import net.jr.api.client.render.Draw;
import net.jr.api.client.resource.Asset;
import net.jr.client.components.navigation.UiAction;
import net.jr.client.components.widgets.compact.CompactGridEntry;
import net.jr.client.components.widgets.compact.CompactSlotElement;
import net.jr.client.render.item.ItemRenderOpacity;
import net.jr.client.ui.container.slots.SlotHighlightRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;

public class RecipeSlotElement extends CompactSlotElement<RecipeEntryView, RecipeVariantView> {
    public enum ActivationFeedback {
        CLICK,
        CRAFT,
        BLOCK
    }

    private static final Asset AVAILABLE = Asset.MC("textures/gui/sprites/container/crafting_menu/recipe_slot_available.png");
    private static final Asset UNAVAILABLE = Asset.MC("textures/gui/sprites/container/crafting_menu/recipe_slot_unavailable.png");
    private static final Asset VARIANT_AVAILABLE = Asset.MC("textures/gui/sprites/container/crafting_menu/recipe_variant_available.png");
    private static final Asset VARIANT_UNAVAILABLE = Asset.MC("textures/gui/sprites/container/crafting_menu/recipe_variant_unavailable.png");
    private static final Asset VARIANTS_CLOSED = Asset.MC("textures/gui/sprites/container/crafting_menu/recipe_variants_closed.png");
    private static final Asset VARIANTS_OPEN = Asset.MC("textures/gui/sprites/container/crafting_menu/recipe_variants_open.png");
    public boolean isCraftable() {
        RecipeVariantView recipe = this.displayedValue();

        return this.entry().expandedValue()
                ? recipe.craftable()
                : this.entry().group().hasCraftableVariant();
    }

    public ActivationFeedback mouseActivationFeedback(int button) {
        if (button == 0) {
            return this.primaryCrafts() ? this.craftFeedback() : ActivationFeedback.CLICK;
        }
        if (button == 1) {
            return this.craftFeedback();
        }
        return ActivationFeedback.CLICK;
    }

    public ActivationFeedback uiActivationFeedback(UiAction action) {
        if (action == UiAction.ACCEPT) {
            return this.primaryCrafts() ? this.craftFeedback() : ActivationFeedback.CLICK;
        }
        if (action == UiAction.ALTERNATE) {
            return this.craftFeedback();
        }
        return ActivationFeedback.CLICK;
    }

    private boolean primaryCrafts() {
        return this.entry().expandedValue() || !this.entry().group().hasVariants();
    }

    private ActivationFeedback craftFeedback() {
        return this.isCraftable() ? ActivationFeedback.CRAFT : ActivationFeedback.BLOCK;
    }

    RecipeSlotElement(
            int width,
            int height,
            CompactGridEntry<RecipeEntryView, RecipeVariantView> entry,
            BooleanSupplier expanded,
            Consumer<CompactGridEntry<RecipeEntryView, RecipeVariantView>> primary,
            Consumer<CompactGridEntry<RecipeEntryView, RecipeVariantView>> alternate,
            Function<? super RecipeEntryView, ? extends RecipeVariantView> displayedValue
    ) {
        super(width, height, entry, expanded, primary, alternate, displayedValue);
    }

    public RecipeVariantView displayedRecipe() {
        return this.displayedValue();
    }
    @Override protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractWidgetRenderState(graphics, mouseX, mouseY, partialTick);
        RecipeVariantView recipe = this.displayedValue();

        boolean craftable = this.entry().expandedValue()
                ? recipe.craftable()
                : this.entry().group().hasCraftableVariant();

        Asset background = this.entry().expandedValue()
                ? (craftable ? VARIANT_AVAILABLE : VARIANT_UNAVAILABLE)
                : (craftable ? AVAILABLE : UNAVAILABLE);
        Draw.imageFromMeta(background, this.x(), this.y(), this.width(), this.height()).draw(graphics);
        this.drawItem(graphics, recipe, craftable);
        if (!this.entry().expandedValue() && this.entry().group().hasVariants()) {
            Asset indicator = this.isGroupExpanded() ? VARIANTS_OPEN : VARIANTS_CLOSED;
            Draw.image(indicator, this.getX() + this.getWidth() - 6, this.getY() + this.getHeight() - 6, 5, 5).draw(graphics);
        }
        if (this.isFocused() || this.isHovered()) {
            SlotHighlightRenderer.draw(graphics, this.getX(), this.getY(), this.getWidth(), this.getHeight());
        }
    }

    private void drawItem(
            GuiGraphicsExtractor graphics,
            RecipeVariantView recipe,
            boolean craftable
    ) {
        if (recipe.icon().isEmpty()) return;

        int size = Math.max(1, Math.min(16, Math.min(this.getWidth(), this.getHeight()) - 2));
        float scale = size / 16.0F;

        graphics.pose().pushMatrix();
        graphics.pose().translate(
                this.getX() + (this.getWidth() - size) / 2.0F,
                this.getY() + (this.getHeight() - size) / 2.0F
        );
        graphics.pose().scale(scale, scale);

        ItemRenderOpacity.draw(
                craftable ? 1.0F : 0.3F,
                () -> graphics.item(recipe.icon(), 0, 0)
        );

        graphics.pose().popMatrix();
    }
}
