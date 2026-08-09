package net.alnv.javareforged.ClientRuntime.slot;

import net.alnv.javareforged.ClientRuntime.state.GameplayState;
import net.alnv.javareforged.ClientRuntime.state.InputState;
import net.alnv.javareforged.ClientRuntime.state.RenderState;
import net.alnv.javareforged.ClientRuntime.state.ScreenState;
import net.alnv.javareforged.ClientRuntime.viewport.ViewportArea;

public final class PlayerSlot {
    private final int id;
    private final RenderState renderState = new RenderState();
    private final GameplayState gameplayState = new GameplayState();
    private final ScreenState screenState = new ScreenState();
    private final InputState inputState = new InputState();
    private boolean connected;
    private boolean visible;
    private int viewportId;
    private ViewportArea viewport;

    PlayerSlot(int id) {
        this.id = id;
        this.connected = true;
        this.visible = true;
        this.viewportId = id;
    }

    void bindViewport(ViewportArea viewport) {
        this.viewport = viewport;
    }

    void clearViewport() {
        this.viewport = null;
    }

    public int id() {
        return this.id;
    }

    public RenderState renderState() {
        return this.renderState;
    }

    public GameplayState gameplayState() {
        return this.gameplayState;
    }

    public ScreenState screenState() {
        return this.screenState;
    }

    public InputState inputState() {
        return this.inputState;
    }

    public boolean connected() {
        return this.connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public boolean visible() {
        return this.visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public int viewportId() {
        return this.viewportId;
    }

    void setViewportId(int viewportId) {
        this.viewportId = viewportId;
    }

    public boolean hasViewport() {
        return this.viewport != null;
    }

    public ViewportArea viewport() {
        if (this.viewport == null) {
            throw new IllegalStateException("Player slot " + this.id + " has no resolved viewport");
        }
        return this.viewport;
    }

    public boolean drawable() {
        return this.connected && this.visible && this.viewport != null;
    }

    public void clearWorldBinding() {
        this.renderState.clearWorldBinding();
        this.gameplayState.clearWorldBinding();
        this.screenState.clear();
        this.inputState.clear();
    }
}
