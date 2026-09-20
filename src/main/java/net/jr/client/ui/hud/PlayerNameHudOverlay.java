package net.jr.client.ui.hud;

import net.jr.api.client.render.gui.HudRenderContext;
import net.jr.api.client.split.SplitScreen;
import net.jr.client.runtime.slot.LocalClientSlotRegistry;
import net.jr.client.components.elements.TextElement;
import net.jr.playerdata.PlayerProfileDataManager;
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

    public static void render(HudRenderContext context) {
        if (!SplitScreen.isActive()) {
            return;
        }
        LocalPlayer player = context.client().player();
        if (player == null) {
            return;
        }

        TextElement name = nameElement(context.slot().id());
        Component visualName = PlayerProfileDataManager.getSplitDisplayName(player);
        if (visualName == null) {
            visualName = player.getDisplayName();
        }
        name.setText(visualName);
        name.setPosition(context.viewport().guiWidth() - RIGHT_PADDING - name.width(), TOP_PADDING);
        name.draw(context.graphics());
    }

    private static TextElement nameElement(int slotId) {
        TextElement name = PLAYER_NAMES[slotId];
        if (name != null) {
            return name;
        }

        name = new TextElement(Component.empty(), 0, 0, null);
        name.setColor(TEXT_COLOR);
        name.setShadow(true);
        name.setShadowColor(OUTLINE_COLOR);
        PLAYER_NAMES[slotId] = name;
        return name;
    }
}
