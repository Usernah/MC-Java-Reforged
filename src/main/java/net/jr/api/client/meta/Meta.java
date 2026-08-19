package net.jr.api.client.meta;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Parsed metadata associated with a UI texture. */
public final class Meta {
    public final Map<String, Object> data;
    public final Map<String, Map<String, Object>> configs;

    public Meta(Map<String, Object> data, Map<String, Map<String, Object>> configs) {
        this.data = immutable(data);
        Map<String, Map<String, Object>> copy = new LinkedHashMap<>();
        configs.forEach((key, value) -> copy.put(key, immutable(value)));
        this.configs = Collections.unmodifiableMap(copy);
        validate(this.data);
        this.configs.keySet().forEach(key -> validate(resolveData(key)));
    }

    public boolean hasConfig(String id) {
        return configs.containsKey(id);
    }

    public Map<String, Object> resolveData() {
        return data;
    }

    public Map<String, Object> resolveData(String id) {
        if (id == null || id.isBlank() || !configs.containsKey(id)) {
            return data;
        }
        Map<String, Object> result = mutable(data);
        merge(result, configs.get(id));
        return immutable(result);
    }

    public String resolveType() {
        return resolveType(data);
    }

    public String resolveType(String id) {
        return resolveType(resolveData(id));
    }

    public Atlas atlas() {
        return Atlas.from(data);
    }

    public Atlas atlas(String id) {
        return Atlas.from(resolveData(id));
    }

    public float imageScale() {
        return imageScale(data);
    }

    public float imageScale(String id) {
        return imageScale(resolveData(id));
    }

    public static String resolveType(Map<String, Object> values) {
        return values.containsKey("nine") ? "NineSlice"
            : values.containsKey("animation") ? "Animation" : "Image";
    }

    public static float imageScale(Map<String, Object> root) {
        Map<String, Object> image = child(root, "image");
        if (image == null || !image.containsKey("scale")) {
            return 1.0F;
        }
        return decimal(image.get("scale"));
    }

    public record ImageSize(int width, int height) {
        public static ImageSize from(Map<String, Object> root) {
            int[] size = imageSize(root);
            return new ImageSize(size[0], size[1]);
        }
    }

    public record Atlas(int x, int y, int width, int height) {
        public static Atlas from(Map<String, Object> root) {
            Map<String, Object> atlas = child(root, "atlas");
            Map<String, Object> uv = child(atlas, "uv");
            Map<String, Object> region = child(atlas, "region");
            int[] image = imageSize(root);
            return new Atlas(integer(uv, "x"), integer(uv, "y"),
                dimension(region, "width", "w", image[0]),
                dimension(region, "height", "h", image[1]));
        }
    }

    public record NineSlice(int top, int bottom, int left, int right, boolean repeatCenter) {
        public static NineSlice from(Map<String, Object> root) {
            Map<String, Object> nine = child(root, "nine");
            Object raw = nine == null ? null : nine.get("borders");
            int top = 0, bottom = 0, left = 0, right = 0;
            if (raw instanceof Number || raw instanceof String) {
                top = bottom = left = right = integer(raw);
            } else {
                Map<String, Object> borders = child(nine, "borders");
                top = integer(borders, "top");
                bottom = integer(borders, "bottom");
                left = integer(borders, "left");
                right = integer(borders, "right");
            }
            return new NineSlice(top, bottom, left, right,
                nine != null && "repeat".equalsIgnoreCase(String.valueOf(nine.get("mode"))));
        }
    }

    public record Animation(int frames, int frameWidth, int frameHeight, int duration,
                            String unit, boolean interpolation, Map<Integer, Integer> frameDurations) {
        public static Animation from(Map<String, Object> root) {
            Map<String, Object> animation = child(root, "animation");
            Map<String, Object> size = child(animation, "frame_size");
            String unit = animation == null ? "ms" : String.valueOf(animation.getOrDefault("unit", "ms"));
            Map<Integer, Integer> durations = new LinkedHashMap<>();
            Map<String, Object> rawDurations = child(animation, "frames_duration");
            if (rawDurations != null) {
                rawDurations.forEach((key, value) -> {
                    try {
                        durations.put(Integer.parseInt(key), integer(value));
                    } catch (NumberFormatException ignored) {
                    }
                });
            }
            return new Animation(integer(animation, "frames"), integer(size, "width"),
                integer(size, "height"), integer(animation, "duration"), unit,
                bool(animation, "interpolation"), Collections.unmodifiableMap(durations));
        }
    }

    private static void validate(Map<String, Object> root) {
        int[] image = imageSize(root);
        float scale = imageScale(root);
        if (!Float.isFinite(scale) || scale <= 0.0F) {
            throw new IllegalArgumentException("image.scale must be a finite number greater than zero");
        }
        if ((root.containsKey("atlas") || root.containsKey("nine") || root.containsKey("animation"))
            && (image[0] <= 0 || image[1] <= 0)) {
            throw new IllegalArgumentException("Meta uses atlas/nine/animation but does not define image.size");
        }
        if (root.containsKey("animation")) {
            Animation animation = Animation.from(root);
            if (animation.frames <= 0 || animation.frameWidth <= 0
                || animation.frameHeight <= 0 || animation.duration <= 0) {
                throw new IllegalArgumentException("Incomplete animation metadata");
            }
            if (!switch (animation.unit.toLowerCase()) {
                case "ms", "ticks", "seconds", "minutes" -> true;
                default -> false;
            }) {
                throw new IllegalArgumentException("Unsupported animation unit: " + animation.unit);
            }
        }
    }

    static Map<String, Object> child(Map<String, Object> parent, String key) {
        if (parent == null || !(parent.get(key) instanceof Map<?, ?> raw)) {
            return null;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        raw.forEach((k, value) -> {
            if (k instanceof String text) result.put(text, value);
        });
        return result;
    }

    private static int[] imageSize(Map<String, Object> root) {
        Map<String, Object> size = child(child(root, "image"), "size");
        return new int[] {dimension(size, "width", "w", 0), dimension(size, "height", "h", 0)};
    }

    private static int dimension(Map<String, Object> map, String full, String shortName, int fallback) {
        if (map == null) return fallback;
        int result = integer(map.get(map.containsKey(full) ? full : shortName));
        return result > 0 ? result : fallback;
    }

    private static int integer(Map<String, Object> map, String key) {
        return map == null ? 0 : integer(map.get(key));
    }

    private static int integer(Object value) {
        if (value instanceof Number number) return number.intValue();
        try {
            return value == null ? 0 : Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static float decimal(Object value) {
        if (value instanceof Number number) return number.floatValue();
        try {
            return value == null ? Float.NaN : Float.parseFloat(value.toString().trim());
        } catch (NumberFormatException ignored) {
            return Float.NaN;
        }
    }

    private static boolean bool(Map<String, Object> map, String key) {
        Object value = map == null ? null : map.get(key);
        return value instanceof Boolean bool ? bool : value != null && Boolean.parseBoolean(value.toString());
    }

    private static Map<String, Object> mutable(Map<String, Object> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            Map<String, Object> nested = value instanceof Map<?, ?> ? child(source, key) : null;
            result.put(key, nested == null ? value : mutable(nested));
        });
        return result;
    }

    private static Map<String, Object> immutable(Map<String, Object> source) {
        Map<String, Object> result = mutable(source);
        result.replaceAll((key, value) -> value instanceof Map<?, ?> ? immutable(child(result, key)) : value);
        return Collections.unmodifiableMap(result);
    }

    private static void merge(Map<String, Object> target, Map<String, Object> override) {
        override.forEach((key, value) -> {
            Map<String, Object> baseChild = child(target, key);
            Map<String, Object> overrideChild = child(override, key);
            if (baseChild != null && overrideChild != null) {
                merge(baseChild, overrideChild);
                target.put(key, baseChild);
            } else {
                target.put(key, overrideChild == null ? value : mutable(overrideChild));
            }
        });
    }
}
