package net.jr.api.client.resource;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.FontDescription;
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

public class Asset {
    private static final String MOD_ID = "java_reforged";
    private static final Map<String, Asset> DYNAMIC_TEXTURE_CACHE = new ConcurrentHashMap<>();

    private final Identifier location;
    @Nullable
    private final Object metadata;

    public Asset(Identifier location, @Nullable Object metadata) {
        this.location = Objects.requireNonNull(location, "location");
        this.metadata = metadata;
    }

    public Asset(Identifier location) {
        this(location, null);
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

    @Nullable
    public <T> T getMetadata(Class<T> type) {
        return metadata != null && type.isInstance(metadata) ? type.cast(metadata) : null;
    }

    public static Asset MOD(String path) {
        return new Asset(Identifier.fromNamespaceAndPath(MOD_ID, path));
    }

    public static Asset MC(String path) {
        return new Asset(Identifier.withDefaultNamespace(path));
    }

    public static Asset NamespaceAndPatch(String NameSpace, String Patch) {
        return new Asset(Identifier.fromNamespaceAndPath(NameSpace, Patch));
    }

    public static Asset PARSE(String fullPath) {
        return new Asset(Identifier.parse(fullPath));
    }

    public Asset child(String childPath) {
        Objects.requireNonNull(childPath, "childPath");
        String normalized = childPath.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("childPath cannot be blank");
        }

        String parentPath = location.getPath();
        String combinedPath = parentPath.endsWith("/")
            ? parentPath + normalized
            : parentPath + "/" + normalized;
        return new Asset(Identifier.fromNamespaceAndPath(location.getNamespace(), combinedPath));
    }

    public Asset withPathSuffix(String suffix) {
        Objects.requireNonNull(suffix, "suffix");
        if (suffix.isBlank()) {
            throw new IllegalArgumentException("suffix cannot be blank");
        }
        return new Asset(
            Identifier.fromNamespaceAndPath(
                location.getNamespace(),
                location.getPath() + suffix
            )
        );
    }

    public Optional<Resource> find(ResourceManager manager) {
        Objects.requireNonNull(manager, "manager");
        return manager.getResource(location);
    }

    public Resource require(ResourceManager manager) {
        return find(manager).orElseThrow(
            () -> new IllegalStateException("Missing required asset: " + this)
        );
    }

    public InputStream open(ResourceManager manager) throws IOException {
        return require(manager).open();
    }

    public void registerReloadListener(
        AddClientReloadListenersEvent event,
        PreparableReloadListener listener
    ) {
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(listener, "listener");
        event.addListener(location, listener);
    }

    public FontDescription.Resource asFontDescription() {
        return new FontDescription.Resource(location);
    }

    public Optional<InputStream> getAsStream() {
        try {
            Optional<Resource> resource = find(Minecraft.getInstance().getResourceManager());
            if (resource.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(resource.get().open());
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    public Identifier res() {
        return location;
    }

    public String namespace() {
        return location.getNamespace();
    }

    public String path() {
        return location.getPath();
    }

    public static void clearDynamicTextureCache() {
        DYNAMIC_TEXTURE_CACHE.clear();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Asset that)) {
            return false;
        }
        return location.equals(that.location) && Objects.equals(metadata, that.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(location, metadata);
    }

    @Override
    public String toString() {
        return location.toString();
    }
}
