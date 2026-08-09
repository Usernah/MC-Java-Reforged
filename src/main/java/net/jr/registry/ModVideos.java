package net.jr.registry;

import net.jr.api.client.video.IVideoRegistryProvider;
import net.jr.api.client.video.VideoHolder;
import net.jr.api.client.video.VideoRegister;

import static net.jr.Java_reforged.MODID;

public final class ModVideos implements IVideoRegistryProvider {
    public static final ModVideos INSTANCE = new ModVideos();
    public static final VideoRegister REGISTRY = VideoRegister.create(MODID);

    public static final VideoHolder LEGACY_BACKGROUND = REGISTRY.register("videos/backgrounds/legacy.mp4");
    public static final VideoHolder NEW_MENU = REGISTRY.register("videos/backgrounds/farm.mp4");

    private ModVideos() {
    }

    public static void bootstrap() {
    }

    @Override
    public VideoRegister getVideoRegister() {
        return REGISTRY;
    }
}
