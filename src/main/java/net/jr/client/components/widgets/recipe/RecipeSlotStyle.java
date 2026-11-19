package net.jr.client.components.widgets.recipe;

import net.jr.api.client.resource.Asset;

public record RecipeSlotStyle(
        Asset available,
        Asset unavailable,
        Asset variantAvailable,
        Asset variantUnavailable,
        Asset variantsClosed,
        Asset variantsOpen,
        int variantIndicatorSize,
        int variantIndicatorInset,
        int maximumItemSize,
        int itemPadding,
        float unavailableItemAlpha
) {
}
