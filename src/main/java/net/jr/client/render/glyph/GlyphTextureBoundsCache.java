package net.jr.client.render.glyph;

import com.mojang.blaze3d.platform.NativeImage;
import net.jr.api.client.resource.Asset;
import net.minecraft.client.Minecraft;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class GlyphTextureBoundsCache {
    private static final Map<Asset, GlyphTextureBounds> CACHE = new ConcurrentHashMap<>();

    private GlyphTextureBoundsCache() {
    }

    public static GlyphTextureBounds get(Asset texture) {
        return CACHE.computeIfAbsent(texture, GlyphTextureBoundsCache::load);
    }

    public static void clear() {
        CACHE.clear();
    }

    private static GlyphTextureBounds load(Asset texture) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return GlyphTextureBounds.full(1, 1);
        }

        try (InputStream stream = texture.open(minecraft.getResourceManager());
             NativeImage image = NativeImage.read(stream)) {
            int width = image.getWidth();
            int height = image.getHeight();
            int minX = width;
            int minY = height;
            int maxX = -1;
            int maxY = -1;

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if ((image.getPixel(x, y) >>> 24) <= 0) {
                        continue;
                    }
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }

            return maxX < minX || maxY < minY
                ? GlyphTextureBounds.full(width, height)
                : new GlyphTextureBounds(width, height, minX, maxX, minY, maxY);
        } catch (Exception ignored) {
            return GlyphTextureBounds.full(1, 1);
        }
    }
}
