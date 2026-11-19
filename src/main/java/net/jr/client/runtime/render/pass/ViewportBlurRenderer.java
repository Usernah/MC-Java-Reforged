package net.jr.client.runtime.render.pass;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import net.jr.client.runtime.render.state.ViewportBlurState;
import net.jr.client.runtime.render.target.TextureCopyBounds;
import net.jr.mixin.accessors.GameRendererAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.Identifier;

public final class ViewportBlurRenderer implements AutoCloseable {
    private static final Identifier BLUR = Identifier.withDefaultNamespace("blur");
    private final Map<Integer, TextureTarget> targets = new HashMap<>();

    public void prepareFrame(ViewportBlurState state) {
        RenderSystem.assertOnRenderThread();
        this.targets.entrySet().removeIf(entry -> {
            if (state.contains(entry.getKey())) return false;
            entry.getValue().destroyBuffers();
            return true;
        });
    }

    public void render(GameRenderer renderer, ViewportBlurState state) {
        if (!state.isLocal()) {
            renderer.processBlurEffect();
            return;
        }
        PostChain chain = Minecraft.getInstance().getShaderManager().getPostChain(BLUR, LevelTargetBundle.MAIN_TARGETS);
        if (chain == null) return;

        RenderTarget main = renderer.mainRenderTarget();
        var mainColor = Objects.requireNonNull(main.getColorTexture());
        var pool = ((GameRendererAccessor)renderer).javaReforged$getResourcePool();
        for (ViewportBlurState.Region requested : state.regions()) {
            ViewportBlurState.Region region = requested.clipTo(main.width, main.height);
            if (region.isEmpty()) continue;

            TextureTarget target = this.targets.get(region.id());
            if (target != null && Objects.requireNonNull(target.getColorTexture()).getFormat() != mainColor.getFormat()) {
                target.destroyBuffers();
                this.targets.remove(region.id());
                target = null;
            }
            if (target == null) {
                target = new TextureTarget("Viewport " + region.id() + " menu blur", region.width(), region.height(), false, mainColor.getFormat());
                this.targets.put(region.id(), target);
            } else if (target.width != region.width() || target.height != region.height()) {
                target.resize(region.width(), region.height());
            }

            var color = Objects.requireNonNull(target.getColorTexture());

            TextureCopyBounds.copyRegion(mainColor, color, 0, 0, region.x(), region.y(), region.width(), region.height());
            chain.process(target, pool);
            TextureCopyBounds.copyRegion(color, mainColor, region.x(), region.y(), 0, 0, region.width(), region.height());
        }
    }

    @Override
    public void close() {
        RenderSystem.assertOnRenderThread();
        this.targets.values().forEach(RenderTarget::destroyBuffers);
        this.targets.clear();
    }
}
