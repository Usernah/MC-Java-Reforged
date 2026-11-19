package net.jr.client.components.widgets.item;

import net.jr.api.client.resource.Asset;

public record CreativeItemSlotStyle(Asset background, int itemPadding) {
    public CreativeItemSlotStyle {
        itemPadding = Math.max(0, itemPadding);
    }
}
