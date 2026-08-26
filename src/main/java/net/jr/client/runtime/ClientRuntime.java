package net.jr.client.runtime;

import net.jr.ClientConfig;
import net.jr.client.runtime.client.LocalClientManager;
import net.jr.client.runtime.input.InputFocus;
import net.jr.client.runtime.minimap.MinimapEngine;
import net.jr.client.runtime.network.ConnectionSlotRegistry;
import net.jr.client.runtime.slot.LocalClientSlotRegistry;
import net.jr.client.runtime.options.PlayerOptionsStorage;
import net.jr.client.runtime.viewport.ViewportManager;
import net.jr.client.runtime.viewport.ViewportResizeHandler;

public final class ClientRuntime {
    public static final ClientRuntime INSTANCE = new ClientRuntime();

    private final LocalClientSlotRegistry slots = new LocalClientSlotRegistry();
    private final PlayerOptionsStorage playerOptions = new PlayerOptionsStorage(this.slots);
    private final ConnectionSlotRegistry connections = new ConnectionSlotRegistry();
    private final ViewportManager viewports = new ViewportManager(this.slots);
    private final LocalClientManager clients = new LocalClientManager(this.slots, this.connections);
    private final MinimapEngine minimap = new MinimapEngine();
    private final InputFocus inputFocus = new InputFocus(LocalClientSlotRegistry.MAX_SLOTS);
    private final ViewportResizeHandler viewportResize = new ViewportResizeHandler(this.viewports);

    private ClientRuntime() {
        this.viewports.setTwoPlayerOrientation(ClientConfig.splitOrientation());
    }

    public LocalClientSlotRegistry slots() {
        return this.slots;
    }

    public PlayerOptionsStorage playerOptions() {
        return this.playerOptions;
    }

    public LocalClientManager clients() {
        return this.clients;
    }

    public ViewportManager viewports() {
        return this.viewports;
    }

    public ViewportResizeHandler viewportResize() {
        return this.viewportResize;
    }

    public ConnectionSlotRegistry connections() {
        return this.connections;
    }

    public MinimapEngine minimap() {
        return this.minimap;
    }

    public InputFocus inputFocus() {
        return this.inputFocus;
    }

}
