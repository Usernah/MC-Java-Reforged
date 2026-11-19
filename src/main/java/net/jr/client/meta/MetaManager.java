package net.jr.client.meta;

import net.jr.Java_reforged;
import net.jr.api.client.resource.Asset;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class MetaManager {
    private static final String[] SUFFIXES = {".meta", ".genxmeta"};
    private static final Asset RELOAD_LISTENER_ID = Asset.MOD("reload/metadata");
    private static final MetaManager INSTANCE = new MetaManager();
    private final Map<Asset, Optional<Meta>> cache = new HashMap<>();
    private final Map<Asset, Optional<Meta>> explicitCache = new HashMap<>();

    private MetaManager() {}

    public static MetaManager getInstance() {
        return INSTANCE;
    }

    public Optional<Meta> getMeta(Asset texture) {
        return cache.computeIfAbsent(texture, this::load);
    }

    public Optional<Meta> getMetaResource(Asset metaResource) {
        return explicitCache.computeIfAbsent(metaResource, this::loadResource);
    }

    public void clearCache() {
        cache.clear();
        explicitCache.clear();
    }

    public static void registerClientReloadListener(AddClientReloadListenersEvent event) {
        RELOAD_LISTENER_ID.registerReloadListener(event, new ReloadListener());
    }

    private Optional<Meta> load(Asset texture) {
        for (String suffix : SUFFIXES) {
            Asset metaResource = texture.withPathSuffix(suffix);
            Optional<Meta> meta = this.loadResource(metaResource);
            if (meta.isPresent()) {
                return meta;
            }
        }
        return Optional.empty();
    }

    private Optional<Meta> loadResource(Asset metaResource) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getResourceManager() == null) return Optional.empty();
        try {
            Optional<Resource> resource = metaResource.find(minecraft.getResourceManager());
            if (resource.isEmpty()) return Optional.empty();
            try (Reader reader = new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8)) {
                return Optional.of(MetaParser.parse(reader));
            }
        } catch (Exception exception) {
            Java_reforged.LOGGER.error("Invalid metadata {}", metaResource, exception);
            return Optional.empty();
        }
    }

    private static final class ReloadListener extends SimplePreparableReloadListener<List<String>> {
        @Override
        protected List<String> prepare(ResourceManager manager, ProfilerFiller profiler) {
            return List.of();
        }

        @Override
        protected void apply(List<String> ignored, ResourceManager manager, ProfilerFiller profiler) {
            INSTANCE.clearCache();
            Asset.clearDynamicTextureCache();
        }
    }
}
