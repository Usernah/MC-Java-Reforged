package net.jr.client.ui.hint.render;

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

        try (InputStream inputStream = texture.open(minecraft.getResourceManager());
             NativeImage image = NativeImage.read(inputStream)) {
            int width = image.getWidth();
            int height = image.getHeight();

            int minX = width;
            int minY = height;
            int maxX = -1;
            int maxY = -1;

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int alpha = image.getPixel(x, y) >>> 24;
                    if (alpha <= 0) {
                        continue;
                    }

                    if (x < minX) {
                        minX = x;
                    }
                    if (x > maxX) {
                        maxX = x;
                    }
                    if (y < minY) {
                        minY = y;
                    }
                    if (y > maxY) {
                        maxY = y;
                    }
                }
            }

            if (maxX < minX || maxY < minY) {
                return GlyphTextureBounds.full(width, height);
            }

            return new GlyphTextureBounds(width, height, minX, maxX, minY, maxY);
        } catch (Exception ignored) {
            return GlyphTextureBounds.full(1, 1);
        }
    }
}
