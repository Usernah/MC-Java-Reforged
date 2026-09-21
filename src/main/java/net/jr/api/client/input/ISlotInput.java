package net.jr.api.client.input;

import java.util.List;
import javax.annotation.Nullable;
import net.jr.api.client.input.InputMode;

/** Resolved input for one local player, independent of a temporary SlotScope. */
public interface ISlotInput extends IDigitalInput, IAnalogInput {
    int slotId();

    InputMode mode();

    List<IGamepad> gamepads();

    @Nullable IKeyboardMouse keyboardMouse();

    IScreenInput screen();

    boolean hasGamepad();

    boolean hasDisconnectedGamepad();
}
