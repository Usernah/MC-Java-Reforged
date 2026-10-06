package net.jr.client.runtime.render.state;

import net.jr.api.client.local.Client;

public final class HudStateRouter {
    private HudStateRouter() {
    }

    public static float vignetteBrightness() {
        return Client.current().render().hud().vignetteBrightness();
    }

    public static boolean setVignetteBrightness(float value) {
        Client.current().render().hud().setVignetteBrightness(value);
        return true;
    }

}
