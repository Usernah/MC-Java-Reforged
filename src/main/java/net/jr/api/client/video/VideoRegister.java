package net.jr.api.client.video;

import net.jr.api.client.resource.Asset;
import net.jr.client.video.VideoRegistryStore;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;

import java.util.Collection;
import java.util.Optional;

public final class VideoRegister {
    private final VideoRegistryStore store;

    private VideoRegister(VideoRegistryStore store) {
        this.store = store;
    }

    public static VideoRegister create(String modId) {
        return new VideoRegister(VideoRegistryStore.create(modId));
    }

    public String modId() {
        return this.store.modId();
    }

    public VideoHolder register(String resourcePath) {
        return new VideoHolder(this.store.register(resourcePath));
    }

    public Optional<VideoHolder> get(String resourcePath) {
        return this.store.get(resourcePath).map(VideoHolder::new);
    }

    public Collection<VideoHolder> entries() {
        return this.store.entries().stream().map(VideoHolder::new).toList();
    }

    public static Collection<VideoHolder> allRegisteredVideos() {
        return VideoRegistryStore.allRegisteredVideos().stream().map(VideoHolder::new).toList();
    }

    public static Optional<VideoHolder> find(Asset asset) {
        return VideoRegistryStore.find(asset).map(VideoHolder::new);
    }

    public static VideoHolder require(Asset asset) {
        return new VideoHolder(VideoRegistryStore.require(asset));
    }

    public static int synchronizeAll(boolean forceExtract) {
        return VideoRegistryStore.synchronizeAll(forceExtract);
    }

    public static void registerClientReloadListener(AddClientReloadListenersEvent event) {
        VideoRegistryStore.registerClientReloadListener(event);
    }
}
