package net.jr.client.compat.xaero;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.ScissorState;
import java.util.Map;
import java.util.WeakHashMap;

import net.jr.api.client.local.Client;
import net.jr.client.runtime.client.ClientInstance;
import net.jr.client.runtime.viewport.ViewportArea;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import xaero.common.core.XaeroMinimapCore;

public final class XaeroMinimapWorldMatrices {
    private static final Map<ClientInstance, State> STATES = new WeakHashMap<>();

    private XaeroMinimapWorldMatrices() {
    }

    public static void captureProjection(Matrix4f projection) {
        State state = currentState();
        if (state != null) {
            state.projection.set(projection);
            state.hasProjection = true;
        }
    }

    public static void captureModelView(Matrix4fc modelView) {
        State state = currentState();
        if (state != null) {
            state.modelView.set(modelView);
            state.hasModelView = true;
        }
    }

    public static void run(Runnable action) {
        State state = currentState();
        ViewportArea viewport = XaeroViewport.currentOrNull();
        if (viewport == null) {
            action.run();
            return;
        }

        ScissorState previousScissor = new ScissorState(RenderSystem.getScissorStateForRenderTypeDraws());
        enableViewportScissor(previousScissor, viewport);

        boolean replaceMatrices = state != null && state.hasProjection && state.hasModelView;
        Matrix4f previousProjection = null;
        Matrix4f previousModelView = null;
        if (replaceMatrices) {
            previousProjection = XaeroMinimapCore.waypointsProjection;
            previousModelView = XaeroMinimapCore.waypointModelView;
            XaeroMinimapCore.waypointsProjection = viewportProjection(state.projection, viewport);
            XaeroMinimapCore.waypointModelView = new Matrix4f(state.modelView);
        }
        try {
            action.run();
        } finally {
            if (replaceMatrices) {
                XaeroMinimapCore.waypointsProjection = previousProjection;
                XaeroMinimapCore.waypointModelView = previousModelView;
            }
            RenderSystem.getScissorStateForRenderTypeDraws().setFrom(previousScissor);
        }
    }

    private static void enableViewportScissor(ScissorState previous, ViewportArea viewport) {
        int left = viewport.glX();
        int bottom = viewport.glY();
        int right = left + viewport.glWidth();
        int top = bottom + viewport.glHeight();

        if (previous.enabled()) {
            left = Math.max(left, previous.x());
            bottom = Math.max(bottom, previous.y());
            right = Math.min(right, previous.x() + previous.width());
            top = Math.min(top, previous.y() + previous.height());
        }

        RenderSystem.enableScissorForRenderTypeDraws(
            left,
            bottom,
            Math.max(0, right - left),
            Math.max(0, top - bottom)
        );
    }

    private static State currentState() {
        ClientInstance client = Client.currentOrNull();
        if (client == null) {
            return null;
        }
        synchronized (STATES) {
            return STATES.computeIfAbsent(client, ignored -> new State());
        }
    }

    private static Matrix4f viewportProjection(Matrix4f projection, ViewportArea viewport) {
        int width = Minecraft.getInstance().getWindow().getWidth();
        int height = Minecraft.getInstance().getWindow().getHeight();
        if (width <= 0 || height <= 0) {
            return new Matrix4f(projection);
        }

        float scaleX = viewport.width() / (float)width;
        float scaleY = viewport.height() / (float)height;
        float offsetX = (2.0F * viewport.x() + viewport.width()) / width - 1.0F;
        float offsetY = 1.0F - (2.0F * viewport.y() + viewport.height()) / height;
        return new Matrix4f()
            .translation(offsetX, offsetY, 0.0F)
            .scale(scaleX, scaleY, 1.0F)
            .mul(projection);
    }

    private static final class State {
        private final Matrix4f projection = new Matrix4f();
        private final Matrix4f modelView = new Matrix4f();
        private boolean hasProjection;
        private boolean hasModelView;
    }
}
