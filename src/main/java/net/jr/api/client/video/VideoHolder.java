package net.jr.api.client.video;

import net.jr.api.client.resource.Asset;
import net.jr.client.video.VideoResource;

import java.io.File;

public final class VideoHolder {
    private final VideoResource resource;

    VideoHolder(VideoResource resource) {
        this.resource = resource;
    }

    public String modId() {
        return this.resource.modId();
    }

    public String path() {
        return this.resource.path();
    }

    public Asset location() {
        return this.resource.location();
    }

    public Asset loader() {
        return this.resource.loader();
    }

    public String debugId() {
        return this.resource.debugId();
    }

    public File resolveCachedFile() {
        return this.resource.resolveCachedFile();
    }

    public File resolveCachedFile(boolean forceExtract) {
        return this.resource.resolveCachedFile(forceExtract);
    }

    @Override
    public boolean equals(Object object) {
        return this == object
                || object instanceof VideoHolder that && this.resource.equals(that.resource);
    }

    @Override
    public int hashCode() {
        return this.resource.hashCode();
    }

    @Override
    public String toString() {
        return "VideoHolder[" + this.location() + "]";
    }
}
