package net.jr.screens.crafting;

import net.jr.client.render.item.ItemRenderOpacity;
import net.jr.client.render.GuiGraphicsExtractorBridge;
import net.jr.client.ui.container.slots.SlotGrid;
import net.jr.client.ui.container.slots.SlotGroup;
import net.jr.client.ui.container.slots.SlotPos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.recipebook.PlaceRecipeHelper;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;

import java.util.ArrayList;
import java.util.List;

final class CraftingRecipePreviewRenderer {
    private static final int MISSING_BACKGROUND = 0x66B82020;
    private static final int MISSING_TINT = 0x1AFF3030;
    private static final float MISSING_ITEM_ALPHA = 0.70F;

    private final int columns;
    private final int rows;
    private final SlotGroup craftingGridGroup;
    private final SlotGroup craftingResultGroup;

    CraftingRecipePreviewRenderer(
            int columns,
            int rows,
            SlotGroup craftingGridGroup,
            SlotGroup craftingResultGroup
    ) {
        this.columns = columns;
        this.rows = rows;
        this.craftingGridGroup = craftingGridGroup;
        this.craftingResultGroup = craftingResultGroup;
    }

    void render(
            GuiGraphicsExtractor graphics,
            RecipeDisplay display,
            ContextMap displayContext,
            SlotGrid slots,
            AbstractContainerMenu menu,
            Inventory inventory,
            boolean craftable
    ) {
        SlotDisplay[] ingredients = this.placeIngredients(display);
        if (ingredients == null) {
            return;
        }

        AvailableItems availableItems = AvailableItems.capture(inventory, menu, this.columns * this.rows);
        int cycle = Minecraft.getInstance().level == null
                ? 0
                : (int)(Minecraft.getInstance().level.getGameTime() / 30L);

        graphics.nextStratum();
        for (int index = 0; index < ingredients.length; index++) {
            SlotDisplay ingredient = ingredients[index];
            if (ingredient == null) {
                continue;
            }
            List<ItemStack> choices = ingredient.resolveForStacks(displayContext);
            ItemChoice choice = availableItems.chooseAndConsume(choices, cycle);
            if (choice.stack().isEmpty()) {
                continue;
            }
            this.renderIngredient(graphics, slots.get(this.craftingGridGroup, index), choice);
        }

        ItemStack resultStack = this.resultStack(display, displayContext);
        if (!resultStack.isEmpty()) {
            this.renderResult(
                    graphics,
                    slots.get(this.craftingResultGroup, 0),
                    resultStack,
                    craftable
            );
        }
    }

    ItemStack resultStack(RecipeDisplay display, ContextMap displayContext) {
        SlotDisplay result = resultOf(display);
        if (result == null) {
            return ItemStack.EMPTY;
        }
        int cycle = Minecraft.getInstance().level == null
                ? 0
                : (int)(Minecraft.getInstance().level.getGameTime() / 30L);
        return selectDisplayStack(result.resolveForStacks(displayContext), cycle);
    }

    private SlotDisplay[] placeIngredients(RecipeDisplay display) {
        SlotDisplay[] placed = new SlotDisplay[this.columns * this.rows];
        if (display instanceof ShapedCraftingRecipeDisplay shaped) {
            PlaceRecipeHelper.placeRecipe(
                    this.columns,
                    this.rows,
                    shaped.width(),
                    shaped.height(),
                    shaped.ingredients(),
                    (ingredient, slotIndex, x, y) -> placed[slotIndex] = ingredient
            );
            return placed;
        }
        if (display instanceof ShapelessCraftingRecipeDisplay shapeless) {
            int limit = Math.min(placed.length, shapeless.ingredients().size());
            for (int index = 0; index < limit; index++) {
                placed[index] = shapeless.ingredients().get(index);
            }
            return placed;
        }
        return null;
    }

    private void renderIngredient(GuiGraphicsExtractor graphics, SlotPos slot, ItemChoice choice) {
        if (slot == null) {
            return;
        }
        int itemSize = Math.min(16, slot.itemSize());
        int x = slot.x() + (slot.width() - itemSize) / 2;
        int y = slot.y() + (slot.height() - itemSize) / 2;
        if (!choice.available()) {
            graphics.fill(slot.x() + 1, slot.y() + 1, slot.x() + slot.width() - 1, slot.y() + slot.height() - 1, MISSING_BACKGROUND);
        }
        drawScaledItem(graphics, choice.stack(), x, y, itemSize, choice.available() ? 1.0F : MISSING_ITEM_ALPHA, false);
        if (!choice.available()) {
            graphics.fill(x, y, x + itemSize, y + itemSize, MISSING_TINT);
        }
    }

    private void renderResult(
            GuiGraphicsExtractor graphics,
            SlotPos slot,
            ItemStack result,
            boolean available
    ) {
        if (slot == null || result.isEmpty()) {
            return;
        }
        int itemSize = Math.min(24, slot.itemSize());
        int x = slot.x() + (slot.width() - itemSize) / 2;
        int y = slot.y() + (slot.height() - itemSize) / 2;
        if (!available) {
            graphics.fill(
                    slot.x() + 1,
                    slot.y() + 1,
                    slot.x() + slot.width() - 1,
                    slot.y() + slot.height() - 1,
                    MISSING_BACKGROUND
            );
        }
        drawScaledItem(
                graphics,
                result,
                x,
                y,
                itemSize,
                available ? 1.0F : MISSING_ITEM_ALPHA,
                true
        );
        if (!available) {
            graphics.fill(x, y, x + itemSize, y + itemSize, MISSING_TINT);
        }
    }

    private static void drawScaledItem(
            GuiGraphicsExtractor graphics,
            ItemStack stack,
            int x,
            int y,
            int size,
            float alpha,
            boolean decorations
    ) {
        float scale = size / 16.0F;
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        ItemRenderOpacity.draw(alpha, () ->
            ((GuiGraphicsExtractorBridge)graphics).javaReforged$renderItemStackAtSize(
                stack,
                0,
                0,
                0,
                size,
                false,
                decorations ? Minecraft.getInstance().font : null,
                null
            )
        );
        graphics.pose().popMatrix();
    }

    private static SlotDisplay resultOf(RecipeDisplay display) {
        if (display instanceof ShapedCraftingRecipeDisplay shaped) {
            return shaped.result();
        }
        if (display instanceof ShapelessCraftingRecipeDisplay shapeless) {
            return shapeless.result();
        }
        return null;
    }

    private static ItemStack selectDisplayStack(List<ItemStack> choices, int cycle) {
        if (choices.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return choices.get(Math.floorMod(cycle, choices.size()));
    }

    private record ItemChoice(ItemStack stack, boolean available) {}

    private static final class AvailableItems {
        private final List<ItemStack> stacks;

        private AvailableItems(List<ItemStack> stacks) {
            this.stacks = stacks;
        }

        static AvailableItems capture(Inventory inventory, AbstractContainerMenu menu, int craftingSlotCount) {
            List<ItemStack> stacks = new ArrayList<>();
            for (int index = 0; index < inventory.getContainerSize(); index++) {
                addCopy(stacks, inventory.getItem(index));
            }
            int limit = Math.min(craftingSlotCount, Math.max(0, menu.slots.size() - 1));
            for (int index = 0; index < limit; index++) {
                addCopy(stacks, menu.slots.get(index + 1).getItem());
            }
            return new AvailableItems(stacks);
        }

        ItemChoice chooseAndConsume(List<ItemStack> choices, int cycle) {
            if (choices.isEmpty()) {
                return new ItemChoice(ItemStack.EMPTY, false);
            }
            for (ItemStack choice : choices) {
                for (ItemStack available : this.stacks) {
                    if (!available.isEmpty() && ItemStack.isSameItemSameComponents(choice, available)) {
                        ItemStack shown = choice.copy();
                        available.shrink(1);
                        return new ItemChoice(shown, true);
                    }
                }
            }
            return new ItemChoice(selectDisplayStack(choices, cycle).copy(), false);
        }

        private static void addCopy(List<ItemStack> target, ItemStack stack) {
            if (!stack.isEmpty()) {
                target.add(stack.copy());
            }
        }
    }
}
