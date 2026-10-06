package net.jr.api.client.input;

import javax.annotation.Nullable;

/** A connected controller that can represent a player before a world client exists. */
public interface IPreJoin {
    IGamepad gamepad();

    /**
     * Returns the secondary slot retained from the previous world, or {@code null}
     * when this controller is not restoring a local player.
     */
    @Nullable
    Integer assignedSlot();
}
