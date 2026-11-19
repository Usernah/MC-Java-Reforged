package net.jr.bootstrap;

import java.net.URL;
import javax.annotation.Nullable;
import net.neoforged.fml.loading.FMLLoader;


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
