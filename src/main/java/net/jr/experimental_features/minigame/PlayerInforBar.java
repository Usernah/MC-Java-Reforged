package net.jr.experimental_features.minigame;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.jr.api.client.render.gui.BottomHudRenderContext;
import net.jr.api.client.render.gui.BottomHudRenderObject;
import net.jr.api.client.resource.Asset;
import net.jr.client.components.elements.ImageElement;
import net.jr.experimental_features.minigame.client.BattleClientState;
import net.jr.experimental_features.minigame.network.BattleNetwork;
import net.minecraft.client.gui.contextualbar.ContextualBar;

public final class PlayerInforBar implements BottomHudRenderObject {
    private static final int MAX_PLAYERS = 8;
    private static final int ICON_WIDTH = 16;
    private static final int ICON_HEIGHT = 5;
    private static final int ICON_GAP = 8;
    private static final Asset ALIVE_TEXTURE = Asset.MOD("textures/experimental/minigame/player_icon_alive.png");
    private static final Asset DEATH_TEXTURE = Asset.MOD("textures/experimental/minigame/player_icon_death.png");
    private static final Asset ALIVE_LAYER = Asset.MOD("textures/experimental/minigame/player_icon_alive_layer.png");
    private static final Asset DEATH_LAYER = Asset.MOD("textures/experimental/minigame/player_icon_death_layer.png");

    private final ImageElement aliveIcon = new ImageElement(ALIVE_TEXTURE, 0, 0, ICON_WIDTH, ICON_HEIGHT, null);
    private final ImageElement deathIcon = new ImageElement(DEATH_TEXTURE, 0, 0, ICON_WIDTH, ICON_HEIGHT, null);
    private final ImageElement aliveLayer = new ImageElement(ALIVE_LAYER, 0, 0, ICON_WIDTH, ICON_HEIGHT, null);
    private final ImageElement deathLayer = new ImageElement(DEATH_LAYER, 0, 0, ICON_WIDTH, ICON_HEIGHT, null);

    @Override
    public void render(BottomHudRenderContext context) {
        if (!BattleClientState.isBattle()) {
            return;
        }
        List<BattleNetwork.PlayerBarState> players = new ArrayList<>(BattleClientState.players());
        players.sort(Comparator.comparingInt(player -> player.eliminated() ? 1 : 0));

        int playerCount = Math.min(players.size(), MAX_PLAYERS);
        if (playerCount == 0) {
            return;
        }

        int totalWidth = playerCount * ICON_WIDTH + (playerCount - 1) * ICON_GAP;
        int x = (context.viewport().guiWidth() - totalWidth) / 2;
        int y = context.availableHeight()
            - ContextualBar.MARGIN_BOTTOM
            - ContextualBar.HEIGHT;

        for (int index = 0; index < playerCount; index++) {
            BattleNetwork.PlayerBarState player = players.get(index);
            ImageElement icon = player.eliminated() ? this.deathIcon : this.aliveIcon;
            ImageElement icon_layer = player.eliminated() ? this.deathLayer : this.aliveLayer;
            icon.setBounds(x + index * (ICON_WIDTH + ICON_GAP), y, ICON_WIDTH, ICON_HEIGHT);
            icon_layer.setBounds(x + index * (ICON_WIDTH + ICON_GAP), y, ICON_WIDTH, ICON_HEIGHT);
            icon.setColor(0xFF000000 | player.color());
            icon.draw(context.graphics());
            icon_layer.draw(context.graphics());
        }
    }
}
