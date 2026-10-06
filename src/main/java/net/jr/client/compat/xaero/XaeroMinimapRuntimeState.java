package net.jr.client.compat.xaero;

import java.io.IOException;
import java.util.Map;
import java.util.WeakHashMap;

import net.jr.api.client.local.Client;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.client.ClientInstance;
import net.jr.mixin.compat.xaerominimap.MinimapAccessor;
import net.jr.mixin.compat.xaerominimap.MinimapElementWorldRendererHandlerAccessor;
import net.jr.mixin.compat.xaerominimap.MinimapRendererAccessor;
import net.jr.mixin.compat.xaerominimap.MinimapSafeModeRendererAccessor;
import net.minecraft.resources.Identifier;
import xaero.common.HudMod;
import xaero.common.graphics.MinimapTexture;
import xaero.common.minimap.render.MinimapFBORenderer;
import xaero.common.minimap.render.MinimapSafeModeRenderer;
import xaero.hud.minimap.Minimap;
import xaero.hud.minimap.element.render.MinimapElementRenderer;
import xaero.hud.minimap.element.render.world.MinimapElementWorldRendererHandler;

public final class XaeroMinimapRuntimeState {
    private static final Map<ClientInstance, State> STATES = new WeakHashMap<>();
    private static final ThreadLocal<State> ACTIVE_STATE = new ThreadLocal<>();

    private XaeroMinimapRuntimeState() {
    }

    public static void run(Runnable action) {
        try (Scope ignored = activate()) {
            action.run();
        }
    }

    public static Scope activate() {
        Minimap minimap = HudMod.INSTANCE.getMinimap();
        if (minimap == null) {
            return new Scope(null, null, false);
        }

        State state = currentState(minimap);
        State previous = ACTIVE_STATE.get();
        boolean switched = previous != state;
        if (switched) {
            if (previous != null) {
                previous.capture(minimap);
            } else {
                primaryState(minimap).capture(minimap);
            }
            state.apply(minimap);
            ACTIVE_STATE.set(state);
        }
        return new Scope(state, previous, switched);
    }

    private static State currentState(Minimap minimap) {
        ClientInstance client = Client.currentOrNull();
        if (client == null) {
            client = ClientRuntime.INSTANCE.clients().client(0);
        }
        return state(client, minimap);
    }

    private static State primaryState(Minimap minimap) {
        return state(ClientRuntime.INSTANCE.clients().client(0), minimap);
    }

    private static State state(ClientInstance client, Minimap minimap) {
        synchronized (STATES) {
            return STATES.computeIfAbsent(client, ignored -> new State(client.slotId(), minimap));
        }
    }

    public static final class Scope implements AutoCloseable {
        private final State state;
        private final State previous;
        private final boolean switched;
        private boolean closed;

        private Scope(State state, State previous, boolean switched) {
            this.state = state;
            this.previous = previous;
            this.switched = switched;
        }

        @Override
        public void close() {
            if (this.closed || !this.switched || this.state == null) {
                return;
            }
            this.closed = true;
            Minimap minimap = HudMod.INSTANCE.getMinimap();
            this.state.capture(minimap);
            if (this.previous != null) {
                this.previous.apply(minimap);
                ACTIVE_STATE.set(this.previous);
            } else {
                primaryState(minimap).apply(minimap);
                ACTIVE_STATE.remove();
            }
        }
    }

    private static final class State {
        private int fboLastMinimapSize;
        private double fboZoom = 1.0;
        private int safeLastMinimapSize;
        private double safeZoom = 1.0;
        private int[] safeData;
        private byte safeDrawYState;
        private MinimapTexture safeTexture;
        private Throwable crashedWith;
        private Object previousHoveredElement;
        private MinimapElementRenderer<?, ?> previousHoveredRenderer;
        private boolean previousHoveredElementPresent;

        private State(int slotId, Minimap minimap) {
            if (slotId == 0) {
                this.capture(minimap);
                return;
            }
            try {
                this.safeTexture = new MinimapTexture(Identifier.parse("java_reforged:xaero_minimap_" + slotId));
            } catch (IOException exception) {
                throw new IllegalStateException(exception);
            }
        }

        private void capture(Minimap minimap) {
            MinimapFBORenderer fbo = minimap.getMinimapFBORenderer();
            MinimapSafeModeRenderer safe = minimap.getMinimapSafeModeRenderer();
            MinimapRendererAccessor fboState = (MinimapRendererAccessor)fbo;
            MinimapRendererAccessor safeState = (MinimapRendererAccessor)safe;
            MinimapSafeModeRendererAccessor safeModeState = (MinimapSafeModeRendererAccessor)safe;
            this.fboLastMinimapSize = fboState.javaReforged$getLastMinimapSize();
            this.fboZoom = fboState.javaReforged$getZoom();
            this.safeLastMinimapSize = safeState.javaReforged$getLastMinimapSize();
            this.safeZoom = safeState.javaReforged$getZoom();
            this.safeData = safeModeState.javaReforged$getData();
            this.safeDrawYState = safeModeState.javaReforged$getDrawYState();
            this.safeTexture = safeModeState.javaReforged$getMapTexture();
            this.crashedWith = ((MinimapAccessor)minimap).javaReforged$getCrashedWith();
            MinimapElementWorldRendererHandler worldHandler = minimap.getWorldRendererHandler();
            MinimapElementWorldRendererHandlerAccessor worldState =
                (MinimapElementWorldRendererHandlerAccessor)worldHandler;
            this.previousHoveredElement = worldState.javaReforged$getPreviousClosestHoveredElement();
            this.previousHoveredRenderer = worldState.javaReforged$getPreviousClosestHoveredElementRenderer();
            this.previousHoveredElementPresent = worldState.javaReforged$getPreviousClosestHoveredElementPresent();
        }

        private void apply(Minimap minimap) {
            MinimapFBORenderer fbo = minimap.getMinimapFBORenderer();
            MinimapSafeModeRenderer safe = minimap.getMinimapSafeModeRenderer();
            MinimapRendererAccessor fboState = (MinimapRendererAccessor)fbo;
            MinimapRendererAccessor safeState = (MinimapRendererAccessor)safe;
            MinimapSafeModeRendererAccessor safeModeState = (MinimapSafeModeRendererAccessor)safe;
            fboState.javaReforged$setLastMinimapSize(this.fboLastMinimapSize);
            fboState.javaReforged$setZoom(this.fboZoom);
            safeState.javaReforged$setLastMinimapSize(this.safeLastMinimapSize);
            safeState.javaReforged$setZoom(this.safeZoom);
            safeModeState.javaReforged$setData(this.safeData);
            safeModeState.javaReforged$setDrawYState(this.safeDrawYState);
            safeModeState.javaReforged$setMapTexture(this.safeTexture);
            ((MinimapAccessor)minimap).javaReforged$setCrashedWith(this.crashedWith);
            MinimapElementWorldRendererHandler worldHandler = minimap.getWorldRendererHandler();
            MinimapElementWorldRendererHandlerAccessor worldState =
                (MinimapElementWorldRendererHandlerAccessor)worldHandler;
            worldState.javaReforged$setPreviousClosestHoveredElement(this.previousHoveredElement);
            worldState.javaReforged$setPreviousClosestHoveredElementRenderer(this.previousHoveredRenderer);
            worldState.javaReforged$setPreviousClosestHoveredElementPresent(this.previousHoveredElementPresent);
        }
    }
}
