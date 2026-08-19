package net.jr.client.ui.hud;

import net.jr.client.runtime.client.LocalClient;
import net.jr.client.runtime.context.LocalClientScope;
import net.jr.client.runtime.viewport.ViewportGuiScale;
import net.jr.client.runtime.slot.LocalClientSlotRegistry;
import net.jr.client.components.elements.TextElement;
import net.jr.playerdata.PlayerProfileDataManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

public final class PlayerNameHudOverlay {
    private static final int TOP_PADDING = 10;
    private static final int RIGHT_PADDING = 10;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int OUTLINE_COLOR = 0xFF202020;
    private static final TextElement[] PLAYER_NAMES = new TextElement[LocalClientSlotRegistry.MAX_SLOTS];

    private PlayerNameHudOverlay() {
    }

    public static void render(GuiGraphicsExtractor graphics) {
        LocalClient client = LocalClientScope.currentClientOrNull();
        LocalPlayer player = client != null ? client.player() : null;
        Integer guiWidth = ViewportGuiScale.activeGuiWidthOrNull();
        if (player == null || guiWidth == null) {
            return;
        }

        TextElement name = nameElement(client.slotId());
        Component visualName = PlayerProfileDataManager.getSplitDisplayName(player);
        if (visualName == null) {
            visualName = player.getDisplayName();
        }
        name.setText(visualName);
        name.setPosition(guiWidth - RIGHT_PADDING - name.width(), TOP_PADDING);
        name.draw(graphics);
    }

    private static TextElement nameElement(int slotId) {
        TextElement name = PLAYER_NAMES[slotId];
        if (name != null) {
            return name;
        }

        name = new TextElement(Component.empty(), 0, 0, null);
        name.setColor(TEXT_COLOR);
        name.setOutline(OUTLINE_COLOR);
        PLAYER_NAMES[slotId] = name;
        return name;
    }
}
