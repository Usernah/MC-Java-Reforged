package net.jr.client.render.glyph;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.jr.api.client.input.GamepadDigitalInput;
import net.jr.api.client.render.glyph.GlyphDefinition;
import net.jr.api.client.render.glyph.GlyphProvider;
import net.jr.api.client.resource.Asset;
import net.jr.client.ui.hint.ControlHintPressAnimation;
import net.jr.client.ui.hint.glyph.ControllerGlyph;
import net.jr.client.ui.hint.glyph.ControllerGlyphTheme;
import net.jr.client.ui.hint.glyph.GamepadGlyphThemeState;
import net.jr.client.ui.hint.glyph.KeyboardMouseGlyph;
import net.jr.client.ui.hint.glyph.KeyboardMouseGlyphResolver;
import net.jr.client.ui.presentation.UiPresentation;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class ControllerGlyphProvider implements GlyphProvider {
    private static final String DEFAULTS_ID = "_defaults";
    private static final Set<String> ALLOWED_FIELDS = Set.of(
        "asset", "h", "press_asset", "advance", "offset_x", "offset_y"
    );
    private static final Set<String> DEFAULT_FIELDS = Set.of(
        "advance", "offset_x", "offset_y"
    );
    private static final Pattern METRIC_KEY = Pattern.compile(
        "[a-z0-9_]+"
    );
    private static final Map<ControllerGlyph, GamepadDigitalInput> INPUTS = inputs();
    private final Map<String, Definition> definitions;

    public ControllerGlyphProvider(Identifier providerId, Map<String, JsonObject> source) {
        Defaults defaults = parseDefaults(providerId, source.get(DEFAULTS_ID));
        Map<String, Definition> parsed = new LinkedHashMap<>();
        source.forEach((id, object) -> {
            if (id.equals(DEFAULTS_ID)) {
                return;
            }
            StaticGlyphProvider.rejectUnknownFields(
                providerId, id, object, ALLOWED_FIELDS
            );
            String asset = StaticGlyphProvider.requiredString(
                providerId, id, object, "asset"
            );
            HeightMetrics heights = parseHeights(providerId, id, object);
            String pressedAsset = object.has("press_asset")
                ? StaticGlyphProvider.requiredString(providerId, id, object, "press_asset")
                : null;
            AdvanceMetrics advance = object.has("advance")
                ? parseAdvance(providerId, id, object.get("advance"))
                : defaults.advance();
            OffsetMetrics offsetX = object.has("offset_x")
                ? parseOffset(providerId, id, "offset_x", object.get("offset_x"))
                : defaults.offsetX();
            OffsetMetrics offsetY = object.has("offset_y")
                ? parseOffset(providerId, id, "offset_y", object.get("offset_y"))
                : defaults.offsetY();
            parsed.put(id, new Definition(
                asset,
                heights,
                pressedAsset,
                advance,
                offsetX,
                offsetY
            ));
        });
        this.definitions = Map.copyOf(parsed);
    }

    @Override
    @Nullable
    public GlyphDefinition resolve(String id) {
        return this.resolve(id, Map.of());
    }

    @Override
    @Nullable
    public GlyphDefinition resolve(String id, Map<String, String> arguments) {
        if (!arguments.isEmpty()) {
            return null;
        }
        Definition definition = this.definitions.get(id);
        if (definition == null) {
            return null;
        }

        ControllerGlyph controllerGlyph = enumValue(ControllerGlyph.class, id);
        KeyboardMouseGlyph keyboardGlyph = enumValue(KeyboardMouseGlyph.class, id);
        Asset root;
        String metricId;
        boolean pressed = false;

        if (controllerGlyph != null) {
            ControllerGlyphTheme theme = GamepadGlyphThemeState.themeForCurrentClient();
            if (!theme.supports(controllerGlyph)) {
                return null;
            }
            root = theme.textureFolder();
            metricId = theme.metricId();
            GamepadDigitalInput input = INPUTS.get(controllerGlyph);
            pressed = input != null && ControlHintPressAnimation.isAnimating(input);
        } else if (keyboardGlyph != null) {
            var theme = KeyboardMouseGlyphResolver.currentTheme();
            root = theme.textureFolder();
            metricId = theme.metricId();
        } else {
            return null;
        }

        Asset asset = null;
        if (pressed && definition.pressAsset() != null) {
            asset = themedAsset(root, definition.pressAsset(), true);
        }
        if (asset == null) {
            asset = themedAsset(root, definition.asset(), false);
        }
        boolean smallMetrics = UiPresentation.isSmall();
        float height = definition.heights().resolve(metricId)
            * (smallMetrics ? 1.0F : 2.0F);
        Float advance = definition.advance() == null
            ? null
            : definition.advance().resolve(smallMetrics);
        return new GlyphDefinition(
            asset,
            height,
            null,
            definition.offsetX().resolve(smallMetrics),
            definition.offsetY().resolve(smallMetrics),
            advance
        );
    }

    private static Defaults parseDefaults(
        Identifier providerId,
        @Nullable JsonObject defaults
    ) {
        if (defaults == null) {
            return Defaults.EMPTY;
        }
        StaticGlyphProvider.rejectUnknownFields(
            providerId, DEFAULTS_ID, defaults, DEFAULT_FIELDS
        );
        JsonElement advance = defaults.get("advance");
        AdvanceMetrics parsedAdvance = advance == null
            ? null
            : parseAdvance(providerId, DEFAULTS_ID, advance);
        return new Defaults(
            parsedAdvance,
            parseOptionalOffset(providerId, DEFAULTS_ID, defaults, "offset_x"),
            parseOptionalOffset(providerId, DEFAULTS_ID, defaults, "offset_y")
        );
    }

    private static OffsetMetrics parseOptionalOffset(
        Identifier providerId,
        String id,
        JsonObject object,
        String field
    ) {
        JsonElement value = object.get(field);
        return value == null
            ? OffsetMetrics.ZERO
            : parseOffset(providerId, id, field, value);
    }

    private static OffsetMetrics parseOffset(
        Identifier providerId,
        String id,
        String field,
        JsonElement value
    ) {
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
            float offset = finiteOffset(providerId, id, field, value);
            return new OffsetMetrics(offset, offset);
        }
        if (!value.isJsonArray() || value.getAsJsonArray().size() != 2) {
            throw StaticGlyphProvider.invalid(
                providerId,
                id,
                "Field '" + field + "' must be numeric or [base, small]"
            );
        }
        return new OffsetMetrics(
            finiteOffset(providerId, id, field + "[0]", value.getAsJsonArray().get(0)),
            finiteOffset(providerId, id, field + "[1]", value.getAsJsonArray().get(1))
        );
    }

    private static float finiteOffset(
        Identifier providerId,
        String id,
        String field,
        JsonElement value
    ) {
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw StaticGlyphProvider.invalid(
                providerId, id, "Field '" + field + "' must be numeric"
            );
        }
        float offset = value.getAsFloat();
        if (!Float.isFinite(offset)) {
            throw StaticGlyphProvider.invalid(
                providerId, id, "Field '" + field + "' must be finite"
            );
        }
        return offset;
    }

    private static AdvanceMetrics parseAdvance(
        Identifier providerId,
        String id,
        JsonElement value
    ) {
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
            float spacing = nonNegativeAdvance(providerId, id, "advance", value);
            return new AdvanceMetrics(spacing, spacing);
        }
        if (!value.isJsonArray() || value.getAsJsonArray().size() != 2) {
            throw StaticGlyphProvider.invalid(
                providerId,
                id,
                "Field 'advance' must be numeric or [base, small]"
            );
        }
        return new AdvanceMetrics(
            nonNegativeAdvance(
                providerId, id, "advance[0]", value.getAsJsonArray().get(0)
            ),
            nonNegativeAdvance(
                providerId, id, "advance[1]", value.getAsJsonArray().get(1)
            )
        );
    }

    private static float nonNegativeAdvance(
        Identifier providerId,
        String id,
        String field,
        JsonElement value
    ) {
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw StaticGlyphProvider.invalid(
                providerId, id, "Field '" + field + "' must be numeric"
            );
        }
        float advance = value.getAsFloat();
        if (!Float.isFinite(advance) || advance < 0.0F) {
            throw StaticGlyphProvider.invalid(
                providerId,
                id,
                "Field '" + field + "' must be non-negative and finite"
            );
        }
        return advance;
    }

    private static HeightMetrics parseHeights(
        Identifier providerId,
        String id,
        JsonObject definition
    ) {
        JsonElement value = definition.get("h");
        if (value == null) {
            throw StaticGlyphProvider.invalid(providerId, id, "Missing field 'h'");
        }
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
            return new HeightMetrics(positiveHeight(providerId, id, "h", value), Map.of());
        }
        if (!value.isJsonObject()) {
            throw StaticGlyphProvider.invalid(
                providerId, id, "Field 'h' must be numeric or an object"
            );
        }

        JsonObject variants = value.getAsJsonObject();
        JsonElement fallbackValue = variants.get("default");
        if (fallbackValue == null) {
            throw StaticGlyphProvider.invalid(
                providerId, id, "Height variants require a numeric 'default'"
            );
        }
        float fallback = positiveHeight(
            providerId, id, "h.default", fallbackValue
        );
        Map<String, Float> overrides = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : variants.entrySet()) {
            String key = entry.getKey();
            if (key.equals("default")) {
                continue;
            }
            if (!METRIC_KEY.matcher(key).matches()) {
                throw StaticGlyphProvider.invalid(
                    providerId, id, "Invalid height variant '" + key + "'"
                );
            }
            overrides.put(
                key,
                positiveHeight(providerId, id, "h." + key, entry.getValue())
            );
        }
        return new HeightMetrics(fallback, Map.copyOf(overrides));
    }

    private static float positiveHeight(
        Identifier providerId,
        String id,
        String field,
        JsonElement value
    ) {
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw StaticGlyphProvider.invalid(
                providerId, id, "Field '" + field + "' must be numeric"
            );
        }
        float height = value.getAsFloat();
        if (!Float.isFinite(height) || height <= 0.0F) {
            throw StaticGlyphProvider.invalid(
                providerId, id, "Field '" + field + "' must be positive and finite"
            );
        }
        return height;
    }

    @Nullable
    private static Asset themedAsset(
        Asset root,
        String relativePath,
        boolean mustExist
    ) {
        if (relativePath.indexOf(':') >= 0) {
            Asset parsed = Asset.PARSE(relativePath);
            return !mustExist || exists(parsed) ? parsed : null;
        }
        Asset base = root.child(relativePath);
        return !mustExist || exists(base) ? base : null;
    }

    private static boolean exists(Asset asset) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft != null && asset.find(minecraft.getResourceManager()).isPresent();
    }

    @Nullable
    private static <E extends Enum<E>> E enumValue(Class<E> type, String id) {
        try {
            return Enum.valueOf(type, id);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static Map<ControllerGlyph, GamepadDigitalInput> inputs() {
        EnumMap<ControllerGlyph, GamepadDigitalInput> result =
            new EnumMap<>(ControllerGlyph.class);
        for (GamepadDigitalInput input : GamepadDigitalInput.values()) {
            ControllerGlyph glyph = ControllerGlyph.forInput(input);
            if (glyph != null) {
                result.putIfAbsent(glyph, input);
            }
        }
        return Map.copyOf(result);
    }

    private record Definition(
        String asset,
        HeightMetrics heights,
        @Nullable String pressAsset,
        @Nullable AdvanceMetrics advance,
        OffsetMetrics offsetX,
        OffsetMetrics offsetY
    ) {
    }

    private record Defaults(
        @Nullable AdvanceMetrics advance,
        OffsetMetrics offsetX,
        OffsetMetrics offsetY
    ) {
        private static final Defaults EMPTY = new Defaults(
            null,
            OffsetMetrics.ZERO,
            OffsetMetrics.ZERO
        );
    }

    private record AdvanceMetrics(float base, float small) {
        private float resolve(boolean small) {
            return small ? this.small : this.base;
        }
    }

    private record OffsetMetrics(float base, float small) {
        private static final OffsetMetrics ZERO = new OffsetMetrics(0.0F, 0.0F);

        private float resolve(boolean small) {
            return small ? this.small : this.base;
        }
    }

    private record HeightMetrics(float fallback, Map<String, Float> overrides) {
        private float resolve(String theme) {
            Float themed = this.overrides.get(theme);
            if (themed != null) {
                return themed;
            }
            return this.fallback;
        }
    }
}
