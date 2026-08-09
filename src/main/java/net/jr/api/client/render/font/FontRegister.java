package net.jr.api.client.render.font;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class FontRegister {
    private static final Object REGISTRY_LOCK = new Object();
    private static final Map<String, FontRegister> REGISTRIES = new LinkedHashMap<>();
    private static final Map<String, FontHolder> ALL_FONTS = new LinkedHashMap<>();

    private final String modId;
    private final Map<String, FontHolder> entries = new LinkedHashMap<>();

    private FontRegister(String modId) {
        this.modId = modId;
    }

    public static FontRegister create(String modId) {
        synchronized (REGISTRY_LOCK) {
            return REGISTRIES.computeIfAbsent(modId, FontRegister::new);
        }
    }

    public String modId() {
        return modId;
    }

    public FontHolder register(String fontPath) {
        synchronized (REGISTRY_LOCK) {
            return entries.computeIfAbsent(fontPath, path -> {
                FontHolder holder = new FontHolder(modId, path);
                ALL_FONTS.put(holder.location().toString(), holder);
                return holder;
            });
        }
    }

    public Optional<FontHolder> get(String fontPath) {
        synchronized (REGISTRY_LOCK) {
            return Optional.ofNullable(entries.get(fontPath));
        }
    }

    public Collection<FontHolder> entries() {
        synchronized (REGISTRY_LOCK) {
            return List.copyOf(entries.values());
        }
    }

    public static Collection<FontHolder> allRegisteredFonts() {
        synchronized (REGISTRY_LOCK) {
            return List.copyOf(ALL_FONTS.values());
        }
    }
}
