package net.jr.api.client.render.glyph;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public final class Glyphs {
    private static final Pattern ARGUMENT_KEY = Pattern.compile("[a-z0-9_]+");
    private static final String SPACING_MARKER_PREFIX = "java_reforged::layout/space@width:";

    private Glyphs() {
    }

    public static MutableComponent component(String id) {
        return Component.literal(marker(null, null, id));
    }

    public static MutableComponent component(String id, Map<String, String> arguments) {
        return Component.literal(marker(null, null, id, arguments));
    }

    public static MutableComponent component(String provider, String id) {
        return Component.literal(marker(null, provider, id));
    }

    public static MutableComponent component(
        String provider,
        String id,
        Map<String, String> arguments
    ) {
        return Component.literal(marker(null, provider, id, arguments));
    }

    public static MutableComponent component(String namespace, String provider, String id) {
        return Component.literal(marker(namespace, provider, id));
    }

    public static MutableComponent component(
        String namespace,
        String provider,
        String id,
        Map<String, String> arguments
    ) {
        return Component.literal(marker(namespace, provider, id, arguments));
    }

    public static MutableComponent horizontalSpace(float pixels) {
        if (!Float.isFinite(pixels) || pixels < 0.0F) {
            throw new IllegalArgumentException("pixels must be finite and non-negative");
        }
        return Component.literal(marker(
            "java_reforged",
            "layout",
            "space",
            Map.of("width", Float.toString(pixels))
        ));
    }

    @Nullable
    public static Float horizontalSpaceWidth(String marker) {
        if (marker == null || !marker.startsWith(SPACING_MARKER_PREFIX)) {
            return null;
        }
        String encoded = marker.substring(SPACING_MARKER_PREFIX.length());
        if (encoded.isEmpty() || encoded.indexOf('@') >= 0) {
            return null;
        }
        try {
            float pixels = Float.parseFloat(encoded);
            return Float.isFinite(pixels) && pixels >= 0.0F ? pixels : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public static String marker(
        @Nullable String namespace,
        @Nullable String provider,
        String id
    ) {
        return marker(namespace, provider, id, Map.of());
    }

    public static String marker(
        @Nullable String namespace,
        @Nullable String provider,
        String id,
        Map<String, String> arguments
    ) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(arguments, "arguments");
        StringBuilder marker = new StringBuilder("${");
        if (namespace != null && !namespace.isBlank()) {
            marker.append(namespace).append("::");
        }
        if (provider != null && !provider.isBlank()) {
            marker.append(provider).append('/');
        }
        marker.append(id);
        arguments.forEach((key, value) -> {
            if (key == null || !ARGUMENT_KEY.matcher(key).matches()) {
                throw new IllegalArgumentException("Invalid glyph argument key: " + key);
            }
            if (value == null
                || value.isBlank()
                || value.indexOf('@') >= 0
                || value.indexOf('}') >= 0) {
                throw new IllegalArgumentException(
                    "Invalid value for glyph argument '" + key + "': " + value
                );
            }
            marker.append('@').append(key).append(':').append(value);
        });
        return marker.append('}').toString();
    }
}
