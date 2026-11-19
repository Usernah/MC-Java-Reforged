package net.jr.api.client.render.glyph;

import net.jr.client.render.glyph.GlyphRegistryStore;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;

public final class GlyphProviders {
    private GlyphProviders() {
    }

    public static void register(Identifier providerId, GlyphProviderFactory factory) {
        GlyphRegistryStore.register(providerId, factory);
    }

    public static void registerClientReloadListener(AddClientReloadListenersEvent event) {
        GlyphRegistryStore.registerClientReloadListener(event);
    }
}
