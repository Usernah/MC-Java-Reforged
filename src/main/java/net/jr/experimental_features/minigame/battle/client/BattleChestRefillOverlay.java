package net.jr.experimental_features.minigame.battle.client;

import net.jr.api.client.render.gui.GlobalRenderContext;
import net.jr.api.client.render.gui.IGlobalRenderObject;
import net.jr.api.client.resource.Asset;
import net.jr.client.components.elements.ImageElement;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.client.LocalClientReadinessPolicy;
import net.jr.experimental_features.minigame.client.BattleClientState;

/** One window-wide animation, regardless of how many local viewports are active. */
public final class BattleChestRefillOverlay implements IGlobalRenderObject {
    private static final int SIZE = 32 * 2;
    private static final int MARGIN = 10;
    private static final Asset ICON = Asset.MOD("textures/experimental/minigame/chest_fill.png");
    private final ImageElement image = new ImageElement(ICON, 0, 0, SIZE, SIZE, null);

    public BattleChestRefillOverlay() {
        this.image.setUseMeta(true);
    }

    @Override
    public void render(GlobalRenderContext context) {
        if (!BattleClientState.showChestRefill()) {
            return;
        }
        long localPlayers = ClientRuntime.INSTANCE.clients().all().stream()
            .filter(LocalClientReadinessPolicy::worldReady)
            .count();
        if (localPlayers == 0) {
            return;
        }
        int x = localPlayers > 1 ? (context.guiWidth() - SIZE) / 2 : context.guiWidth() - SIZE - MARGIN;
        int y = localPlayers > 1 ? (context.guiHeight() - SIZE) / 2 : MARGIN;
        this.image.setBounds(x, y, SIZE, SIZE);
        this.image.setAnimationStartMillis(BattleClientState.chestRefillStartedAtMs());
        this.image.draw(context.graphics());
    }
}
