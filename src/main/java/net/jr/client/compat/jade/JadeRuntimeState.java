package net.jr.client.compat.jade;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;

import net.jr.api.client.local.Client;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.client.ClientInstance;
import net.jr.mixin.compat.jade.ClientProxyAccessor;
import net.jr.mixin.compat.jade.JadeClientAccessor;
import net.jr.mixin.compat.jade.ObjectDataCenterAccessor;
import net.jr.mixin.compat.jade.OverlayRendererAccessor;
import snownee.jade.JadeClient;
import snownee.jade.api.ui.TooltipAnimation;
import snownee.jade.impl.ObjectDataCenter;
import snownee.jade.impl.config.WailaConfig;
import snownee.jade.impl.ui.BoxElementImpl;
import snownee.jade.overlay.OverlayRenderer;
import snownee.jade.overlay.RayTracing;
import snownee.jade.overlay.WailaTickHandler;

public final class JadeRuntimeState {
    private static final Map<ClientInstance, State> STATES = new WeakHashMap<>();
    private static final ThreadLocal<State> ACTIVE_STATE = new ThreadLocal<>();

    private JadeRuntimeState() {
    }

    public static WailaTickHandler tickHandler(WailaTickHandler primary) {
        State state = currentState();
        if (state.tickHandler == null) {
            state.tickHandler = state.client.isPrimary() ? primary : new WailaTickHandler();
        }
        return state.tickHandler;
    }

    public static RayTracing rayTracing(RayTracing primary) {
        State state = currentState();
        if (state.rayTracing == null) {
            state.rayTracing = state.client.isPrimary() ? primary : new RayTracing();
        }
        return state.rayTracing;
    }

    public static TooltipAnimation animation() {
        return currentState().animation;
    }

    public static WailaConfig options(WailaConfig primary) {
        return currentState().options.config(primary);
    }

    public static boolean saveOptions() {
        return currentState().options.save();
    }

    public static boolean invalidateOptions() {
        return currentState().options.invalidate();
    }

    public static boolean resetOptions(WailaConfig primary) {
        return currentState().options.reset(primary);
    }

    public static boolean shown() {
        return currentState().shown;
    }

    public static void exposeShown() {
        OverlayRenderer.shown = currentState().shown;
    }

    public static void setFog(float start, float end) {
        State state = currentState();
        state.renderDistanceStart = start;
        state.renderDistanceEnd = end;
    }

    public static void run(Runnable action) {
        try (Scope ignored = activate()) {
            action.run();
        }
    }

    public static <T> T call(Supplier<T> action) {
        try (Scope ignored = activate()) {
            return action.get();
        }
    }

    public static Runnable wrap(Runnable action) {
        return () -> run(action);
    }

    public static Scope activate() {
        State state = currentState();
        State previous = ACTIVE_STATE.get();
        boolean switched = previous != state;
        if (switched) {
            if (previous != null) {
                previous.capture();
            }
            state.apply();
            ACTIVE_STATE.set(state);
        }
        return new Scope(state, previous, switched);
    }

    private static State currentState() {
        ClientInstance client = Client.currentOrNull();
        if (client == null) {
            client = ClientRuntime.INSTANCE.clients().client(0);
        }
        synchronized (STATES) {
            return STATES.computeIfAbsent(client, State::new);
        }
    }

    private static State primaryState() {
        ClientInstance client = ClientRuntime.INSTANCE.clients().client(0);
        synchronized (STATES) {
            return STATES.computeIfAbsent(client, State::new);
        }
    }

    private static void copyAnimation(TooltipAnimation target, TooltipAnimation source) {
        target.expectedRect.copy(source.expectedRect);
        target.rect.copy(source.rect);
        target.startTime = source.startTime;
        target.startRect.copy(source.startRect);
        target.scale = source.scale;
        target.showHideAlpha = source.showHideAlpha;
        target.alpha = source.alpha;
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
            if (this.closed) {
                return;
            }
            this.closed = true;
            if (!this.switched) {
                return;
            }
            this.state.capture();
            if (this.previous != null) {
                this.previous.apply();
                ACTIVE_STATE.set(this.previous);
            } else {
                primaryState().apply();
                ACTIVE_STATE.remove();
            }
        }
    }

    private static final class State {
        private final ClientInstance client;
        private final JadeOptionsState options;
        private final TooltipAnimation animation = new TooltipAnimation();
        private WailaTickHandler tickHandler;
        private RayTracing rayTracing;
        private float renderDistanceStart;
        private float renderDistanceEnd;
        private float savedProgress;
        private float progressAlpha;
        private boolean canHarvest;
        private long inPowderSnowTime;
        private long timeLastUpdate;
        private boolean serverConnected;
        private Object lastObject;
        private float ticks;
        private boolean shown;
        private BoxElementImpl lingerTooltip;
        private float disappearTicks;
        private boolean bossbarShown;
        private int bossbarHeight;

        private State(ClientInstance client) {
            this.client = client;
            this.options = new JadeOptionsState(client.slotId());
        }

        private void capture() {
            this.renderDistanceStart = JadeClient.renderDistanceStart;
            this.renderDistanceEnd = JadeClient.renderDistanceEnd;
            this.savedProgress = JadeClientAccessor.javaReforged$getSavedProgress();
            this.progressAlpha = JadeClientAccessor.javaReforged$getProgressAlpha();
            this.canHarvest = JadeClientAccessor.javaReforged$getCanHarvest();
            this.inPowderSnowTime = JadeClientAccessor.javaReforged$getInPowderSnowTime();
            this.timeLastUpdate = ObjectDataCenter.timeLastUpdate;
            this.serverConnected = ObjectDataCenter.serverConnected;
            this.lastObject = ObjectDataCenterAccessor.javaReforged$getLastObject();
            copyAnimation(this.animation, OverlayRenderer.animation);
            this.ticks = OverlayRenderer.ticks;
            this.shown = OverlayRenderer.shown;
            this.lingerTooltip = OverlayRendererAccessor.javaReforged$getLingerTooltip();
            this.disappearTicks = OverlayRendererAccessor.javaReforged$getDisappearTicks();
            this.bossbarShown = ClientProxyAccessor.javaReforged$getBossbarShown();
            this.bossbarHeight = ClientProxyAccessor.javaReforged$getBossbarHeight();
        }

        private void apply() {
            JadeClient.renderDistanceStart = this.renderDistanceStart;
            JadeClient.renderDistanceEnd = this.renderDistanceEnd;
            JadeClientAccessor.javaReforged$setSavedProgress(this.savedProgress);
            JadeClientAccessor.javaReforged$setProgressAlpha(this.progressAlpha);
            JadeClientAccessor.javaReforged$setCanHarvest(this.canHarvest);
            JadeClientAccessor.javaReforged$setInPowderSnowTime(this.inPowderSnowTime);
            ObjectDataCenter.timeLastUpdate = this.timeLastUpdate;
            ObjectDataCenter.serverConnected = this.serverConnected;
            ObjectDataCenterAccessor.javaReforged$setLastObject(this.lastObject);
            copyAnimation(OverlayRenderer.animation, this.animation);
            OverlayRenderer.ticks = this.ticks;
            OverlayRenderer.shown = this.shown;
            OverlayRendererAccessor.javaReforged$setLingerTooltip(this.lingerTooltip);
            OverlayRendererAccessor.javaReforged$setDisappearTicks(this.disappearTicks);
            ClientProxyAccessor.javaReforged$setBossbarShown(this.bossbarShown);
            ClientProxyAccessor.javaReforged$setBossbarHeight(this.bossbarHeight);
        }
    }
}
