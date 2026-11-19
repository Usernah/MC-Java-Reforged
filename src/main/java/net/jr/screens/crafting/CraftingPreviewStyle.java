package net.jr.screens.crafting;

import net.jr.api.client.resource.Asset;

record CraftingPreviewStyle(
        int missingBackground,
        int missingTint,
        float missingItemAlpha,
        int backgroundInset,
        int cycleTicks,
        Asset missingOverlay,
        int missingOverlayX,
        int missingOverlayY,
        int missingOverlayWidth,
        int missingOverlayHeight
) {
}
