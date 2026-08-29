package net.jr.api.client.render.font;

import net.jr.client.render.font.FontRegistryStore;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;

import java.util.Collection;
import java.util.Optional;

public final class FontRegister {
    private final FontRegistryStore store;

    private FontRegister(FontRegistryStore store) {
        this.store = store;
    }

    public static FontRegister create(String modId) {
        return new FontRegister(FontRegistryStore.create(modId));
    }

    public String modId() {
        return this.store.modId();
    }

    public FontHolder register(String fontPath) {
        return new FontHolder(this.store.register(fontPath));
    }

    public Optional<FontHolder> get(String fontPath) {
        return this.store.get(fontPath).map(FontHolder::new);
    }

    public Collection<FontHolder> entries() {
        return this.store.entries().stream().map(FontHolder::new).toList();
    }

    public static Collection<FontHolder> allRegisteredFonts() {
        return FontRegistryStore.allRegisteredFonts().stream().map(FontHolder::new).toList();
    }

    public static void registerClientReloadListener(AddClientReloadListenersEvent event) {
        FontRegistryStore.registerClientReloadListener(event);
    }
}
