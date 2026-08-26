package net.jr.client.runtime.viewport;

import java.util.ArrayDeque;
import java.util.Deque;

public final class GuiScissorState {
    private final Deque<PreciseGuiScissor> preciseScissors = new ArrayDeque<>();

    public PreciseGuiScissor currentPreciseScissor() {
        return this.preciseScissors.peekLast();
    }

    public void pushPreciseScissor(PreciseGuiScissor scissor) {
        this.preciseScissors.addLast(scissor);
    }

    public void popPreciseScissor() {
        if (!this.preciseScissors.isEmpty()) {
            this.preciseScissors.removeLast();
        }
    }
}
