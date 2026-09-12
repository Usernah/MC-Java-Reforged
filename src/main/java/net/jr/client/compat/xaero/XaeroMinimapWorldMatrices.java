package net.jr.client.compat.xaero;

import java.util.Map;
import java.util.WeakHashMap;
import net.jr.client.runtime.client.LocalClient;
import net.jr.client.runtime.context.LocalClientScope;
import net.jr.client.runtime.viewport.ViewportArea;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import xaero.common.core.XaeroMinimapCore;

public final class XaeroMinimapWorldMatrices {
    private static final Map<LocalClient, State> STATES = new WeakHashMap<>();

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
        if (state == null || viewport == null || !state.hasProjection || !state.hasModelView) {
            action.run();
            return;
        }

        Matrix4f previousProjection = XaeroMinimapCore.waypointsProjection;
        Matrix4f previousModelView = XaeroMinimapCore.waypointModelView;
        XaeroMinimapCore.waypointsProjection = viewportProjection(state.projection, viewport);
        XaeroMinimapCore.waypointModelView = new Matrix4f(state.modelView);
        try {
            action.run();
        } finally {
            XaeroMinimapCore.waypointsProjection = previousProjection;
            XaeroMinimapCore.waypointModelView = previousModelView;
        }
    }

    private static State currentState() {
        LocalClient client = LocalClientScope.currentClientOrNull();
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
