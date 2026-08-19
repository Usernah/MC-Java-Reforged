package net.jr.api.client.video;

import java.util.Collection;

public interface IVideoRegistryProvider {
    VideoRegister getVideoRegister();

    default Collection<VideoHolder> getRegisteredVideos() {
        return getVideoRegister().entries();
    }
}
