package net.jr.client.runtime.render.state;

import net.jr.client.runtime.client.LocalClient;
import net.jr.client.runtime.context.LocalClientScope;

public final class HudStateRouter {
    private HudStateRouter() {
    }

    public static float vignetteBrightness(float fallback) {
        LocalClient client = LocalClientScope.currentClientOrNull();
        return client != null ? client.render().hud().vignetteBrightness() : fallback;
    }

    public static boolean setVignetteBrightness(float value) {
        LocalClient client = LocalClientScope.currentClientOrNull();
        if (client == null) {
            return false;
        }
        client.render().hud().setVignetteBrightness(value);
        return true;
    }
}
