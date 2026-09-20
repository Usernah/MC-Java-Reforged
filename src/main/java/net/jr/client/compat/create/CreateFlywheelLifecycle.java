package net.jr.client.compat.create;

import java.lang.reflect.Method;
import net.jr.Java_reforged;
import net.jr.client.runtime.client.LocalClient;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.fml.ModList;

/** Cleans optional Create/Flywheel state before a local world is detached. */
public final class CreateFlywheelLifecycle {
    private static volatile boolean resetLookupAttempted;
    private static volatile Method resetMethod;

    private CreateFlywheelLifecycle() {
    }

    public static void release(LocalClient client) {
        if (client == null) {
            return;
        }

        ClientLevel level = client.level();
        CreateRuntimeState.clear(client);
        if (level == null || !ModList.get().isLoaded("create_flywheel")) {
            return;
        }

        Method reset = resetMethod();
        if (reset == null) {
            return;
        }
        try {
            reset.invoke(null, level);
        } catch (ReflectiveOperationException error) {
            Java_reforged.LOGGER.warn("Could not release Flywheel state for a detached local world", error);
        }
    }

    private static Method resetMethod() {
        if (resetLookupAttempted) {
            return resetMethod;
        }
        synchronized (CreateFlywheelLifecycle.class) {
            if (resetLookupAttempted) {
                return resetMethod;
            }
            try {
                Class<?> manager = Class.forName(
                    "dev.engine_room.create_flywheel.impl.visualization.VisualizationManagerImpl",
                    false,
                    CreateFlywheelLifecycle.class.getClassLoader()
                );
                resetMethod = manager.getMethod("reset", LevelAccessor.class);
            } catch (ClassNotFoundException | NoSuchMethodException error) {
                Java_reforged.LOGGER.warn("Flywheel local-world cleanup is unavailable", error);
            } finally {
                resetLookupAttempted = true;
            }
            return resetMethod;
        }
    }
}
