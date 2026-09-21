package net.jr.client.ui.overlay;

import net.jr.client.components.elements.TextElement;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * Passive, per-viewport warning shown while a secondary player's assigned
 * controller is temporarily unavailable. It deliberately does not consume
 * input, so a reconnect can resume the existing slot immediately.
 */
public final class GamepadDisconnectedScreenOverlay implements VanillaScreenOverlay {
    private static final int PANEL_COLOR = 0xD92B0B0B;
    private static final int PANEL_BORDER_COLOR = 0xFFFF6B6B;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int HORIZONTAL_PADDING = 10;
    private static final int VERTICAL_PADDING = 6;

    private final TextElement message = new TextElement(
        Component.literal("Mando desconectado"),
        0.0F,
        0.0F,
        null
    );

    public GamepadDisconnectedScreenOverlay() {
        this.message.setColor(TEXT_COLOR);
        this.message.setShadow(true);
        this.message.setShadowColor(0xFF000000);
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, int width, int height, float partialTick) {
        if (width <= 0 || height <= 0) {
            return;
        }

        int textWidth = Math.round(this.message.width());
        int textHeight = Math.round(this.message.height());
        int panelWidth = Math.min(width - 8, textWidth + HORIZONTAL_PADDING * 2);
        int panelHeight = Math.min(height - 8, textHeight + VERTICAL_PADDING * 2);
        int left = Math.max(4, (width - panelWidth) / 2);
        int top = Math.max(4, (height - panelHeight) / 2);
        int right = Math.min(width - 4, left + panelWidth);
        int bottom = Math.min(height - 4, top + panelHeight);

        graphics.nextStratum();
        graphics.fill(left, top, right, bottom, PANEL_COLOR);
        graphics.fill(left, top, right, top + 1, PANEL_BORDER_COLOR);
        graphics.fill(left, bottom - 1, right, bottom, PANEL_BORDER_COLOR);
        graphics.fill(left, top + 1, left + 1, bottom - 1, PANEL_BORDER_COLOR);
        graphics.fill(right - 1, top + 1, right, bottom - 1, PANEL_BORDER_COLOR);
        this.message.setPosition(
            (width - this.message.width()) / 2.0F,
            top + (panelHeight - this.message.height()) / 2.0F
        );
        this.message.draw(graphics);
    }

    @Override
    public boolean blocksScreenInput() {
        return false;
    }

    @Override
    public boolean hidesCursor() {
        return false;
    }
}
