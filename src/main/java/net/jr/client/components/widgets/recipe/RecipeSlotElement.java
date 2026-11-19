package net.jr.client.components.widgets.recipe;

import net.jr.api.client.render.Draw;
import net.jr.client.components.navigation.UiAction;
import net.jr.client.components.widgets.compact.CompactGridEntry;
import net.jr.client.components.widgets.compact.CompactSlotElement;
import net.jr.client.render.GuiGraphicsExtractorBridge;
import net.jr.client.render.item.ItemRenderOpacity;
import net.jr.client.ui.container.slots.ItemSlotHighlightRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;

public class RecipeSlotElement extends CompactSlotElement<RecipeEntryView, RecipeVariantView> {
    public enum ActivationFeedback {
        CLICK,
        CRAFT,
        BLOCK
    }

    private ActivationFeedback pendingActivationFeedback = ActivationFeedback.CLICK;
    private final RecipeSlotStyle style;

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

    ActivationFeedback consumeActivationFeedback() {
        ActivationFeedback feedback = this.pendingActivationFeedback;
        this.pendingActivationFeedback = ActivationFeedback.CLICK;
        return feedback;
    }

    RecipeSlotElement(
            int width,
            int height,
            CompactGridEntry<RecipeEntryView, RecipeVariantView> entry,
            BooleanSupplier expanded,
            Consumer<CompactGridEntry<RecipeEntryView, RecipeVariantView>> primary,
            Consumer<CompactGridEntry<RecipeEntryView, RecipeVariantView>> alternate,
            Function<? super RecipeEntryView, ? extends RecipeVariantView> displayedValue,
            RecipeSlotStyle style
    ) {
        super(width, height, entry, expanded, primary, alternate, displayedValue);
        this.style = style;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        this.pendingActivationFeedback = this.mouseActivationFeedback(event.button());
        boolean handled = super.mouseClicked(event, doubleClick);
        if (!handled) {
            this.pendingActivationFeedback = ActivationFeedback.CLICK;
        }
        return handled;
    }

    @Override
    public boolean handleUiAction(UiAction action) {
        this.pendingActivationFeedback = this.uiActivationFeedback(action);
        boolean handled = super.handleUiAction(action);
        if (!handled) {
            this.pendingActivationFeedback = ActivationFeedback.CLICK;
        }
        return handled;
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

        net.jr.api.client.resource.Asset background = this.entry().expandedValue()
                ? (craftable ? this.style.variantAvailable() : this.style.variantUnavailable())
                : (craftable ? this.style.available() : this.style.unavailable());
        Draw.imageFromMeta(background, this.x(), this.y(), this.width(), this.height()).draw(graphics);
        this.drawItem(graphics, recipe, craftable);
        if (!this.entry().expandedValue() && this.entry().group().hasVariants()) {
            net.jr.api.client.resource.Asset indicator = this.isGroupExpanded()
                    ? this.style.variantsOpen()
                    : this.style.variantsClosed();
            int indicatorSize = this.style.variantIndicatorSize();
            int inset = this.style.variantIndicatorInset();
            Draw.image(
                    indicator,
                    this.getX() + this.getWidth() - inset,
                    this.getY() + this.getHeight() - inset,
                    indicatorSize,
                    indicatorSize
            ).draw(graphics);
        }
        if (this.isFocused() || this.isHovered()) {
            ItemSlotHighlightRenderer.draw(graphics, this.getX(), this.getY(), this.getWidth(), this.getHeight());
        }
    }

    private void drawItem(
            GuiGraphicsExtractor graphics,
            RecipeVariantView recipe,
            boolean craftable
    ) {
        if (recipe.icon().isEmpty()) return;

        int size = Math.max(1, Math.min(
                this.style.maximumItemSize(),
                Math.min(this.getWidth(), this.getHeight()) - this.style.itemPadding()
        ));
        float scale = size / 16.0F;

        graphics.pose().pushMatrix();
        graphics.pose().translate(
                this.getX() + (this.getWidth() - size) / 2.0F,
                this.getY() + (this.getHeight() - size) / 2.0F
        );
        graphics.pose().scale(scale, scale);

        ItemRenderOpacity.draw(
                craftable ? 1.0F : this.style.unavailableItemAlpha(),
                () -> ((GuiGraphicsExtractorBridge)graphics).javaReforged$renderItemAtSize(
                        recipe.icon(),
                        0,
                        0,
                        0,
                        size,
                        false
                )
        );

        graphics.pose().popMatrix();
    }
}
