package net.alnv.javareforged.ClientRuntime.runtime;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.alnv.javareforged.ClientRuntime.viewport.ViewportArea;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.ClientHooks;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.lwjgl.opengl.GL11;

/** Installs the complete GUI render state for the already selected local client. */
public final class GuiViewportScope implements AutoCloseable {
    private final ViewportPass.Scope viewportScope;
    private final int[] previousViewport = new int[4];
    private final int[] previousScissor = new int[4];
    private final boolean previousScissorEnabled;
    private final Matrix4fStack modelView;
    private boolean closed;

    private GuiViewportScope(ViewportArea viewport) {
        this.viewportScope = ViewportPass.enter(viewport);
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, this.previousViewport);
        this.previousScissorEnabled = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, this.previousScissor);

        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getMainRenderTarget().bindWrite(true);
        RenderSystem.viewport(viewport.glX(), viewport.glY(), viewport.glWidth(), viewport.glHeight());
        RenderSystem.enableScissor(viewport.glX(), viewport.glY(), viewport.glWidth(), viewport.glHeight());

        RenderSystem.backupProjectionMatrix();
        float farPlane = ClientHooks.getGuiFarPlane();
        Matrix4f projection = new Matrix4f().setOrtho(
            0.0F,
            (float)viewport.guiWidth(),
            (float)viewport.guiHeight(),
            0.0F,
            1000.0F,
            farPlane
        );
        RenderSystem.setProjectionMatrix(projection, VertexSorting.ORTHOGRAPHIC_Z);

        this.modelView = RenderSystem.getModelViewStack();
        this.modelView.pushMatrix();
        this.modelView.identity();
        this.modelView.translation(0.0F, 0.0F, 10000.0F - farPlane);
        RenderSystem.applyModelViewMatrix();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
    }

    public static GuiViewportScope enter() {
        return new GuiViewportScope(Client.viewport());
    }

    @Override
    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;

        this.modelView.popMatrix();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.restoreProjectionMatrix();
        RenderSystem.viewport(
            this.previousViewport[0],
            this.previousViewport[1],
            this.previousViewport[2],
            this.previousViewport[3]
        );
        if (this.previousScissorEnabled) {
            RenderSystem.enableScissor(
                this.previousScissor[0],
                this.previousScissor[1],
                this.previousScissor[2],
                this.previousScissor[3]
            );
        } else {
            RenderSystem.disableScissor();
        }
        this.viewportScope.close();
    }
}
