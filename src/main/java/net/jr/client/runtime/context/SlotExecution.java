package net.jr.client.runtime.context;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.jr.api.client.render.gui.GuiRenderContext;
import net.jr.client.compat.create.CreateRuntimeState;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.client.ClientInstance;
import net.jr.client.runtime.render.state.HudStateScope;
import net.jr.client.runtime.render.state.HandStateScope;
import net.jr.client.runtime.render.state.WorldEngineStateScope;
import net.jr.client.runtime.render.target.ViewportRenderTargets;
import net.jr.client.runtime.render.world.WorldRenderBackend;
import net.jr.client.runtime.render.world.WorldRenderBackends;
import net.jr.client.runtime.render.world.WorldRenderView;
import net.jr.client.runtime.slot.Slots;
import net.jr.client.runtime.viewport.GuiViewportScope;
import net.jr.client.runtime.viewport.ViewportArea;
import net.jr.client.runtime.viewport.ViewportRenderScope;
import net.jr.mixin.accessors.GameRendererAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.TextureFilteringMethod;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.Identifier;

public final class SlotExecution {
    private static final ThreadLocal<ExecutionFrame> ACTIVE = new ThreadLocal<>();
    private static volatile boolean minecraftReady;

    private SlotExecution() {
    }

    public static Slots currentSlot() {
        ExecutionFrame frame = currentFrameOrNull();
        if (frame == null) {
            throw new IllegalStateException("No local client slot is active on this execution path");
        }
        return frame.slot();
    }

    @Nullable
    public static Slots currentSlotOrNull() {
        ExecutionFrame frame = currentFrameOrNull();
        return frame != null ? frame.slot() : null;
    }

    public static int currentSlotId() {
        return currentSlot().id();
    }

    @Nullable
    public static Integer currentSlotIdOrNull() {
        Slots slot = currentSlotOrNull();
        return slot != null ? slot.id() : null;
    }

    @Nullable
    public static ViewportArea currentViewportOrNull() {
        ExecutionFrame frame = currentFrameOrNull();
        return frame != null ? frame.viewport() : null;
    }

    public static ViewportArea currentViewport() {
        ViewportArea viewport = currentViewportOrNull();
        if (viewport == null) {
            throw new IllegalStateException("No viewport is active on this execution path");
        }
        return viewport;
    }

    @Nullable
    public static ExecutionFrame currentFrameOrNull() {
        return ACTIVE.get();
    }

    public static boolean isMinecraftBootstrapping() {
        return !minecraftReady;
    }

    public static void completeMinecraftBootstrap() {
        minecraftReady = true;
    }

    public static void run(Slots slot, Runnable action) {
        runResolved(slot, null, null, ExecutionPhase.GENERAL, action, null);
    }

    public static <T> T run(Slots slot, Supplier<T> action) {
        Objects.requireNonNull(action, "action");
        Holder<T> result = new Holder<>();
        run(slot, (Runnable)() -> {
            result.value = action.get();
        });
        return result.value;
    }

    public static void run(
        Slots slot,
        WorldRenderView view,
        ExecutionPhase phase,
        Runnable action
    ) {
        Objects.requireNonNull(view, "view");
        if (phase != ExecutionPhase.WORLD_EXTRACTION && phase != ExecutionPhase.WORLD_RENDER) {
            throw new IllegalArgumentException("World render views require a world execution phase");
        }
        runResolved(slot, view.viewport(), view, phase, action, null);
    }

    public static void run(
        Slots slot,
        GuiRenderContext context,
        ExecutionPhase phase,
        Runnable action
    ) {
        Objects.requireNonNull(context, "context");
        if (phase != ExecutionPhase.GUI) {
            throw new IllegalArgumentException("GUI render contexts require the GUI execution phase");
        }
        runResolved(slot, context.viewport(), null, phase, action, context.graphics());
    }

    public static void run(
        Slots slot,
        GuiGraphicsExtractor graphics,
        ViewportArea viewport,
        ExecutionPhase phase,
        Runnable action
    ) {
        Objects.requireNonNull(graphics, "graphics");
        if (phase != ExecutionPhase.GUI) {
            throw new IllegalArgumentException("GUI graphics require the GUI execution phase");
        }
        runResolved(slot, viewport, null, phase, action, graphics);
    }

    public static void run(Slots slot, ExecutionPhase phase, Runnable action) {
        runResolved(slot, null, null, phase, action, null);
    }

    public static void run(Slots slot, ViewportArea viewport, Runnable action) {
        runResolved(slot, viewport, null, ExecutionPhase.GENERAL, action, null);
    }

    private static void runResolved(
        Slots slot,
        @Nullable ViewportArea viewport,
        @Nullable WorldRenderView view,
        ExecutionPhase phase,
        Runnable action,
        @Nullable GuiGraphicsExtractor guiGraphics
    ) {
        Objects.requireNonNull(slot, "slot");
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(action, "action");
        Slots resolvedSlot = SlotResolver.requireSlot(slot);
        if (view != null && view.slot() != resolvedSlot) {
            throw new IllegalArgumentException("World render view does not belong to the requested slot");
        }
        ClientInstance client = ClientRuntime.INSTANCE.clients().clientOrNull(resolvedSlot.id());
        ViewportArea resolvedViewport = viewport != null
            ? viewport
            : ClientRuntime.INSTANCE.viewports().viewportOrNull(resolvedSlot.id());
        ExecutionFrame frame = new ExecutionFrame(resolvedSlot, client, resolvedViewport, view, phase);
        Binding binding = install(frame, guiGraphics);
        try {
            action.run();
            binding.complete();
        } finally {
            binding.close();
        }
    }

    private static Binding install(ExecutionFrame frame, @Nullable GuiGraphicsExtractor guiGraphics) {
        Minecraft minecraft = Minecraft.getInstance();
        ExecutionFrame previous = ACTIVE.get();
        ACTIVE.set(frame);
        Binding binding = new Binding(frame, previous);
        try {
            binding.add(MinecraftClientStateScope.bind(minecraft, frame.slot()));
            binding.add(CreateRuntimeState.activate(frame.client()));
            switch (frame.phase()) {
                case HAND -> installHand(binding, minecraft, frame);
                case WORLD_EXTRACTION -> installWorldExtraction(binding, minecraft, frame);
                case WORLD_RENDER -> installWorldRender(binding, minecraft, frame);
                case WORLD_TICK -> binding.add(WorldEngineStateScope.bind(minecraft, frame.slot()));
                case TERRAIN -> binding.add(WorldEngineStateScope.bindTerrainOnly(minecraft, frame.slot()));
                case GUI -> installGui(binding, minecraft, frame, Objects.requireNonNull(guiGraphics));
                case HUD_TICK -> binding.add(HudStateScope.enter(
                    minecraft.gui.hud,
                    frame.slot().renderState().hud()
                ));
                case GENERAL -> {
                }
            }
            return binding;
        } catch (RuntimeException | Error error) {
            binding.close();
            throw error;
        }
    }

    private static void installWorldExtraction(Binding binding, Minecraft minecraft, ExecutionFrame frame) {
        WorldRenderView view = Objects.requireNonNull(frame.worldView());
        syncOptions(minecraft.gameRenderer, frame.slot());
        binding.add(WorldRenderBackends.active().enterExtractionView(view));
        binding.add(WorldEngineStateScope.bind(minecraft, frame.slot()));
    }

    private static void installWorldRender(Binding binding, Minecraft minecraft, ExecutionFrame frame) {
        WorldRenderView view = Objects.requireNonNull(frame.worldView());
        ViewportArea viewport = Objects.requireNonNull(frame.viewport());
        GameRenderer gameRenderer = minecraft.gameRenderer;
        WorldRenderBackend backend = WorldRenderBackends.active();

        binding.add(ViewportRenderScope.enter(viewport));
        binding.add(WorldEngineStateScope.bind(minecraft, frame.slot()));
        binding.add(backend.enterRenderView(view));
        installHand(binding, minecraft, frame);

        syncOptions(gameRenderer, frame.slot());
        var gameState = gameRenderer.gameRenderState();
        var optionsState = gameState.optionsRenderState;
        GameRendererAccessor renderer = (GameRendererAccessor)(Object)gameRenderer;
        gameState.guiRenderState.isHudHidden = minecraft.gui.hud.isHidden();
        renderer.javaReforged$getGlobalSettingsUniform().update(
            viewport.width(),
            viewport.height(),
            optionsState.glintStrength,
            view.level().getGameTime(),
            view.deltaTracker(),
            optionsState.menuBackgroundBlurriness,
            frame.slot().renderState().levelRenderState().cameraRenderState.pos,
            optionsState.textureFiltering == TextureFilteringMethod.RGSS
        );
        renderer.javaReforged$getLightmap().render(frame.slot().renderState().lightmapRenderState());

        ViewportRenderTargets.Scope targets = ViewportRenderTargets.enter(
            frame.slot().id(),
            viewport,
            gameRenderer.mainRenderTarget()
        );
        binding.add(targets);
        binding.onComplete(() -> {
            minecraft.levelRenderer.doEntityOutline();
            Identifier postEffect = gameRenderer.currentPostEffect();
            if (postEffect != null && renderer.javaReforged$isPostEffectActive()) {
                PostChain chain = minecraft.getShaderManager().getPostChain(
                    postEffect,
                    LevelTargetBundle.MAIN_TARGETS
                );
                if (chain != null) {
                    chain.process(gameRenderer.mainRenderTarget(), renderer.javaReforged$getResourcePool());
                }
            }
            targets.present();
        });
    }

    private static void installGui(
        Binding binding,
        Minecraft minecraft,
        ExecutionFrame frame,
        GuiGraphicsExtractor graphics
    ) {
        binding.add(GuiViewportScope.enter(graphics, Objects.requireNonNull(frame.viewport())));
        binding.add(HudStateScope.enter(
            minecraft.gui.hud,
            frame.slot().renderState().hud()
        ));
    }

    private static void installHand(Binding binding, Minecraft minecraft, ExecutionFrame frame) {
        binding.add(HandStateScope.enter(
            ((GameRendererAccessor)(Object)minecraft.gameRenderer).javaReforged$getItemInHandRenderer(),
            frame.slot().renderState().hands()
        ));
    }

    private static void syncOptions(GameRenderer gameRenderer, Slots slot) {
        var optionsState = gameRenderer.gameRenderState().optionsRenderState;
        optionsState.cameraType = slot.optionsState().cameraType();
        optionsState.fov = slot.optionsState().fov().get();
    }

    private static final class Binding implements AutoCloseable {
        private final ExecutionFrame frame;
        @Nullable
        private final ExecutionFrame previous;
        private final Deque<AutoCloseable> scopes = new ArrayDeque<>();
        private Runnable completion = () -> {
        };
        private boolean closed;

        private Binding(ExecutionFrame frame, @Nullable ExecutionFrame previous) {
            this.frame = frame;
            this.previous = previous;
        }

        private void add(AutoCloseable scope) {
            this.scopes.push(scope);
        }

        private void onComplete(Runnable completion) {
            this.completion = completion;
        }

        private void complete() {
            this.completion.run();
        }

        @Override
        public void close() {
            if (this.closed) {
                return;
            }
            if (ACTIVE.get() != this.frame) {
                throw new IllegalStateException("Slot execution is unbalanced");
            }
            this.closed = true;
            RuntimeException failure = null;
            while (!this.scopes.isEmpty()) {
                try {
                    this.scopes.pop().close();
                } catch (Exception error) {
                    RuntimeException closeError = error instanceof RuntimeException runtime
                        ? runtime
                        : new RuntimeException(error);
                    if (failure == null) {
                        failure = closeError;
                    } else {
                        failure.addSuppressed(closeError);
                    }
                }
            }
            if (this.previous == null) {
                ACTIVE.remove();
            } else {
                ACTIVE.set(this.previous);
            }
            if (failure != null) {
                throw failure;
            }
        }
    }

    private static final class Holder<T> {
        private T value;
    }
}
