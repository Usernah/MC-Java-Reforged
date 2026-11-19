package net.jr.client.render.item;

import net.minecraft.client.gui.render.GuiItemAtlas;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class GuiItemAtlasSet implements AutoCloseable {
    private final FeatureRenderDispatcher featureRenderDispatcher;
    private final Map<AtlasKey, GuiItemAtlas> atlases = new HashMap<>();
    private final Map<GuiItemRenderState, GuiItemAtlas> frameAtlases = new IdentityHashMap<>();

    public GuiItemAtlasSet(FeatureRenderDispatcher featureRenderDispatcher) {
        this.featureRenderDispatcher = featureRenderDispatcher;
    }

    public void prepare(GuiRenderState renderState, int guiScale) {
        Map<GuiItemRenderState, ItemRenderResolution.Request> requests =
            ItemRenderResolution.collect(renderState);
        Map<GuiItemRenderState, AtlasKey> stateKeys = new IdentityHashMap<>();
        Map<AtlasKey, Set<Object>> identitiesByAtlas = new LinkedHashMap<>();

        this.frameAtlases.clear();
        renderState.forEachItem(state -> {
            if (state.oversizedItemBounds() != null) {
                return;
            }
            ItemRenderResolution.Request request = ItemRenderResolution.requestFor(requests, state);
            AtlasKey key = new AtlasKey(request.kind(), Math.multiplyExact(request.size(), guiScale));
            stateKeys.put(state, key);
            identitiesByAtlas.computeIfAbsent(key, ignored -> new LinkedHashSet<>())
                .add(state.itemStackRenderState().getModelIdentity());
        });

        this.atlases.entrySet().removeIf(entry -> {
            if (identitiesByAtlas.containsKey(entry.getKey())) {
                return false;
            }
            entry.getValue().close();
            return true;
        });

        identitiesByAtlas.forEach((key, identities) ->
            this.atlases.compute(key, (ignored, atlas) -> this.prepareAtlas(atlas, key, identities))
        );
        stateKeys.forEach((state, key) -> this.frameAtlases.put(state, this.atlases.get(key)));
    }

    public GuiItemAtlas.SlotView getOrUpdate(GuiItemRenderState state) {
        GuiItemAtlas atlas = this.frameAtlases.get(state);
        return atlas == null ? null : atlas.getOrUpdate(state.itemStackRenderState());
    }

    public GuiItemAtlas representative(int vanillaPixelSize) {
        if (!this.atlases.isEmpty()) {
            return this.atlases.values().iterator().next();
        }
        AtlasKey key = new AtlasKey(ItemRenderResolution.Kind.SLOT, vanillaPixelSize);
        return this.atlases.computeIfAbsent(key, ignored -> {
            int textureSize = GuiItemAtlas.computeTextureSizeFor(vanillaPixelSize, 0);
            return new GuiItemAtlas(
                this.featureRenderDispatcher,
                textureSize,
                vanillaPixelSize
            );
        });
    }

    public void endFrame() {
        this.atlases.values().forEach(GuiItemAtlas::endFrame);
        this.frameAtlases.clear();
    }

    private GuiItemAtlas prepareAtlas(
        GuiItemAtlas atlas,
        AtlasKey key,
        Set<Object> identities
    ) {
        if (atlas != null && atlas.tryPrepareFor(identities)) {
            return atlas;
        }
        if (atlas != null) {
            atlas.close();
        }
        int textureSize = GuiItemAtlas.computeTextureSizeFor(key.pixelSize(), identities.size());
        return new GuiItemAtlas(this.featureRenderDispatcher, textureSize, key.pixelSize());
    }

    @Override
    public void close() {
        this.atlases.values().forEach(GuiItemAtlas::close);
        this.atlases.clear();
        this.frameAtlases.clear();
    }

    private record AtlasKey(ItemRenderResolution.Kind kind, int pixelSize) {
    }
}
