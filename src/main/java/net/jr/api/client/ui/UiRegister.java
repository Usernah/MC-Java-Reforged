package net.jr.api.client.ui;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class UiRegister {
    private static final Object REGISTRY_LOCK = new Object();
    private static final Map<String, UiRegister> REGISTRIES = new LinkedHashMap<>();

    private final String modId;
    private final Map<Key, UiFile> entries = new LinkedHashMap<>();

    private UiRegister(String modId) {
        this.modId = Objects.requireNonNull(modId, "modId");
    }

    public static UiRegister create(String modId) {
        synchronized (REGISTRY_LOCK) {
            return REGISTRIES.computeIfAbsent(modId, UiRegister::new);
        }
    }

    public String modId() {
        return this.modId;
    }

    public UiScreenFile registerScreen(String path) {
        return (UiScreenFile)this.register(UiFileType.SCREEN, path);
    }

    public UiContainerFile registerContainer(String path) {
        return (UiContainerFile)this.register(UiFileType.CONTAINER, path);
    }

    public UiOverlayFile registerOverlay(String path) {
        return (UiOverlayFile)this.register(UiFileType.OVERLAY, path);
    }

    private UiFile register(UiFileType type, String path) {
        Objects.requireNonNull(type, "type");
        String normalizedPath = normalizePath(path);
        Key key = new Key(type, normalizedPath);
        synchronized (REGISTRY_LOCK) {
            return this.entries.computeIfAbsent(key, ignored -> createFile(this.modId, normalizedPath, type));
        }
    }

    public Optional<UiFile> get(UiFileType type, String path) {
        Key key = new Key(Objects.requireNonNull(type, "type"), normalizePath(path));
        synchronized (REGISTRY_LOCK) {
            return Optional.ofNullable(this.entries.get(key));
        }
    }

    public Collection<UiFile> entries() {
        synchronized (REGISTRY_LOCK) {
            return List.copyOf(this.entries.values());
        }
    }

    public static Collection<UiFile> allRegisteredFiles() {
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

    private static UiFile createFile(String modId, String path, UiFileType type) {
        return switch (type) {
            case SCREEN -> new UiScreenFile(modId, path);
            case CONTAINER -> new UiContainerFile(modId, path);
            case OVERLAY -> new UiOverlayFile(modId, path);
            case STYLE -> throw new IllegalArgumentException(
                "Style files are resolved through imports and cannot be registered"
            );
        };
    }

    private record Key(UiFileType type, String path) {
    }
}
