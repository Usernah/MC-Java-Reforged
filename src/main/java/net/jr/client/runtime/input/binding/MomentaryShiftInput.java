package net.jr.client.runtime.input.binding;

import net.jr.api.client.local.Client;
import net.jr.client.input.InputApi;
import net.jr.client.runtime.client.ClientInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Input;

public final class MomentaryShiftInput {
    private MomentaryShiftInput() {
    }

    public static boolean isActive() {
        LocalPlayer player = playerOrNull();
        return player != null && (player.isPassenger() || player.getAbilities().flying);
    }

    public static boolean isPhysicalDown() {
        ClientInstance client = Client.currentOrNull();
        return client != null
            && InputApi.isPhysicallyDown(Minecraft.getInstance().options.keyShift);
    }

    public static Input project(Input input) {
        if (!isActive()) {
            return input;
        }
        return new Input(
            input.forward(),
            input.backward(),
            input.left(),
            input.right(),
            input.jump(),
            isPhysicalDown(),
            input.sprint()
        );
    }

    private static LocalPlayer playerOrNull() {
        ClientInstance client = Client.currentOrNull();
        return client != null ? client.player() : null;
    }
}
