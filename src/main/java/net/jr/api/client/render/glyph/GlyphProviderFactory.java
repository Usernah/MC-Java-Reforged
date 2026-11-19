package net.jr.api.client.render.glyph;

import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;

import java.util.Map;

@FunctionalInterface
public interface GlyphProviderFactory {
    GlyphProvider create(Identifier providerId, Map<String, JsonObject> definitions);
}
