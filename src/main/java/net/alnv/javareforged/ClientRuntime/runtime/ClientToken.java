package net.alnv.javareforged.ClientRuntime.runtime;

/** Immutable identity captured when client-owned asynchronous work is scheduled. */
public record ClientToken(int slotId, long sessionId, long generation) {
    public ClientToken {
        if (slotId < 0) {
            throw new IllegalArgumentException("slotId cannot be negative");
        }
        if (sessionId <= 0L) {
            throw new IllegalArgumentException("sessionId must be positive");
        }
        if (generation < 0L) {
            throw new IllegalArgumentException("generation cannot be negative");
        }
    }
}
