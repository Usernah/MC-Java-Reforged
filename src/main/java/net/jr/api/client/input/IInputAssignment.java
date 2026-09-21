package net.jr.api.client.input;

import java.util.List;
import javax.annotation.Nullable;

public interface IInputAssignment {
    @Nullable Integer assignedSlot(IGamepad gamepad);

    int effectiveSlot(IGamepad gamepad);

    boolean awaitingNeutral(IGamepad gamepad);

    List<IGamepad> gamepadsForSlot(int slotId);

    boolean hasGamepadForSlot(int slotId);

    boolean hasDisconnectedGamepadForSlot(int slotId);
}
