package net.jr.client.runtime.context;

/** Immutable identity captured when client-owned asynchronous work is scheduled. */
public record LocalClientToken(int slotId, long sessionId, long generation) {
    public LocalClientToken {
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
