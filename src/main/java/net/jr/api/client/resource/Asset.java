package net.jr.api.client.resource;

import net.jr.client.resource.ClientAssetManager;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public class Asset {
    private static final String MOD_ID = "java_reforged";

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
        return ClientAssetManager.fromFile(imageFile);
    }

    @Nullable
    public static Asset fromInternal(String internalPath) {
        return ClientAssetManager.fromInternal(internalPath);
    }

    @Nullable
    public static Asset fromStream(
        String cacheKey,
        String resourceIdentifier,
        Supplier<InputStream> inputStreamSupplier
    ) {
        return ClientAssetManager.fromStream(cacheKey, resourceIdentifier, inputStreamSupplier);
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
        return ClientAssetManager.find(this, manager);
    }

    public Resource require(ResourceManager manager) {
        return ClientAssetManager.require(this, manager);
    }

    public InputStream open(ResourceManager manager) throws IOException {
        return ClientAssetManager.open(this, manager);
    }

    public void registerReloadListener(
        AddClientReloadListenersEvent event,
        PreparableReloadListener listener
    ) {
        ClientAssetManager.registerReloadListener(this, event, listener);
    }

    public FontDescription.Resource asFontDescription() {
        return new FontDescription.Resource(location);
    }

    public Optional<InputStream> getAsStream() {
        return ClientAssetManager.getAsStream(this);
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
        ClientAssetManager.clearDynamicTextureCache();
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
