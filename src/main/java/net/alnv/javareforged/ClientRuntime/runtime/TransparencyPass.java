package net.alnv.javareforged.ClientRuntime.runtime;

import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import net.alnv.javareforged.ClientRuntime.viewport.ViewportArea;
import net.alnv.javareforged.mixin.SSM.LevelRendererSSAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

public final class TransparencyPass {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation TRANSPARENCY_LOCATION = ResourceLocation.withDefaultNamespace("shaders/post/transparency.json");
    private static final SlotTransparencyChain[] TRANSPARENCY_CHAINS = new SlotTransparencyChain[Client.MAX_CLIENTS];

    private TransparencyPass() {
    }

    public static void prepare(LevelRenderer levelRenderer) {
        LevelRendererSSAccessor accessor = (LevelRendererSSAccessor)levelRenderer;
        if (!Minecraft.useShaderTransparency()) {
            clearInstalled(accessor);
            return;
        }

        LocalClient client = Client.currentOrNull();
        ViewportArea viewport = ViewportPass.activeViewportOrNull();
        if (client == null || viewport == null) {
            return;
        }

        SlotTransparencyChain chain = chainFor(client.slotId(), viewport.glWidth(), viewport.glHeight());
        if (chain == null) {
            clearInstalled(accessor);
            return;
        }

        chain.install(accessor);
    }

    public static void restorePrimary(LevelRenderer levelRenderer) {
        LevelRendererSSAccessor accessor = (LevelRendererSSAccessor)levelRenderer;
        SlotTransparencyChain chain = TRANSPARENCY_CHAINS[0];
        if (chain != null) {
            chain.install(accessor);
        } else {
            clearInstalled(accessor);
        }
    }

    public static void reset(LevelRenderer levelRenderer) {
        LevelRendererSSAccessor accessor = (LevelRendererSSAccessor)levelRenderer;
        PostChain installed = accessor.splitTest$getTransparencyChain();
        boolean installedOwned = isOwned(installed);

        for (int index = 0; index < TRANSPARENCY_CHAINS.length; index++) {
            SlotTransparencyChain chain = TRANSPARENCY_CHAINS[index];
            if (chain != null) {
                chain.close();
                TRANSPARENCY_CHAINS[index] = null;
            }
        }

        if (installed != null && !installedOwned) {
            installed.close();
        }
        clearInstalledFields(accessor);
    }

    private static SlotTransparencyChain chainFor(int clientId, int width, int height) {
        if (clientId < 0 || clientId >= TRANSPARENCY_CHAINS.length) {
            clientId = 0;
        }

        width = Math.max(1, width);
        height = Math.max(1, height);

        SlotTransparencyChain chain = TRANSPARENCY_CHAINS[clientId];
        if (chain != null && chain.width() == width && chain.height() == height) {
            return chain;
        }

        if (chain != null) {
            chain.close();
            TRANSPARENCY_CHAINS[clientId] = null;
        }

        try {
            chain = SlotTransparencyChain.create(Minecraft.getInstance(), width, height);
            TRANSPARENCY_CHAINS[clientId] = chain;
            return chain;
        } catch (IOException | JsonSyntaxException exception) {
            LOGGER.warn("Failed to create split-screen fabulous transparency chain for client {}", clientId, exception);
            return null;
        }
    }

    private static void clearInstalled(LevelRendererSSAccessor accessor) {
        closeForeignInstalled(accessor, null);
        clearInstalledFields(accessor);
    }

    private static void clearInstalledFields(LevelRendererSSAccessor accessor) {
        accessor.splitTest$setTransparencyChain(null);
        accessor.splitTest$setTranslucentTarget(null);
        accessor.splitTest$setItemEntityTarget(null);
        accessor.splitTest$setParticlesTarget(null);
        accessor.splitTest$setWeatherTarget(null);
        accessor.splitTest$setCloudsTarget(null);
    }

    private static void closeForeignInstalled(LevelRendererSSAccessor accessor, PostChain replacement) {
        PostChain installed = accessor.splitTest$getTransparencyChain();
        if (installed != null && installed != replacement && !isOwned(installed)) {
            installed.close();
        }
    }

    private static boolean isOwned(PostChain postChain) {
        if (postChain == null) {
            return false;
        }
        for (SlotTransparencyChain chain : TRANSPARENCY_CHAINS) {
            if (chain != null && chain.postChain() == postChain) {
                return true;
            }
        }
        return false;
    }

    private static final class SlotTransparencyChain implements AutoCloseable {
        private final ViewportPostChain postChain;
        private final RenderTarget translucentTarget;
        private final RenderTarget itemEntityTarget;
        private final RenderTarget particlesTarget;
        private final RenderTarget weatherTarget;
        private final RenderTarget cloudsTarget;

        private SlotTransparencyChain(
            ViewportPostChain postChain,
            RenderTarget translucentTarget,
            RenderTarget itemEntityTarget,
            RenderTarget particlesTarget,
            RenderTarget weatherTarget,
            RenderTarget cloudsTarget
        ) {
            this.postChain = postChain;
            this.translucentTarget = translucentTarget;
            this.itemEntityTarget = itemEntityTarget;
            this.particlesTarget = particlesTarget;
            this.weatherTarget = weatherTarget;
            this.cloudsTarget = cloudsTarget;
        }

        private static SlotTransparencyChain create(Minecraft minecraft, int width, int height) throws IOException, JsonSyntaxException {
            ViewportPostChain postChain = ViewportPostChain.create(minecraft, TRANSPARENCY_LOCATION, width, height, true);
            return new SlotTransparencyChain(
                postChain,
                requiredTarget(postChain, "translucent"),
                requiredTarget(postChain, "itemEntity"),
                requiredTarget(postChain, "particles"),
                requiredTarget(postChain, "weather"),
                requiredTarget(postChain, "clouds")
            );
        }

        private static RenderTarget requiredTarget(ViewportPostChain postChain, String name) {
            RenderTarget target = postChain.tempTarget(name);
            if (target == null) {
                throw new IllegalStateException("Fabulous transparency target '" + name + "' was not created");
            }
            return target;
        }

        private int width() {
            return this.postChain.width();
        }

        private int height() {
            return this.postChain.height();
        }

        private PostChain postChain() {
            return this.postChain.postChain();
        }

        private void install(LevelRendererSSAccessor accessor) {
            closeForeignInstalled(accessor, this.postChain());
            accessor.splitTest$setTransparencyChain(this.postChain());
            accessor.splitTest$setTranslucentTarget(this.translucentTarget);
            accessor.splitTest$setItemEntityTarget(this.itemEntityTarget);
            accessor.splitTest$setParticlesTarget(this.particlesTarget);
            accessor.splitTest$setWeatherTarget(this.weatherTarget);
            accessor.splitTest$setCloudsTarget(this.cloudsTarget);
        }

        @Override
        public void close() {
            this.postChain.close();
        }
    }
}
