package net.jr.client.ui.dsl;

import net.jr.api.client.resource.Asset;
import net.jr.api.client.ui.dsl.UiCompiledDocument;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public final class UiDataContext implements UiCompiledDocument.DataContext {
    private final Map<List<String>, Supplier<?>> values = new LinkedHashMap<>();

    public void exposeNumber(String path, Supplier<? extends Number> value) {
        this.expose(path, value);
    }

    public void exposeBoolean(String path, Supplier<Boolean> value) {
        this.expose(path, value);
    }

    public void exposeText(String path, Supplier<String> value) {
        this.expose(path, value);
    }

    public void exposeAsset(String path, Supplier<Asset> value) {
        this.expose(path, value);
    }

    public void remove(String path) {
        this.values.remove(parsePath(path));
    }

    @Override
    public Object resolve(List<String> path) {
        Supplier<?> supplier = this.values.get(List.copyOf(path));
        if (supplier == null) {
            throw new IllegalArgumentException("UI data is not exposed: ${" + String.join(".", path) + "}");
        }
        return supplier.get();
    }

    private void expose(String path, Supplier<?> value) {
        this.values.put(parsePath(path), Objects.requireNonNull(value, "value"));
    }

    private static List<String> parsePath(String path) {
        Objects.requireNonNull(path, "path");
        List<String> parts = Arrays.stream(path.trim().split("\\."))
            .map(String::trim)
            .toList();
        if (parts.isEmpty() || parts.stream().anyMatch(String::isEmpty) || parts.getFirst().equals("this")) {
            throw new IllegalArgumentException("Invalid exposed UI data path: " + path);
        }
        return parts;
    }
}
