package net.jr.client.video;

import net.jr.api.client.resource.Asset;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class VideoRegistryStore {
    private static final Object REGISTRY_LOCK = new Object();
    private static final Map<String, VideoRegistryStore> REGISTRIES = new LinkedHashMap<>();
    private static final Map<String, VideoResource> ALL_VIDEOS = new LinkedHashMap<>();
    private static final Asset RELOAD_LISTENER_ID = Asset.MOD("reload/videos");
    private static final ClientVideoReloadListener RELOAD_LISTENER = new ClientVideoReloadListener();

    private final String modId;
    private final Map<String, VideoResource> entries = new LinkedHashMap<>();

    private VideoRegistryStore(String modId) {
        this.modId = modId;
    }

    public static VideoRegistryStore create(String modId) {
        synchronized (REGISTRY_LOCK) {
            return REGISTRIES.computeIfAbsent(modId, VideoRegistryStore::new);
        }
    }

    public String modId() {
        return modId;
    }

    public VideoResource register(String resourcePath) {
        synchronized (REGISTRY_LOCK) {
            return entries.computeIfAbsent(resourcePath, path -> {
                VideoResource holder = new VideoResource(modId, path);
                ALL_VIDEOS.put(holder.location().toString(), holder);
                return holder;
            });
        }
    }

    public Optional<VideoResource> get(String resourcePath) {
        synchronized (REGISTRY_LOCK) {
            return Optional.ofNullable(entries.get(resourcePath));
        }
    }

    public Collection<VideoResource> entries() {
        synchronized (REGISTRY_LOCK) {
            return List.copyOf(entries.values());
        }
    }

    public static Collection<VideoResource> allRegisteredVideos() {
        synchronized (REGISTRY_LOCK) {
            return List.copyOf(ALL_VIDEOS.values());
        }
    }

    public static Optional<VideoResource> find(Asset asset) {
        synchronized (REGISTRY_LOCK) {
            return Optional.ofNullable(ALL_VIDEOS.get(asset.toString()));
        }
    }

    public static VideoResource require(Asset asset) {
        return find(asset).orElseThrow(() ->
            new IllegalArgumentException("Video asset is not registered: " + asset)
        );
    }

    public static int synchronizeAll(boolean forceExtract) {
        int synchronizedCount = 0;
        for (VideoResource holder : allRegisteredVideos()) {
            File resolved = holder.resolveCachedFile(forceExtract);
            if (resolved != null) {
                synchronizedCount++;
            }
        }
        return synchronizedCount;
    }

    public static void registerClientReloadListener(AddClientReloadListenersEvent event) {
        RELOAD_LISTENER_ID.registerReloadListener(event, RELOAD_LISTENER);
    }

    private static final class ClientVideoReloadListener extends SimplePreparableReloadListener<List<VideoResource>> {
        @Override
        protected List<VideoResource> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
            return List.copyOf(VideoRegistryStore.allRegisteredVideos());
        }

        @Override
        protected void apply(List<VideoResource> holders, ResourceManager resourceManager, ProfilerFiller profiler) {
            for (VideoResource holder : holders) {
                holder.resolveCachedFile(false);
            }
        }
    }
}
