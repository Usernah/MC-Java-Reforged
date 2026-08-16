package net.jr.client.runtime.render.target;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import net.jr.client.runtime.viewport.ViewportArea;
import net.jr.client.runtime.viewport.ViewportRenderScope;
import org.jspecify.annotations.Nullable;

/**
 * Owns only the render surfaces required to execute one complete vanilla frame
 * graph per local player. The GameRenderer, LevelRenderer and terrain engines
 * remain global and are never duplicated.
 */
public final class ViewportRenderTargets {
    private static final Map<Integer, Targets> TARGETS = new HashMap<>();
    private static final ThreadLocal<Targets> ACTIVE = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> PRESENTING = ThreadLocal.withInitial(() -> false);

    private ViewportRenderTargets() {
    }

    public static Scope enter(int slotId, ViewportArea viewport, RenderTarget globalTarget) {
        RenderSystem.assertOnRenderThread();
        Objects.requireNonNull(viewport, "viewport");
        Objects.requireNonNull(globalTarget, "globalTarget");

        Targets targets = TARGETS.computeIfAbsent(slotId, Targets::new);
        targets.ensureSize(viewport.width(), viewport.height(), globalTarget.useStencil);
        Targets previous = ACTIVE.get();
        ACTIVE.set(targets);
        return new Scope(previous, targets, viewport, globalTarget);
    }

    public static RenderTarget activeMainOr(RenderTarget fallback) {
        Targets targets = ACTIVE.get();
        return targets == null ? fallback : targets.main;
    }

    public static RenderTarget activeOutlineOr(RenderTarget fallback) {
        Targets targets = ACTIVE.get();
        return targets == null ? fallback : targets.outline;
    }

    public static @Nullable RenderTarget activeMainOrNull() {
        Targets targets = ACTIVE.get();
        return targets == null ? null : targets.main;
    }

    public static boolean isPresenting() {
        return PRESENTING.get();
    }

    public static void closeAll() {
        RenderSystem.assertOnRenderThread();
        ACTIVE.remove();
        TARGETS.values().forEach(Targets::destroy);
        TARGETS.clear();
    }

    private static final class Targets {
        private final int slotId;
        private @Nullable TextureTarget main;
        private @Nullable TextureTarget outline;
        private boolean stencil;

        private Targets(int slotId) {
            this.slotId = slotId;
        }

        private void ensureSize(int width, int height, boolean useStencil) {
            if (this.main == null || this.outline == null || this.stencil != useStencil) {
                this.destroy();
                this.stencil = useStencil;
                this.main = new TextureTarget(
                    "Split slot " + this.slotId + " main",
                    width,
                    height,
                    true,
                    useStencil,
                    GpuFormat.RGBA8_UNORM
                );
                this.outline = new TextureTarget("Split slot " + this.slotId + " outline", width, height, true, GpuFormat.RGBA8_UNORM);
                return;
            }

            if (this.main.width != width || this.main.height != height) {
                this.main.resize(width, height);
            }
            if (this.outline.width != width || this.outline.height != height) {
                this.outline.resize(width, height);
            }
        }

        private void destroy() {
            if (this.main != null) {
                this.main.destroyBuffers();
                this.main = null;
            }
            if (this.outline != null) {
                this.outline.destroyBuffers();
                this.outline = null;
            }
        }
    }

    public static final class Scope implements AutoCloseable {
        private final @Nullable Targets previous;
        private final Targets targets;
        private final ViewportArea viewport;
        private final RenderTarget globalTarget;
        private boolean closed;

        private Scope(@Nullable Targets previous, Targets targets, ViewportArea viewport, RenderTarget globalTarget) {
            this.previous = previous;
            this.targets = targets;
            this.viewport = viewport;
            this.globalTarget = globalTarget;
        }

        /** Copies the completed slot frame once, after every vanilla pass has finished. */
        public void present() {
            RenderPass.RenderArea area = ViewportRenderScope.areaFor(this.viewport, this.globalTarget.width, this.globalTarget.height);
            int width = Math.min(Objects.requireNonNull(this.targets.main).width, area.width());
            int height = Math.min(this.targets.main.height, area.height());
            PRESENTING.set(true);
            try {
                RenderSystem.getDevice()
                    .createCommandEncoder()
                    .copyTextureToTexture(
                        Objects.requireNonNull(this.targets.main.getColorTexture()),
                        Objects.requireNonNull(this.globalTarget.getColorTexture()),
                        0,
                        area.x(),
                        area.y(),
                        0,
                        0,
                        width,
                        height
                    );
            } finally {
                PRESENTING.set(false);
            }
        }

        @Override
        public void close() {
            if (this.closed) {
                return;
            }
            this.closed = true;
            if (this.previous == null) {
                ACTIVE.remove();
            } else {
                ACTIVE.set(this.previous);
            }
        }
    }

}
