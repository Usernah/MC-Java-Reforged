package net.alnv.javareforged.ClientRuntime.runtime;

import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.alnv.javareforged.ClientRuntime.viewport.ViewportArea;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30C;

/**
 * Runs a vanilla {@link PostChain} against a viewport-local screen target.
 *
 * <p>Vanilla post chains treat {@code minecraft:main} as the full window. In
 * split-screen that makes shaders sample or write outside the current player
 * viewport. This wrapper gives the chain a local {@code minecraft:main}, then
 * copies between the real main framebuffer and that local target at the single
 * global boundary: {@link PostChain#process(float)}.</p>
 */
public final class ViewportPostChain implements AutoCloseable {
    private static final Map<PostChain, ViewportPostChain> CHAINS = new IdentityHashMap<>();

    private final int width;
    private final int height;
    private final boolean copyDepth;
    private final TextureTarget screenTarget;
    private final PostChain postChain;
    private boolean closed;

    private ViewportPostChain(int width, int height, boolean copyDepth, TextureTarget screenTarget, PostChain postChain) {
        this.width = width;
        this.height = height;
        this.copyDepth = copyDepth;
        this.screenTarget = screenTarget;
        this.postChain = postChain;
    }

    public static ViewportPostChain create(
        Minecraft minecraft,
        ResourceLocation location,
        int width,
        int height,
        boolean copyDepth
    ) throws IOException, JsonSyntaxException {
        width = Math.max(1, width);
        height = Math.max(1, height);

        TextureTarget screenTarget = new TextureTarget(width, height, copyDepth, Minecraft.ON_OSX);
        screenTarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);

        PostChain postChain = new PostChain(
            minecraft.getTextureManager(),
            minecraft.getResourceManager(),
            screenTarget,
            location
        );
        postChain.resize(width, height);

        ViewportPostChain chain = new ViewportPostChain(width, height, copyDepth, screenTarget, postChain);
        CHAINS.put(postChain, chain);
        return chain;
    }

    public static void beforeProcess(PostChain postChain) {
        ViewportPostChain chain = CHAINS.get(postChain);
        if (chain != null) {
            chain.captureFromMain();
        }
    }

    public static void afterProcess(PostChain postChain) {
        ViewportPostChain chain = CHAINS.get(postChain);
        if (chain != null) {
            chain.drawToMain();
        }
    }

    public int width() {
        return this.width;
    }

    public int height() {
        return this.height;
    }

    public PostChain postChain() {
        return this.postChain;
    }

    @Nullable
    public RenderTarget tempTarget(String name) {
        return this.postChain.getTempTarget(name);
    }

    private void captureFromMain() {
        ViewportArea viewport = ViewportPass.activeViewportOrNull();
        if (viewport == null) {
            return;
        }

        RenderTarget mainTarget = Minecraft.getInstance().getMainRenderTarget();
        int mask = GL11.GL_COLOR_BUFFER_BIT | (this.copyDepth ? GL11.GL_DEPTH_BUFFER_BIT : 0);
        blit(
            mainTarget.frameBufferId,
            this.screenTarget.frameBufferId,
            viewport.glX(),
            viewport.glY(),
            viewport.glX() + viewport.glWidth(),
            viewport.glY() + viewport.glHeight(),
            0,
            0,
            this.width,
            this.height,
            mask,
            GL11.GL_NEAREST
        );
    }

    private void drawToMain() {
        ViewportArea viewport = ViewportPass.activeViewportOrNull();
        if (viewport == null) {
            return;
        }

        RenderTarget mainTarget = Minecraft.getInstance().getMainRenderTarget();
        blit(
            this.screenTarget.frameBufferId,
            mainTarget.frameBufferId,
            0,
            0,
            this.width,
            this.height,
            viewport.glX(),
            viewport.glY(),
            viewport.glX() + viewport.glWidth(),
            viewport.glY() + viewport.glHeight(),
            GL11.GL_COLOR_BUFFER_BIT,
            GL11.GL_NEAREST
        );

        mainTarget.bindWrite(false);
        ViewportPass.applyActiveViewport(mainTarget);
    }

    private static void blit(
        int readFramebuffer,
        int drawFramebuffer,
        int srcX0,
        int srcY0,
        int srcX1,
        int srcY1,
        int dstX0,
        int dstY0,
        int dstX1,
        int dstY1,
        int mask,
        int filter
    ) {
        int previousRead = GL11.glGetInteger(GL30C.GL_READ_FRAMEBUFFER_BINDING);
        int previousDraw = GL11.glGetInteger(GL30C.GL_DRAW_FRAMEBUFFER_BINDING);
        boolean scissorEnabled = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        try {
            GL30C.glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, readFramebuffer);
            GL30C.glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL30C.glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);
        } finally {
            GL30C.glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, previousRead);
            GL30C.glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, previousDraw);
            if (scissorEnabled) {
                GL11.glEnable(GL11.GL_SCISSOR_TEST);
            }
        }
    }

    @Override
    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        CHAINS.remove(this.postChain);
        this.postChain.close();
        this.screenTarget.destroyBuffers();
    }
}
