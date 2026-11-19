package net.jr.client.runtime.input.binding;

import java.util.function.BooleanSupplier;
import net.jr.client.input.InputApi;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.state.OptionsState;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.neoforged.neoforge.client.settings.KeyMappingLookup;

public final class KeyMappingRouter {
    private KeyMappingRouter() {
    }

    public static boolean toggleMode(KeyMapping keyMapping, BooleanSupplier vanillaMode) {
        OptionsState options = ClientRuntime.INSTANCE
                .slots()
                .slot(InputApi.currentSlotId())
                .optionsState();
        return switch (keyMapping.getName()) {
            case "key.sneak" -> options.toggleCrouch().get();
            case "key.sprint" -> options.toggleSprint().get();
            case "key.attack" -> options.toggleAttack().get();
            case "key.use" -> options.toggleUse().get();
            default -> vanillaMode.getAsBoolean();
        };
    }

    public static void clickAll(KeyMappingLookup mappings, InputConstants.Key key) {
        for (KeyMapping keyMapping : mappings.getAll(key)) {
            if (keyMapping != null) {
                InputApi.click(keyMapping);
            }
        }
    }

    public static void setPhysicalDownAll(
        KeyMappingLookup mappings,
        InputConstants.Key key,
        boolean down
    ) {
        for (KeyMapping keyMapping : mappings.getAll(key)) {
            if (keyMapping != null) {
                InputApi.setPhysicallyDown(keyMapping, down);
            }
        }
    }
}
