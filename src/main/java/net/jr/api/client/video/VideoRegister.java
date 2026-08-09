package net.jr.api.client.video;

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

public final class VideoRegister {
    private static final Object REGISTRY_LOCK = new Object();
    private static final Map<String, VideoRegister> REGISTRIES = new LinkedHashMap<>();
    private static final Map<String, VideoHolder> ALL_VIDEOS = new LinkedHashMap<>();
    private static final Asset RELOAD_LISTENER_ID = Asset.MOD("reload/videos");
    private static final ClientVideoReloadListener RELOAD_LISTENER = new ClientVideoReloadListener();

    private final String modId;
    private final Map<String, VideoHolder> entries = new LinkedHashMap<>();

    private VideoRegister(String modId) {
        this.modId = modId;
    }

    public static VideoRegister create(String modId) {
        synchronized (REGISTRY_LOCK) {
            return REGISTRIES.computeIfAbsent(modId, VideoRegister::new);
        }
    }

    public String modId() {
        return modId;
    }

    public VideoHolder register(String resourcePath) {
        synchronized (REGISTRY_LOCK) {
            return entries.computeIfAbsent(resourcePath, path -> {
                VideoHolder holder = new VideoHolder(modId, path);
                ALL_VIDEOS.put(holder.location().toString(), holder);
                return holder;
            });
        }
    }

    public Optional<VideoHolder> get(String resourcePath) {
        synchronized (REGISTRY_LOCK) {
            return Optional.ofNullable(entries.get(resourcePath));
        }
    }

    public Collection<VideoHolder> entries() {
        synchronized (REGISTRY_LOCK) {
            return List.copyOf(entries.values());
        }
    }

    public static Collection<VideoHolder> allRegisteredVideos() {
        synchronized (REGISTRY_LOCK) {
            return List.copyOf(ALL_VIDEOS.values());
        }
    }

    public static Optional<VideoHolder> find(Asset asset) {
        synchronized (REGISTRY_LOCK) {
            return Optional.ofNullable(ALL_VIDEOS.get(asset.toString()));
        }
    }

    public static VideoHolder require(Asset asset) {
        return find(asset).orElseThrow(() ->
            new IllegalArgumentException("Video asset is not registered: " + asset)
        );
    }

    public static int synchronizeAll(boolean forceExtract) {
        int synchronizedCount = 0;
        for (VideoHolder holder : allRegisteredVideos()) {
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

    private static final class ClientVideoReloadListener extends SimplePreparableReloadListener<List<VideoHolder>> {
        @Override
        protected List<VideoHolder> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
            return List.copyOf(VideoRegister.allRegisteredVideos());
        }

        @Override
        protected void apply(List<VideoHolder> holders, ResourceManager resourceManager, ProfilerFiller profiler) {
            for (VideoHolder holder : holders) {
                holder.resolveCachedFile(false);
            }
        }
    }
}
