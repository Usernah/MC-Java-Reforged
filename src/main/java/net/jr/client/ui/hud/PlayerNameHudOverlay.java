package net.jr.client.ui.hud;

import java.util.HashMap;
import java.util.Map;
import net.jr.api.client.local.Client;
import net.jr.api.client.render.gui.HudRenderContext;
import net.jr.api.client.render.gui.HudRenderObject;
import net.jr.api.client.split.SplitScreen;
import net.jr.client.components.elements.TextElement;
import net.jr.playerdata.PlayerProfileDataManager;
import net.jr.registry.ModFonts;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

public final class PlayerNameHudOverlay implements HudRenderObject {
    private static final int TOP_PADDING = 14;
    private static final int RIGHT_PADDING = 10;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int SHADOW_COLOR = 0xFF191919;
    private final Map<Integer, TextElement> playerNames = new HashMap<>();

    public PlayerNameHudOverlay() {
    }

    @Override
    public void render(HudRenderContext context) {
        if (!SplitScreen.isActive()) {
            return;
        }
        if (!Client.get(0).options().showPlayerNameHud().get()) {
            return;
        }
        LocalPlayer player = Client.current().player();
        if (player == null) {
            return;
        }

        TextElement name = nameElement(Client.slotId());
        Component visualName = PlayerProfileDataManager.getSplitDisplayName(player);
        if (visualName == null) {
            visualName = player.getDisplayName();
        }
        name.setText(visualName);
        name.setPosition(
            context.graphics().guiWidth() - RIGHT_PADDING - name.width(),
            TOP_PADDING
        );
        name.draw(context.graphics());
    }

    private TextElement nameElement(int slotId) {
        return this.playerNames.computeIfAbsent(slotId, ignored -> {
            TextElement name = new TextElement(Component.empty(), 0, 0, null);
            name.setColor(TEXT_COLOR);
            name.setShadowColor(SHADOW_COLOR);
            name.setLegacyShadow(true);
            name.setScale(1F);
            name.setFont(ModFonts.MC_SEVEN);
            return name;
        });
    }
}
