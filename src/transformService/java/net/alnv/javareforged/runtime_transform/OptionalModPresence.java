package net.alnv.javareforged.runtime_transform;

import java.net.URL;
import net.neoforged.fml.loading.FMLLoader;

final class OptionalModPresence {
    private static final String SODIUM_CLASS_RESOURCE =
        "net/caffeinemc/mods/sodium/client/SodiumClientMod.class";

    private static volatile Boolean sodiumLoaded;

    private OptionalModPresence() {
    }

    static boolean sodiumLoaded() {
        Boolean cached = sodiumLoaded;
        if (cached != null) {
            return cached;
        }

        FMLLoader loader = FMLLoader.getCurrentOrNull();
        if (loader != null) {
            try {
                boolean loaded = loader.getLoadingModList().getModFileById("sodium") != null;
                sodiumLoaded = loaded;
                return loaded;
            } catch (IllegalStateException ignored) {

            }
        }

        return findResource(SODIUM_CLASS_RESOURCE) != null;
    }

    private static URL findResource(String name) {
        ClassLoader ownLoader = OptionalModPresence.class.getClassLoader();
        URL resource = ownLoader != null ? ownLoader.getResource(name) : ClassLoader.getSystemResource(name);
        if (resource != null) {
            return resource;
        }
        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        return contextLoader != null ? contextLoader.getResource(name) : null;
    }
}
