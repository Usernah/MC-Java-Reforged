package net.jr.client.runtime.render.experimental;

import net.jr.ClientConfig;
import net.jr.Java_reforged;
import net.jr.bootstrap.OptionalModPresence;
import com.mojang.blaze3d.systems.RenderSystem;

public final class ExperimentalRenderController {
    private static volatile boolean requestedEnabled;
    private static boolean initialized;
    private static volatile boolean enabled;

    private ExperimentalRenderController() {
    }

    public static void requestEnabled(boolean requested) {
        requestedEnabled = requested;
    }

    public static boolean enabled() {
        return enabled;
    }

    public static void beginFrame() {
        RenderSystem.assertOnRenderThread();
        requestedEnabled = ClientConfig.experimentalRenderOptimizations()
            && !OptionalModPresence.sodiumLoaded();
        if (!initialized) {
            initialized = true;
            enabled = requestedEnabled;
        } else if (requestedEnabled != enabled) {
            enabled = requestedEnabled;
            Java_reforged.LOGGER.info("Native terrain MDI changed to {}", enabled ? "ON" : "OFF");
            if (!enabled) {
                TerrainMultiDraw.close();
            }
        }
    }

}
