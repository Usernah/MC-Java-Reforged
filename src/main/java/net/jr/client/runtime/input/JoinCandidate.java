package net.jr.client.runtime.input;

import java.util.Objects;
import javax.annotation.Nullable;
import net.jr.api.client.input.IGamepad;
import net.jr.api.client.input.IPreJoin;

public record JoinCandidate(
    IGamepad gamepad,
    @Nullable Integer assignedSlot
) implements IPreJoin {
    public JoinCandidate {
        Objects.requireNonNull(gamepad, "gamepad");
    }
}
