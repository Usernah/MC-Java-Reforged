package net.jr.client.render.font;

import net.jr.api.client.resource.Asset;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class FontRegistryStore {
    private static final Object REGISTRY_LOCK = new Object();
    private static final Map<String, FontRegistryStore> REGISTRIES = new LinkedHashMap<>();
    private static final Map<String, FontResource> ALL_FONTS = new LinkedHashMap<>();
    private static final Asset RELOAD_LISTENER_ID = Asset.MOD("reload/fonts");
    private static final Component ATLAS_WARMUP_TEXT = Component.literal(
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 áéíóúÁÉÍÓÚñÑüÜ¿?¡!.,:;+-*/()[]{}<>_=|\\'\"@#$%&~^°"
    );

    private final String modId;
    private final Map<String, FontResource> entries = new LinkedHashMap<>();

    private FontRegistryStore(String modId) {
        this.modId = modId;
    }

    public static FontRegistryStore create(String modId) {
        synchronized (REGISTRY_LOCK) {
            return REGISTRIES.computeIfAbsent(modId, FontRegistryStore::new);
        }
    }

    public String modId() {
        return modId;
    }

    public FontResource register(String fontPath) {
        synchronized (REGISTRY_LOCK) {
            return entries.computeIfAbsent(fontPath, path -> {
                FontResource holder = new FontResource(modId, path);
                ALL_FONTS.put(holder.location().toString(), holder);
                return holder;
            });
        }
    }

    public Optional<FontResource> get(String fontPath) {
        synchronized (REGISTRY_LOCK) {
            return Optional.ofNullable(entries.get(fontPath));
        }
    }

    public Collection<FontResource> entries() {
        synchronized (REGISTRY_LOCK) {
            return List.copyOf(entries.values());
        }
    }

    public static Collection<FontResource> allRegisteredFonts() {
        synchronized (REGISTRY_LOCK) {
            return List.copyOf(ALL_FONTS.values());
        }
    }

    public static void registerClientReloadListener(AddClientReloadListenersEvent event) {
        RELOAD_LISTENER_ID.registerReloadListener(event, new ReloadListener());
    }

    private static final class ReloadListener extends SimplePreparableReloadListener<Collection<FontResource>> {
        @Override
        protected Collection<FontResource> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
            return allRegisteredFonts();
        }

        @Override
        protected void apply(Collection<FontResource> fonts, ResourceManager resourceManager, ProfilerFiller profiler) {
            Font font = Minecraft.getInstance().font;
            for (FontResource registeredFont : fonts) {
                font.prepareText(registeredFont.apply(ATLAS_WARMUP_TEXT).getVisualOrderText(), 0.0F, 0.0F, -1, false, false, 0);
            }
        }
    }
}
