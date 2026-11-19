package net.jr.api.client.input;

import javax.annotation.Nullable;


public interface IPreJoin {
    IGamepad gamepad();


    @Nullable
    Integer assignedSlot();
}
