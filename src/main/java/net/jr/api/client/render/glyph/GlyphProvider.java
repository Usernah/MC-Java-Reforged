package net.jr.api.client.render.glyph;

import java.util.Map;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface GlyphProvider {
    @Nullable
    GlyphDefinition resolve(String id);

    @Nullable
    default GlyphDefinition resolve(String id, Map<String, String> arguments) {
        return this.resolve(id);
    }
}
