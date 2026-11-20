package net.jr.client.runtime.slot;

import java.util.function.Supplier;
import net.jr.api.client.render.gui.GuiRenderContext;
import net.jr.client.runtime.context.ExecutionPhase;
import net.jr.client.runtime.context.SlotExecution;
import net.jr.client.runtime.input.SlotCursorView;
import net.jr.client.runtime.render.world.WorldRenderView;
import net.jr.client.runtime.viewport.ViewportArea;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.jr.client.runtime.input.SlotInput;
import net.jr.client.runtime.state.ChatState;
import net.jr.client.runtime.state.ClientRenderState;
import net.jr.client.runtime.state.GameplayState;
import net.jr.client.runtime.state.InputState;
import net.jr.client.runtime.state.OptionsState;
import net.jr.client.runtime.state.PlayerModelHudState;
import net.jr.client.runtime.state.ScreenState;
import net.jr.client.runtime.state.ToastState;

public final class Slots {
    private final int id;
    private final ClientRenderState renderState = new ClientRenderState();
    private final ChatState chatState = new ChatState();
    private final GameplayState gameplayState = new GameplayState();
    private final ScreenState screenState = new ScreenState();
    private final InputState inputState = new InputState();
    private final SlotInput input;
    private final ToastState toastState = new ToastState();
    private final PlayerModelHudState playerModelHudState = new PlayerModelHudState();
    private final OptionsState optionsState;
    private final SlotCursorView cursor;

    Slots(int id) {
        this.id = id;
        this.input = new SlotInput(id);
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

    public SlotInput input() {
        return this.input;
    }

    public ToastState toastState() {
        return this.toastState;
    }

    public PlayerModelHudState playerModelHudState() {
        return this.playerModelHudState;
    }

    public OptionsState optionsState() {
        return this.optionsState;
    }

    public SlotCursorView cursor() {
        return this.cursor;
    }

    public boolean isPrimary() {
        return ClientSlots.isPrimary(this.id);
    }

    public boolean isSecondary() {
        return ClientSlots.isSecondary(this.id);
    }

    public void runForSlot(Runnable action) {
        SlotExecution.run(this, action);
    }

    public <T> T runForSlot(Supplier<T> action) {
        return SlotExecution.run(this, action);
    }

    public void runForSlot(ExecutionPhase phase, Runnable action) {
        SlotExecution.run(this, phase, action);
    }

    public void runForSlot(ViewportArea viewport, Runnable action) {
        SlotExecution.run(this, viewport, action);
    }

    public void runForSlot(WorldRenderView view, ExecutionPhase phase, Runnable action) {
        SlotExecution.run(this, view, phase, action);
    }

    public void runForSlot(GuiRenderContext context, ExecutionPhase phase, Runnable action) {
        SlotExecution.run(this, context, phase, action);
    }

    public void runForSlot(
        GuiGraphicsExtractor graphics,
        ViewportArea viewport,
        ExecutionPhase phase,
        Runnable action
    ) {
        SlotExecution.run(this, graphics, viewport, phase, action);
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
