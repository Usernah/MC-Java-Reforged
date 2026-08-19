package net.jr.client.runtime.slot;

import net.jr.client.input.SlotCursorView;
import net.jr.client.runtime.state.ChatState;
import net.jr.client.runtime.state.ClientRenderState;
import net.jr.client.runtime.state.GameplayState;
import net.jr.client.runtime.state.InputState;
import net.jr.client.runtime.state.OptionsState;
import net.jr.client.runtime.state.ScreenState;
import net.jr.client.runtime.state.ToastState;

public final class LocalClientSlot {
    private final int id;
    private final ClientRenderState renderState = new ClientRenderState();
    private final ChatState chatState = new ChatState();
    private final GameplayState gameplayState = new GameplayState();
    private final ScreenState screenState = new ScreenState();
    private final InputState inputState = new InputState();
    private final ToastState toastState = new ToastState();
    private final OptionsState optionsState;
    private final SlotCursorView cursor;

    LocalClientSlot(int id) {
        this.id = id;
        this.optionsState = new OptionsState(id);
        this.cursor = new SlotCursorView(id);
    }

    public int id() {
        return this.id;
    }

    public ClientRenderState renderState() {
        return this.renderState;
    }

    public ChatState chatState() {
        return this.chatState;
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

    public ToastState toastState() {
        return this.toastState;
    }

    public OptionsState optionsState() {
        return this.optionsState;
    }

    public SlotCursorView cursor() {
        return this.cursor;
    }

    public void clearWorldBinding() {
        this.renderState.clearWorldBinding();
        this.gameplayState.clearWorldBinding();
        this.screenState.clear();
        this.inputState.clear();
        this.chatState.clear();
        this.toastState.clear();
    }
}
