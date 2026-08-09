package net.alnv.javareforged.ClientRuntime.runtime;

import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import net.alnv.javareforged.ClientRuntime.viewport.ViewportArea;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

/** Runs Minecraft's vanilla menu-background blur inside the current Client viewport. */
public final class VanillaBackgroundBlur {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation BLUR_LOCATION = ResourceLocation.withDefaultNamespace("shaders/post/blur.json");
    private static final RuntimeChain[] CHAINS = new RuntimeChain[Client.MAX_CLIENTS];

    private VanillaBackgroundBlur() {
    }

    public static boolean process(float partialTick) {
        LocalClient client = Client.currentOrNull();
        ViewportArea viewport = ViewportPass.activeViewportOrNull();
        if (client == null || viewport == null || !client.hasViewport()) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        float radius = minecraft.options.getMenuBackgroundBlurriness();
        if (radius < 1.0F) {
            return true;
        }

        RuntimeChain chain = runtimeFor(minecraft, client.slotId(), viewport.glWidth(), viewport.glHeight());
        if (chain == null) {
            return false;
        }

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        chain.viewportChain().postChain().setUniform("Radius", radius);
        chain.viewportChain().postChain().process(partialTick);

        minecraft.getMainRenderTarget().bindWrite(false);
        ViewportPass.applyActiveViewport(minecraft.getMainRenderTarget());
        RenderSystem.depthMask(true);
        return true;
    }

    public static void closeAll() {
        for (int i = 0; i < CHAINS.length; i++) {
            close(i);
        }
    }

    private static RuntimeChain runtimeFor(Minecraft minecraft, int clientId, int width, int height) {
        if (clientId < 0 || clientId >= CHAINS.length) {
            clientId = 0;
        }

        width = Math.max(1, width);
        height = Math.max(1, height);

        RuntimeChain chain = CHAINS[clientId];
        if (chain != null && chain.width == width && chain.height == height) {
            return chain;
        }

        close(clientId);

        try {
            chain = new RuntimeChain(
                width,
                height,
                ViewportPostChain.create(minecraft, BLUR_LOCATION, width, height, false)
            );
            CHAINS[clientId] = chain;
            return chain;
        } catch (IOException | JsonSyntaxException exception) {
            LOGGER.warn("Failed to create split-screen vanilla background blur for client {}", clientId, exception);
            return null;
        }
    }

    private static void close(int clientId) {
        RuntimeChain chain = CHAINS[clientId];
        if (chain == null) {
            return;
        }
        CHAINS[clientId] = null;
        chain.close();
    }

    private static final class RuntimeChain implements AutoCloseable {
        private final int width;
        private final int height;
        private final ViewportPostChain postChain;

        private RuntimeChain(int width, int height, ViewportPostChain postChain) {
            this.width = width;
            this.height = height;
            this.postChain = postChain;
        }

        private ViewportPostChain viewportChain() {
            return this.postChain;
        }

        @Override
        public void close() {
            this.postChain.close();
        }
    }
}
