package net.jr.client.ui.hint.glyph;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.jr.Java_reforged;
import net.jr.api.client.resource.Asset;
import net.jr.client.ui.hint.render.GlyphTextureBoundsCache;
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

/** Reloadable metric store for keyboard and mouse glyph themes. */
@EventBusSubscriber(modid = Java_reforged.MODID, value = Dist.CLIENT)
public final class KeyboardMouseGlyphDefinitions {
    private static final Asset RELOAD_LISTENER_ID =
        Asset.MOD("reload/keyboard_mouse_glyph_definitions");

    private static final Map<String, KeyboardMouseGlyph> GLYPHS_BY_NAME = createGlyphIndex();
    private static volatile Map<KeyboardMouseGlyphTheme, Definition> definitions = Map.of();

    private KeyboardMouseGlyphDefinitions() {
    }

    @SubscribeEvent
    public static void registerReloadListener(AddClientReloadListenersEvent event) {
        RELOAD_LISTENER_ID.registerReloadListener(event, new ReloadListener());
    }

    public static float height(KeyboardMouseGlyphTheme theme, KeyboardMouseGlyph glyph) {
        Definition definition = definitions.get(theme);
        if (definition == null) {
            throw new IllegalStateException(
                "Keyboard and mouse glyph definitions have not been loaded for theme " + theme
            );
        }
        return definition.heights().getOrDefault(glyph, definition.defaultHeight());
    }

    private static Definition loadDefinition(
        ResourceManager manager,
        KeyboardMouseGlyphTheme theme
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

            EnumMap<KeyboardMouseGlyph, Float> heights =
                new EnumMap<>(KeyboardMouseGlyph.class);
            for (Map.Entry<String, JsonElement> entry : heightObject.entrySet()) {
                KeyboardMouseGlyph glyph = GLYPHS_BY_NAME.get(entry.getKey());
                if (glyph == null) {
                    Java_reforged.LOGGER.warn(
                        "Unknown keyboard or mouse glyph '{}' in {}",
                        entry.getKey(),
                        source
                    );
                    continue;
                }
                if (!theme.supports(glyph)) {
                    Java_reforged.LOGGER.warn(
                        "Glyph '{}' is not supported by keyboard and mouse theme {}",
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
                    "Invalid keyboard and mouse glyph definitions for {}; keeping previous values",
                    theme,
                    exception
                );
                return previous;
            }
            throw new IllegalStateException(
                "Could not load keyboard and mouse glyph definitions from " + source,
                exception
            );
        }
    }

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

    private static Map<String, KeyboardMouseGlyph> createGlyphIndex() {
        Map<String, KeyboardMouseGlyph> index = new HashMap<>();
        for (KeyboardMouseGlyph glyph : KeyboardMouseGlyph.values()) {
            index.put(glyph.fileName(), glyph);
        }
        return Map.copyOf(index);
    }

    private record Definition(
        float defaultHeight,
        Map<KeyboardMouseGlyph, Float> heights
    ) {
    }

    private static final class ReloadListener
        extends SimplePreparableReloadListener<Map<KeyboardMouseGlyphTheme, Definition>> {

        @Override
        protected Map<KeyboardMouseGlyphTheme, Definition> prepare(
            ResourceManager manager,
            ProfilerFiller profiler
        ) {
            EnumMap<KeyboardMouseGlyphTheme, Definition> loaded =
                new EnumMap<>(KeyboardMouseGlyphTheme.class);
            for (KeyboardMouseGlyphTheme theme : KeyboardMouseGlyphTheme.values()) {
                loaded.put(theme, loadDefinition(manager, theme));
            }
            return Map.copyOf(loaded);
        }

        @Override
        protected void apply(
            Map<KeyboardMouseGlyphTheme, Definition> loaded,
            ResourceManager manager,
            ProfilerFiller profiler
        ) {
            definitions = loaded;
            GlyphTextureBoundsCache.clear();
        }
    }
}
