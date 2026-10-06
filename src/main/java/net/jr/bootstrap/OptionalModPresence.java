package net.jr.bootstrap;

import java.net.URL;
import javax.annotation.Nullable;
import net.neoforged.fml.loading.FMLLoader;

/**
 * Early-safe optional-mod detection for Mixin selection and runtime backend routing.
 * This class deliberately does not reference Minecraft or optional-mod classes.
 */
public final class OptionalModPresence {
    private static final String SODIUM_CLASS_RESOURCE =
        "net/caffeinemc/mods/sodium/client/SodiumClientMod.class";

    @Nullable
    private static volatile Boolean sodiumLoaded;

    private OptionalModPresence() {
    }

    public static boolean sodiumLoaded() {
        Boolean cached = sodiumLoaded;
        if (cached != null) {
            return cached;
        }

        Detection detection = detect("sodium", SODIUM_CLASS_RESOURCE);
        if (detection.resolved()) {
            sodiumLoaded = detection.loaded();
        }
        return detection.loaded();
    }

    private static Detection detect(String modId, String markerResource) {
        FMLLoader loader = FMLLoader.getCurrentOrNull();
        if (loader != null) {
            try {
                return new Detection(true, loader.getLoadingModList().getModFileById(modId) != null);
            } catch (IllegalStateException ignored) {
                // Mixin configuration can be evaluated before the final loading list is published.
            }
        }

        return new Detection(false, findResource(markerResource) != null);
    }

    private static @Nullable URL findResource(String name) {
        ClassLoader ownLoader = OptionalModPresence.class.getClassLoader();
        URL resource = ownLoader != null ? ownLoader.getResource(name) : ClassLoader.getSystemResource(name);
        if (resource != null) {
            return resource;
        }
        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        return contextLoader != null ? contextLoader.getResource(name) : null;
    }

    private record Detection(boolean resolved, boolean loaded) {
    }
}
