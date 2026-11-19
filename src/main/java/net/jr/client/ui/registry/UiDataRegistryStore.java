package net.jr.client.ui.registry;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class UiDataRegistryStore {
    private static final Object LOCK = new Object();
    private static final Map<String, UiDataRegistryStore> REGISTRIES = new LinkedHashMap<>();

    private final String modId;
    private final Map<String, Entry> entries = new LinkedHashMap<>();

    private UiDataRegistryStore(String modId) {
        this.modId = requireName(modId, "mod ID");
    }

    public static UiDataRegistryStore create(String modId) {
        synchronized (LOCK) {
            return REGISTRIES.computeIfAbsent(modId, UiDataRegistryStore::new);
        }
    }

    public String modId() {
        return this.modId;
    }

    public Entry register(String name) {
        String normalizedName = requireName(name, "data name");
        synchronized (LOCK) {
            Entry existing = this.entries.get(normalizedName);
            if (existing != null) {
                return existing;
            }
            Entry entry = new Entry(this.modId, normalizedName);
            this.entries.put(normalizedName, entry);
            return entry;
        }
    }

    public Optional<Entry> get(String name) {
        synchronized (LOCK) {
            return Optional.ofNullable(this.entries.get(requireName(name, "data name")));
        }
    }

    public Collection<Entry> entries() {
        synchronized (LOCK) {
            return List.copyOf(this.entries.values());
        }
    }

    public static Collection<Entry> allRegisteredFiles() {
        synchronized (LOCK) {
            return REGISTRIES.values().stream()
                .flatMap(registry -> registry.entries.values().stream())
                .toList();
        }
    }

    private static String requireName(String value, String description) {
        Objects.requireNonNull(value, description);
        String normalized = value.trim();
        if (normalized.isEmpty() || normalized.indexOf('/') >= 0
            || normalized.indexOf('\\') >= 0 || normalized.endsWith(".data")) {
            throw new IllegalArgumentException("Invalid UI " + description + ": " + value);
        }
        return normalized;
    }

    public record Entry(String modId, String name) {
    }
}
