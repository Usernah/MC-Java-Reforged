package net.jr.api.client.ui;

import java.util.List;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.jr.api.client.resource.Asset;
import net.jr.client.ui.dsl.UiDataUsageTracker;

public final class UiDataValues {
    private final UiDataFile file;
    private final BooleanSupplier loaded;
    private final Supplier<Map<String, Object>> values;

    private UiDataValues(
        UiDataFile file,
        BooleanSupplier loaded,
        Supplier<Map<String, Object>> values
    ) {
        this.file = Objects.requireNonNull(file, "file");
        this.loaded = Objects.requireNonNull(loaded, "loaded");
        this.values = Objects.requireNonNull(values, "values");
    }

    public static UiDataValues loaded(UiDataFile file, Map<String, Object> values) {
        Map<String, Object> immutable = immutable(values);
        return new UiDataValues(file, () -> true, () -> immutable);
    }

    public static UiDataValues live(
        UiDataFile file,
        BooleanSupplier loaded,
        Supplier<Map<String, Object>> values
    ) {
        return new UiDataValues(file, loaded, () -> immutable(values.get()));
    }

    public static UiDataValues empty(UiDataFile file) {
        return new UiDataValues(file, () -> false, Map::of);
    }

    public UiDataFile file() {
        return this.file;
    }

    public boolean isLoaded() {
        return this.loaded.getAsBoolean();
    }

    public boolean contains(String id) {
        return this.currentValues().containsKey(id);
    }

    public Set<String> ids() {
        return this.currentValues().keySet();
    }

    public Object value(String id) {
        Map<String, Object> values = this.currentValues();
        if (!values.containsKey(id)) {
            throw new IllegalArgumentException(
                "UI data '" + this.file.id() + "' does not declare '" + id + "'"
            );
        }
        Object value = values.get(id);
        UiDataUsageTracker.record(this.file, id, value);
        return value;
    }

    public <T> T value(String id, Class<T> type) {
        Object value = this.value(id);
        if (!type.isInstance(value)) {
            throw this.typeError(id, type.getSimpleName(), value);
        }
        return type.cast(value);
    }

    public boolean bool(String id) {
        return this.value(id, Boolean.class);
    }

    public String string(String id) {
        return this.value(id, String.class);
    }

    public Number number(String id) {
        return this.value(id, Number.class);
    }

    public int integer(String id) {
        return this.number(id).intValue();
    }

    public float decimal(String id) {
        return this.number(id).floatValue();
    }

    public int color(String id) {
        Object value = this.value(id);
        if (value instanceof Number number) {
            return number.intValue();
        }
        throw this.typeError(id, "Color", value);
    }

    public Asset asset(String id) {
        return this.value(id, Asset.class);
    }

    public List<?> list(String id) {
        return this.value(id, List.class);
    }

    public Map<?, ?> object(String id) {
        return this.value(id, Map.class);
    }

    public Map<String, Object> values() {
        Map<String, Object> values = this.currentValues();
        values.forEach((id, value) -> UiDataUsageTracker.record(this.file, id, value));
        return values;
    }

    private Map<String, Object> currentValues() {
        return Objects.requireNonNull(this.values.get(), "resolved UI data values");
    }

    private static Map<String, Object> immutable(Map<String, Object> values) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    private IllegalArgumentException typeError(String id, String expected, Object value) {
        return new IllegalArgumentException(
            "UI data '" + this.file.id() + "' value '" + id + "' must be " + expected
                + ", got " + (value == null ? "null" : value.getClass().getSimpleName())
        );
    }
}
