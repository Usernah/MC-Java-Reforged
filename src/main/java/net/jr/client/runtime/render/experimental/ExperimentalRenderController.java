package net.jr.client.runtime.render.experimental;

import java.util.ArrayList;
import java.util.List;
import net.jr.ClientConfig;
import net.jr.Java_reforged;
import com.mojang.blaze3d.systems.RenderSystem;

public final class ExperimentalRenderController {
    private static final List<ExperimentalRenderFeature> FEATURES = new ArrayList<>();

    private static volatile boolean requestedEnabled;
    private static boolean initialized;
    private static volatile boolean enabled;
    private static long generation;

    private ExperimentalRenderController() {
    }

    public static synchronized void register(ExperimentalRenderFeature feature) {
        if (initialized) {
            throw new IllegalStateException("Experimental render features must be registered before the first frame");
        }
        for (ExperimentalRenderFeature registered : FEATURES) {
            if (registered.id().equals(feature.id())) {
                throw new IllegalArgumentException("Duplicate experimental render feature " + feature.id());
            }
        }
        FEATURES.add(feature);
    }

    public static void requestEnabled(boolean requested) {
        requestedEnabled = requested;
    }

    public static boolean enabled() {
        return enabled;
    }

    public static long generation() {
        return generation;
    }

    public static void beginFrame() {
        RenderSystem.assertOnRenderThread();
        requestedEnabled = ClientConfig.experimentalRenderOptimizations();
        if (!initialized) {
            initialized = true;
            enabled = false;
            if (requestedEnabled) {
                enableFeatures();
            }
            ExperimentalRenderMetrics.beginRun(enabled, generation);
        } else if (requestedEnabled != enabled) {
            transition(requestedEnabled);
        }
        ExperimentalRenderMetrics.beginFrame();
    }

    public static void endFrame() {
        RenderSystem.assertOnRenderThread();
        ExperimentalRenderMetrics.endFrame();
    }

    private static void transition(boolean nextEnabled) {
        boolean previous = enabled;
        if (nextEnabled) {
            enableFeatures();
        } else {
            disableFeatures();
        }
        generation++;
        ExperimentalRenderMetrics.changeRun(previous, enabled, generation);
        Java_reforged.LOGGER.info(
            "Experimental render optimizations changed from {} to {} at generation {}",
            previous ? "ON" : "OFF",
            enabled ? "ON" : "OFF",
            generation
        );
    }

    private static void enableFeatures() {
        List<ExperimentalRenderFeature> enabledNow = new ArrayList<>();
        try {
            for (ExperimentalRenderFeature feature : FEATURES) {
                feature.enable();
                enabledNow.add(feature);
            }
            enabled = true;
        } catch (RuntimeException exception) {
            for (int index = enabledNow.size() - 1; index >= 0; index--) {
                safelyDisable(enabledNow.get(index));
            }
            enabled = false;
            requestedEnabled = false;
            ClientConfig.setExperimentalRenderOptimizations(false);
            throw new IllegalStateException("Could not enable experimental render optimizations", exception);
        }
    }

    private static void disableFeatures() {
        for (int index = FEATURES.size() - 1; index >= 0; index--) {
            safelyDisable(FEATURES.get(index));
        }
        enabled = false;
    }

    private static void safelyDisable(ExperimentalRenderFeature feature) {
        try {
            feature.disable();
        } catch (RuntimeException exception) {
            Java_reforged.LOGGER.error("Could not clean experimental render feature {}", feature.id(), exception);
        }
    }
}
