package net.jr.client.render.glyph;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.jr.api.client.render.glyph.GlyphDefinition;
import net.jr.api.client.render.glyph.GlyphProvider;
import net.jr.api.client.resource.Asset;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class StaticGlyphProvider implements GlyphProvider {
    private static final Set<String> ALLOWED_FIELDS = Set.of(
        "asset", "h", "w", "offset_x", "offset_y", "advance"
    );

    private final Map<String, GlyphDefinition> definitions;

    public StaticGlyphProvider(Identifier providerId, Map<String, JsonObject> source) {
        Map<String, GlyphDefinition> parsed = new LinkedHashMap<>();
        source.forEach((id, value) -> parsed.put(id, parse(providerId, id, value)));
        this.definitions = Map.copyOf(parsed);
    }

    @Override
    @Nullable
    public GlyphDefinition resolve(String id) {
        return this.definitions.get(id);
    }

    protected static GlyphDefinition parse(
        Identifier providerId,
        String id,
        JsonObject object
    ) {
        rejectUnknownFields(providerId, id, object, ALLOWED_FIELDS);
        Asset asset = asset(requiredString(providerId, id, object, "asset"));
        float height = requiredNumber(providerId, id, object, "h");
        Float width = optionalNumber(providerId, id, object, "w");
        float offsetX = optionalNumber(providerId, id, object, "offset_x", 0.0F);
        float offsetY = optionalNumber(providerId, id, object, "offset_y", 0.0F);
        Float advance = optionalNumber(providerId, id, object, "advance");
        return new GlyphDefinition(asset, height, width, offsetX, offsetY, advance);
    }

    protected static Asset asset(String path) {
        return path.indexOf(':') >= 0 ? Asset.PARSE(path) : Asset.MOD(path);
    }

    protected static String requiredString(
        Identifier providerId,
        String id,
        JsonObject object,
        String field
    ) {
        JsonElement value = object.get(field);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()
            || value.getAsString().isBlank()) {
            throw invalid(providerId, id, "Missing non-empty text field '" + field + "'");
        }
        return value.getAsString();
    }

    protected static float requiredNumber(
        Identifier providerId,
        String id,
        JsonObject object,
        String field
    ) {
        Float value = optionalNumber(providerId, id, object, field);
        if (value == null) {
            throw invalid(providerId, id, "Missing numeric field '" + field + "'");
        }
        return value;
    }

    @Nullable
    protected static Float optionalNumber(
        Identifier providerId,
        String id,
        JsonObject object,
        String field
    ) {
        JsonElement value = object.get(field);
        if (value == null) {
            return null;
        }
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw invalid(providerId, id, "Field '" + field + "' must be numeric");
        }
        float result = value.getAsFloat();
        if (!Float.isFinite(result)) {
            throw invalid(providerId, id, "Field '" + field + "' must be finite");
        }
        return result;
    }

    protected static float optionalNumber(
        Identifier providerId,
        String id,
        JsonObject object,
        String field,
        float fallback
    ) {
        Float value = optionalNumber(providerId, id, object, field);
        return value == null ? fallback : value;
    }

    protected static void rejectUnknownFields(
        Identifier providerId,
        String id,
        JsonObject object,
        Set<String> allowed
    ) {
        for (String field : object.keySet()) {
            if (!allowed.contains(field)) {
                throw invalid(providerId, id, "Unsupported field '" + field + "'");
            }
        }
    }

    protected static IllegalArgumentException invalid(
        Identifier providerId,
        String id,
        String message
    ) {
        return new IllegalArgumentException(providerId + "/" + id + ": " + message);
    }
}
