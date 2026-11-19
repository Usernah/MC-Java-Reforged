package net.jr.client.runtime.input;

import net.jr.api.client.input.IGamepad;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.user.UserManager;
import net.jr.client.user.UserProfile;
import net.minecraft.client.Minecraft;

public final class UserJoinService {
    private UserJoinService() {
    }

    public static int select(
        Minecraft minecraft,
        IGamepad gamepad,
        UserProfile user
    ) {
        ClientRuntime runtime = ClientRuntime.INSTANCE;
        Integer activeSlot = UserManager.get().activeSlot(user.id());
        if (activeSlot != null) {
            runtime.input().mutableAssignments().unassignSlot(activeSlot);
            runtime.input().mutableAssignments().assign(gamepad, activeSlot);
            runtime.slots().slot(activeSlot).inputState().markGamepadInput();
            UserManager.get().markUsed(user);
            return activeSlot;
        }
        return runtime.input().join().join(minecraft, gamepad, user);
    }
}
