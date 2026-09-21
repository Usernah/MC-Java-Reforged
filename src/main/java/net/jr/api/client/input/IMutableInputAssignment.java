package net.jr.api.client.input;

public interface IMutableInputAssignment extends IInputAssignment {
    void assign(IGamepad gamepad, int slotId);

    void unassign(IGamepad gamepad);

    void unassignSlot(int slotId);

    void pruneDisconnectedDevices();

    void clearDisconnectedAssignments();

    void reset();
}
