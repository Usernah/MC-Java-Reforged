package net.jr.api.client.render.gui;

import net.jr.client.runtime.client.LocalClient;

/** A local GUI viewport with a guaranteed local client. */
public interface HudRenderContext extends GuiRenderContext {
    LocalClient client();
}
