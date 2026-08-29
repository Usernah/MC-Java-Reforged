package net.jr.client.resource;

import com.mojang.blaze3d.platform.NativeImage;
import net.jr.api.client.resource.Asset;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class ClientAssetManager {
    private static final String MOD_ID = "java_reforged";
    private static final Map<String, Asset> DYNAMIC_TEXTURE_CACHE = new ConcurrentHashMap<>();

    private ClientAssetManager() {
    }

    @Nullable
    public static Asset fromFile(File imageFile) {
        if (imageFile == null || !imageFile.isFile()) {
            return null;
        }
        return loadFromStream(
                imageFile.getAbsolutePath(),
                imageFile.getName(),
                () -> {
                    try {
                        return new FileInputStream(imageFile);
                    } catch (IOException exception) {
                        throw new IllegalStateException(exception);
                    }
                }
        );
    }

    @Nullable
    public static Asset fromInternal(String internalPath) {
        if (internalPath == null || internalPath.isBlank()) {
            return null;
        }
        return loadFromStream(
                internalPath,
                internalPath,
                () -> Asset.class.getResourceAsStream(internalPath)
        );
    }

    @Nullable
    public static Asset fromStream(
            String cacheKey,
            String resourceIdentifier,
            Supplier<InputStream> inputStreamSupplier
    ) {
        if (cacheKey == null || cacheKey.isBlank() || inputStreamSupplier == null) {
            return null;
        }
        String identifier = resourceIdentifier == null || resourceIdentifier.isBlank()
                ? cacheKey
                : resourceIdentifier;
        return loadFromStream(cacheKey, identifier, inputStreamSupplier);
    }

    public static Optional<Resource> find(Asset asset, ResourceManager manager) {
        Objects.requireNonNull(manager, "manager");
        return manager.getResource(asset.res());
    }

    public static Resource require(Asset asset, ResourceManager manager) {
        return find(asset, manager).orElseThrow(
                () -> new IllegalStateException("Missing required asset: " + asset)
        );
    }

    public static InputStream open(Asset asset, ResourceManager manager) throws IOException {
        return require(asset, manager).open();
    }

    public static void registerReloadListener(
            Asset asset,
            AddClientReloadListenersEvent event,
            PreparableReloadListener listener
    ) {
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(listener, "listener");
        event.addListener(asset.res(), listener);
    }

    public static Optional<InputStream> getAsStream(Asset asset) {
        try {
            Optional<Resource> resource = find(asset, Minecraft.getInstance().getResourceManager());
            if (resource.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(resource.get().open());
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    public static void clearDynamicTextureCache() {
        DYNAMIC_TEXTURE_CACHE.clear();
    }

    @Nullable
    private static Asset loadFromStream(
            String cacheKey,
            String resourceIdentifier,
            Supplier<InputStream> inputStreamSupplier
    ) {
        Asset cached = DYNAMIC_TEXTURE_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        try (InputStream inputStream = inputStreamSupplier.get()) {
            if (inputStream == null) {
                return null;
            }

            NativeImage image = NativeImage.read(inputStream);
            DynamicTexture texture = new DynamicTexture(() -> resourceIdentifier, image);
            String cleanName = resourceIdentifier.toLowerCase().replaceAll("[^a-z0-9/._-]", "_");
            Identifier identifier = Identifier.fromNamespaceAndPath(MOD_ID, "dynamic/" + cleanName);
            Minecraft.getInstance().getTextureManager().register(identifier, texture);

            Asset loaded = new Asset(identifier);
            Asset previous = DYNAMIC_TEXTURE_CACHE.putIfAbsent(cacheKey, loaded);
            if (previous != null) {
                Minecraft.getInstance().getTextureManager().release(identifier);
                return previous;
            }
            return loaded;
        } catch (Exception exception) {
            return null;
        }
    }
}
