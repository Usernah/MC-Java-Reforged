package net.jr.client.runtime.input;

import java.util.Objects;
import net.jr.api.client.input.IGamepad;
import net.jr.api.client.input.IPreJoin;

public record LocalJoinCandidate(IGamepad gamepad) implements IPreJoin {
    public LocalJoinCandidate {
        Objects.requireNonNull(gamepad, "gamepad");
    }
}
