package net.jr.client.ui.hint.glyph;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.jr.Java_reforged;
import net.jr.api.client.resource.Asset;
import net.jr.client.ui.hint.render.GlyphTextureBoundsCache;
import net.jr.client.ui.presentation.UiPresentation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * Reloadable metric store for controller glyph themes.
 *
 * The Java theme registry owns theme structure. This class owns only the
 * default and per-glyph heights read from definitions.json.
 */
@EventBusSubscriber(modid = Java_reforged.MODID, value = Dist.CLIENT)
public final class ControllerGlyphDefinitions {
    /** Stable Asset used as the NeoForge reload-listener key. */
    private static final Asset RELOAD_LISTENER_ID =
        Asset.MOD("reload/controller_glyph_definitions");

    /** Reverse index that translates JSON keys such as button_down to enum values. */
    private static final Map<String, ControllerGlyph> GLYPHS_BY_NAME = createGlyphIndex();

    /** Immutable snapshot currently visible to the render thread. */
    private static volatile Map<ControllerGlyphTheme, Definition> definitions = Map.of();
    private static volatile Map<String, Map<ControllerGlyphTheme, Definition>> variantDefinitions = Map.of();
    private static volatile Set<Asset> availableVariantTextures = Set.of();

    private ControllerGlyphDefinitions() {
    }

    @SubscribeEvent
    public static void registerReloadListener(AddClientReloadListenersEvent event) {
        RELOAD_LISTENER_ID.registerReloadListener(event, new ReloadListener());
    }

    /**
     * Returns a specific glyph height or the theme's default height.
     */
    public static float height(ControllerGlyphTheme theme, ControllerGlyph glyph) {
        Definition definition = activeDefinition(theme);
        if (definition == null) {
            throw new IllegalStateException(
                "Glyph definitions have not been loaded for theme " + theme
            );
        }
        return definition.heights().getOrDefault(glyph, definition.defaultHeight());
    }

    static Asset texture(ControllerGlyphTheme theme, ControllerGlyph glyph, boolean pressed) {
        Asset base = theme.baseTexture(glyph, pressed);
        if (base == null) {
            return null;
        }
        String folder = UiPresentation.resourceVariantFolder();
        if (folder.isEmpty()) {
            return base;
        }
        Asset variant = theme.variantTexture(folder, glyph, pressed);
        return availableVariantTextures.contains(variant) ? variant : base;
    }

    private static Definition activeDefinition(ControllerGlyphTheme theme) {
        String folder = UiPresentation.resourceVariantFolder();
        if (!folder.isEmpty()) {
            Definition variant = variantDefinitions.getOrDefault(folder, Map.of()).get(theme);
            if (variant != null) {
                return variant;
            }
        }
        return definitions.get(theme);
    }

    /**
     * Deserializes and validates one theme definition. On a failed live reload,
     * the last valid definition remains active for that theme.
     */
    private static Definition loadDefinition(
        ResourceManager manager,
        ControllerGlyphTheme theme
    ) {
        Asset source = theme.definitions();
        try (
            InputStream stream = source.open(manager);
            Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)
        ) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            float defaultHeight = positiveHeight(
                root.get("default_height"),
                theme + ".default_height"
            );

            JsonObject heightObject = root.getAsJsonObject("heights");
            if (heightObject == null) {
                throw new IllegalArgumentException("Missing heights object in " + source);
            }

            EnumMap<ControllerGlyph, Float> heights = new EnumMap<>(ControllerGlyph.class);
            for (Map.Entry<String, JsonElement> entry : heightObject.entrySet()) {
                ControllerGlyph glyph = GLYPHS_BY_NAME.get(entry.getKey());
                if (glyph == null) {
                    Java_reforged.LOGGER.warn(
                        "Unknown controller glyph '{}' in {}",
                        entry.getKey(),
                        source
                    );
                    continue;
                }
                if (!theme.supports(glyph)) {
                    Java_reforged.LOGGER.warn(
                        "Glyph '{}' is not supported by theme {}",
                        entry.getKey(),
                        theme
                    );
                    continue;
                }

                heights.put(
                    glyph,
                    positiveHeight(entry.getValue(), theme + "." + entry.getKey())
                );
            }

            return new Definition(defaultHeight, Map.copyOf(heights));
        } catch (Exception exception) {
            Definition previous = definitions.get(theme);
            if (previous != null) {
                Java_reforged.LOGGER.error(
                    "Invalid glyph definitions for {}; keeping previous values",
                    theme,
                    exception
                );
                return previous;
            }
            throw new IllegalStateException(
                "Could not load glyph definitions from " + source,
                exception
            );
        }
    }

    private static Definition loadVariantDefinition(ResourceManager manager, ControllerGlyphTheme theme, String folder, Definition base) {
        Asset source = theme.variantDefinitions(folder);
        try {
            if (source.find(manager).isEmpty()) {
                return base;
            }
            try (InputStream stream = source.open(manager); Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                boolean replacesDefaultHeight = root.has("default_height");
                float defaultHeight = replacesDefaultHeight
                    ? positiveHeight(root.get("default_height"), theme + "." + folder + ".default_height")
                    : base.defaultHeight();
                EnumMap<ControllerGlyph, Float> heights = new EnumMap<>(ControllerGlyph.class);
                // A contextual default defines a complete new sizing baseline. In that
                // case base per-glyph exceptions must not mask it. Without a contextual
                // default, the file behaves as a genuinely partial overlay.
                if (!replacesDefaultHeight) {
                    heights.putAll(base.heights());
                }
                JsonObject heightObject = root.getAsJsonObject("heights");
                if (heightObject != null) {
                    for (Map.Entry<String, JsonElement> entry : heightObject.entrySet()) {
                        ControllerGlyph glyph = GLYPHS_BY_NAME.get(entry.getKey());
                        if (glyph != null && theme.supports(glyph)) {
                            heights.put(glyph, positiveHeight(entry.getValue(), theme + "." + folder + "." + entry.getKey()));
                        }
                    }
                }
                return new Definition(defaultHeight, Map.copyOf(heights));
            }
        } catch (Exception exception) {
            Java_reforged.LOGGER.error("Invalid optional glyph definitions {}; using base values", source, exception);
            return base;
        }
    }

    /** Validates that a JSON element is a usable positive render height. */
    private static float positiveHeight(JsonElement element, String field) {
        if (element == null || !element.isJsonPrimitive()) {
            throw new IllegalArgumentException("Missing numeric height: " + field);
        }

        float value = element.getAsFloat();
        if (!Float.isFinite(value) || value <= 0.0F) {
            throw new IllegalArgumentException(
                "Height must be positive and finite: " + field
            );
        }
        return value;
    }

    /** Builds the immutable reverse lookup used while parsing JSON. */
    private static Map<String, ControllerGlyph> createGlyphIndex() {
        Map<String, ControllerGlyph> index = new HashMap<>();
        for (ControllerGlyph glyph : ControllerGlyph.values()) {
            index.put(glyph.fileName(), glyph);
        }
        return Map.copyOf(index);
    }

    /** Immutable data-transfer object representing one parsed JSON file. */
    private record Definition(
        float defaultHeight,
        Map<ControllerGlyph, Float> heights
    ) {
    }

    /**
     * Two-phase listener: prepare performs I/O and parsing, then apply exposes
     * the complete immutable snapshot atomically.
     */
    private static final class ReloadListener
        extends SimplePreparableReloadListener<ReloadSnapshot> {

        @Override
        protected ReloadSnapshot prepare(
            ResourceManager manager,
            ProfilerFiller profiler
        ) {
            EnumMap<ControllerGlyphTheme, Definition> loaded =
                new EnumMap<>(ControllerGlyphTheme.class);
            for (ControllerGlyphTheme theme : ControllerGlyphTheme.values()) {
                Definition base = loadDefinition(manager, theme);
                loaded.put(theme, base);
            }
            Map<String, Map<ControllerGlyphTheme, Definition>> variants = new HashMap<>();
            Set<Asset> textures = new HashSet<>();
            for (String folder : new String[]{"portable", "split_screen"}) {
                EnumMap<ControllerGlyphTheme, Definition> byTheme = new EnumMap<>(ControllerGlyphTheme.class);
                for (ControllerGlyphTheme theme : ControllerGlyphTheme.values()) {
                    byTheme.put(theme, loadVariantDefinition(manager, theme, folder, loaded.get(theme)));
                    for (ControllerGlyph glyph : theme.supportedGlyphs()) {
                        Asset normal = theme.variantTexture(folder, glyph, false);
                        if (normal != null && normal.find(manager).isPresent()) textures.add(normal);
                        Asset pressed = theme.variantTexture(folder, glyph, true);
                        if (pressed != null && pressed.find(manager).isPresent()) textures.add(pressed);
                    }
                }
                variants.put(folder, Map.copyOf(byTheme));
            }
            return new ReloadSnapshot(Map.copyOf(loaded), Map.copyOf(variants), Set.copyOf(textures));
        }

        @Override
        protected void apply(
            ReloadSnapshot loaded,
            ResourceManager manager,
            ProfilerFiller profiler
        ) {
            definitions = loaded.base();
            variantDefinitions = loaded.variants();
            availableVariantTextures = loaded.availableTextures();
            GlyphTextureBoundsCache.clear();
        }
    }

    private record ReloadSnapshot(
        Map<ControllerGlyphTheme, Definition> base,
        Map<String, Map<ControllerGlyphTheme, Definition>> variants,
        Set<Asset> availableTextures
    ) {
    }
}
