package net.jr.client.ui.registry;

import net.jr.api.client.ui.UiFileType;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class UiRegistryStore {
    private static final Object REGISTRY_LOCK = new Object();
    private static final Map<String, UiRegistryStore> REGISTRIES = new LinkedHashMap<>();

    private final String modId;
    private final Map<Key, UiEntry> entries = new LinkedHashMap<>();

    private UiRegistryStore(String modId) {
        this.modId = Objects.requireNonNull(modId, "modId");
    }

    public static UiRegistryStore create(String modId) {
        synchronized (REGISTRY_LOCK) {
            return REGISTRIES.computeIfAbsent(modId, UiRegistryStore::new);
        }
    }

    public String modId() {
        return this.modId;
    }

    public UiEntry register(UiFileType type, String path) {
        Objects.requireNonNull(type, "type");
        if (type == UiFileType.STYLE || type == UiFileType.DATA) {
            throw new IllegalArgumentException(
                    type == UiFileType.STYLE
                        ? "Style files are resolved through imports and cannot be registered"
                        : "Data files must be registered through UiDataRegister"
            );
        }
        String normalizedPath = normalizePath(path);
        Key key = new Key(type, normalizedPath);
        synchronized (REGISTRY_LOCK) {
            return this.entries.computeIfAbsent(
                    key,
                    ignored -> new UiEntry(this.modId, normalizedPath, type)
            );
        }
    }

    public Optional<UiEntry> get(UiFileType type, String path) {
        Key key = new Key(Objects.requireNonNull(type, "type"), normalizePath(path));
        synchronized (REGISTRY_LOCK) {
            return Optional.ofNullable(this.entries.get(key));
        }
    }

    public Collection<UiEntry> entries() {
        synchronized (REGISTRY_LOCK) {
            return List.copyOf(this.entries.values());
        }
    }

    public static Collection<UiEntry> allRegisteredFiles() {
        synchronized (REGISTRY_LOCK) {
            return REGISTRIES.values().stream()
                .flatMap(registry -> registry.entries.values().stream())
                .toList();
        }
    }

    private static String normalizePath(String path) {
        Objects.requireNonNull(path, "path");
        String normalized = path.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("UI file path cannot be blank");
        }
        if (normalized.indexOf('\\') >= 0) {
            throw new IllegalArgumentException("UI file paths must use '/': " + path);
        }
        if (normalized.startsWith("/") || normalized.endsWith("/")) {
            throw new IllegalArgumentException("UI file paths cannot start or end with '/': " + path);
        }
        for (String segment : normalized.split("/")) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
                throw new IllegalArgumentException("Invalid UI file path: " + path);
            }
        }
        for (UiFileType type : UiFileType.values()) {
            if (normalized.endsWith("." + type.extension())) {
                throw new IllegalArgumentException(
                    "Register UI paths without their extension; received: " + path
                );
            }
        }
        return normalized;
    }

    private record Key(UiFileType type, String path) {
    }

    public record UiEntry(String modId, String path, UiFileType type) {
    }
}
