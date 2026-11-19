package net.jr.client.render.glyph;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.jr.Java_reforged;
import net.jr.api.client.render.glyph.GlyphDefinition;
import net.jr.api.client.render.glyph.GlyphProvider;
import net.jr.api.client.render.glyph.GlyphProviderFactory;
import net.jr.api.client.resource.Asset;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import org.jetbrains.annotations.Nullable;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public final class GlyphRegistryStore {
    public static final String DEFAULT_NAMESPACE = Java_reforged.MODID;
    public static final String DEFAULT_PROVIDER = "default";
    private static final String DIRECTORY = "glyphs";
    private static final String EXTENSION = ".json";
    private static final Pattern ID_PATTERN = Pattern.compile("[A-Za-z0-9_]+");
    private static final Pattern PROVIDER_PATTERN = Pattern.compile("[a-z0-9_]+");
    private static final Asset RELOAD_ID = Asset.MOD("reload/glyph_providers");
    private static final Map<Identifier, GlyphProviderFactory> FACTORIES =
        new ConcurrentHashMap<>();
    private static volatile Map<Identifier, GlyphProvider> providers = Map.of();

    private GlyphRegistryStore() {
    }

    public static void register(Identifier providerId, GlyphProviderFactory factory) {
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(factory, "factory");
        GlyphProviderFactory previous = FACTORIES.putIfAbsent(providerId, factory);
        if (previous != null) {
            throw new IllegalArgumentException("Glyph provider already registered: " + providerId);
        }
    }

    public static void registerClientReloadListener(AddClientReloadListenersEvent event) {
        RELOAD_ID.registerReloadListener(event, new ReloadListener());
    }

    @Nullable
    public static GlyphDefinition resolve(String marker) {
        Reference reference = Reference.parse(marker);
        if (reference == null) {
            return null;
        }
        GlyphProvider provider = providers.get(reference.providerId());
        return provider == null
            ? null
            : provider.resolve(reference.glyphId(), reference.arguments());
    }

    private static Map<Identifier, GlyphProvider> load(ResourceManager manager) {
        Map<Identifier, List<Resource>> resources = manager.listResourceStacks(
            DIRECTORY,
            location -> isProviderFile(location.getPath())
        );
        Map<Identifier, GlyphProvider> loaded = new LinkedHashMap<>();

        for (Map.Entry<Identifier, List<Resource>> resourceEntry : resources.entrySet()) {
            Identifier source = resourceEntry.getKey();
            String providerName = providerName(source.getPath());
            if (providerName == null) {
                continue;
            }
            Identifier providerId = Identifier.fromNamespaceAndPath(
                source.getNamespace(), providerName
            );
            Map<String, JsonObject> definitions = merge(
                providerId, resourceEntry.getValue()
            );
            try {
                GlyphProviderFactory factory = FACTORIES.getOrDefault(
                    providerId, StaticGlyphProvider::new
                );
                loaded.put(providerId, factory.create(providerId, definitions));
            } catch (RuntimeException exception) {
                Java_reforged.LOGGER.error(
                    "Could not load glyph provider {}", providerId, exception
                );
            }
        }
        return Map.copyOf(loaded);
    }

    private static Map<String, JsonObject> merge(
        Identifier providerId,
        List<Resource> stack
    ) {
        Map<String, JsonObject> merged = new LinkedHashMap<>();
        for (Resource resource : stack) {
            try (Reader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
                JsonElement parsed = JsonParser.parseReader(reader);
                if (!parsed.isJsonObject()) {
                    throw new IllegalArgumentException("Root must be an object");
                }
                for (Map.Entry<String, JsonElement> entry : parsed.getAsJsonObject().entrySet()) {
                    String id = entry.getKey();
                    if (!ID_PATTERN.matcher(id).matches()) {
                        Java_reforged.LOGGER.error(
                            "Invalid glyph id '{}' in {}; only letters, numbers and '_' are allowed",
                            id,
                            providerId
                        );
                        continue;
                    }
                    if (!entry.getValue().isJsonObject()) {
                        Java_reforged.LOGGER.error(
                            "Glyph {}/{} must be an object", providerId, id
                        );
                        continue;
                    }
                    merged.put(id, entry.getValue().getAsJsonObject().deepCopy());
                }
            } catch (Exception exception) {
                Java_reforged.LOGGER.error(
                    "Invalid glyph provider resource {}", providerId, exception
                );
            }
        }
        return Map.copyOf(merged);
    }

    private static boolean isProviderFile(String path) {
        if (!path.startsWith(DIRECTORY + "/") || !path.endsWith(EXTENSION)) {
            return false;
        }
        String relative = path.substring(DIRECTORY.length() + 1, path.length() - EXTENSION.length());
        return PROVIDER_PATTERN.matcher(relative).matches();
    }

    @Nullable
    private static String providerName(String path) {
        if (!isProviderFile(path)) {
            return null;
        }
        return path.substring(DIRECTORY.length() + 1, path.length() - EXTENSION.length());
    }

    private record Reference(
        Identifier providerId,
        String glyphId,
        Map<String, String> arguments
    ) {
        @Nullable
        private static Reference parse(String marker) {
            if (marker == null) {
                return null;
            }
            String value = marker.trim();
            if (value.isEmpty()) {
                return null;
            }

            Map<String, String> arguments = new LinkedHashMap<>();
            int argumentStart = value.indexOf('@');
            if (argumentStart >= 0) {
                String encodedArguments = value.substring(argumentStart + 1);
                value = value.substring(0, argumentStart);
                if (value.isEmpty() || !parseArguments(encodedArguments, arguments)) {
                    return null;
                }
            }

            String namespace = DEFAULT_NAMESPACE;
            int namespaceSeparator = value.indexOf("::");
            if (namespaceSeparator >= 0) {
                if (namespaceSeparator == 0 || value.indexOf("::", namespaceSeparator + 2) >= 0) {
                    return null;
                }
                namespace = value.substring(0, namespaceSeparator);
                value = value.substring(namespaceSeparator + 2);
            }

            String provider = DEFAULT_PROVIDER;
            int providerSeparator = value.indexOf('/');
            if (providerSeparator >= 0) {
                if (providerSeparator == 0 || value.indexOf('/', providerSeparator + 1) >= 0) {
                    return null;
                }
                provider = value.substring(0, providerSeparator);
                value = value.substring(providerSeparator + 1);
            }

            if (!PROVIDER_PATTERN.matcher(provider).matches()
                || !ID_PATTERN.matcher(value).matches()) {
                return null;
            }
            try {
                return new Reference(
                    Identifier.fromNamespaceAndPath(namespace, provider),
                    value,
                    Map.copyOf(arguments)
                );
            } catch (RuntimeException ignored) {
                return null;
            }
        }

        private static boolean parseArguments(
            String source,
            Map<String, String> destination
        ) {
            if (source.isEmpty()
                || source.charAt(0) == '@'
                || source.charAt(source.length() - 1) == '@') {
                return false;
            }
            int cursor = 0;
            while (cursor < source.length()) {
                int next = source.indexOf('@', cursor);
                int end = next < 0 ? source.length() : next;
                String entry = source.substring(cursor, end).trim();
                int separator = entry.indexOf(':');
                if (separator <= 0 || separator == entry.length() - 1) {
                    return false;
                }

                String key = entry.substring(0, separator).trim();
                String value = entry.substring(separator + 1).trim();
                if (!PROVIDER_PATTERN.matcher(key).matches()
                    || value.isEmpty()
                    || destination.putIfAbsent(key, value) != null) {
                    return false;
                }
                cursor = end + 1;
            }
            return !destination.isEmpty();
        }
    }

    private static final class ReloadListener
        extends SimplePreparableReloadListener<Map<Identifier, GlyphProvider>> {
        @Override
        protected Map<Identifier, GlyphProvider> prepare(
            ResourceManager manager,
            ProfilerFiller profiler
        ) {
            return load(manager);
        }

        @Override
        protected void apply(
            Map<Identifier, GlyphProvider> loaded,
            ResourceManager manager,
            ProfilerFiller profiler
        ) {
            providers = loaded;
            GlyphTextureBoundsCache.clear();
            Java_reforged.LOGGER.info("Loaded {} glyph providers", loaded.size());
        }
    }
}
