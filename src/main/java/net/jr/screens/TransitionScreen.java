package net.jr.screens;

import java.util.Objects;
import net.jr.api.client.ui.DslScreen;
import net.jr.api.client.ui.UiScreenFile;
import net.jr.client.ui.LoadingTextProvider;
import net.jr.registry.ModUi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.multiplayer.LevelLoadTracker;
import net.minecraft.network.chat.Component;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.level.progress.ChunkLoadStatusView;
import net.minecraft.world.level.chunk.status.ChunkStatus;

public final class TransitionScreen extends LevelLoadingScreen implements DslScreen {
    private static final int TIP_INTERVAL_TICKS = 80;
    private static final int DOT_INTERVAL_TICKS = 10;
    private static final int LOADING_CALLS_PER_LOGICAL_TICK = 3;

    private LevelLoadTracker loadTracker;
    private final LoadingTextProvider loadingTexts;
    private int dotTicks;
    private int loadingTickCalls;

    public TransitionScreen(
        LevelLoadTracker loadTracker,
        Reason reason,
        Component currentTip,
        int currentTipElapsedTicks
    ) {
        super(loadTracker, reason);
        this.loadTracker = Objects.requireNonNull(loadTracker, "loadTracker");
        this.loadingTexts = new LoadingTextProvider(
            TIP_INTERVAL_TICKS,
            Objects.requireNonNull(currentTip, "currentTip"),
            currentTipElapsedTicks
        );
    }

    @Override
    public void update(LevelLoadTracker loadTracker, Reason reason) {
        super.update(loadTracker, reason);
        this.loadTracker = Objects.requireNonNull(loadTracker, "loadTracker");
    }

    @Override
    public UiScreenFile dslFile() {
        return ModUi.TRANSITION_SCREEN;
    }

    @Override
    protected void init() {
        this.dslData().exposeNumber(
            "loading.progress",
            this::loadingProgress
        );
        this.dslData().exposeText(
            "loading.message",
            () -> this.loadingMessage().getString()
        );
        this.dslData().exposeText(
            "status.message",
            () -> this.statusMessage().getString()
        );
        this.dslData().exposeText(
            "game.tip",
            () -> this.loadingTexts.current().getString()
        );
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.advanceLogicalTick()) {
            return;
        }
        this.dotTicks++;
        this.loadingTexts.tick();
    }

    private boolean advanceLogicalTick() {
        Minecraft minecraft = Minecraft.getInstance();
        IntegratedServer server = minecraft.getSingleplayerServer();
        boolean fastLoadingLoop = server != null
            && (!server.isReady() || minecraft.gui.overlay() != null);
        if (!fastLoadingLoop) {
            this.loadingTickCalls = 0;
            return true;
        }

        this.loadingTickCalls++;
        if (this.loadingTickCalls < LOADING_CALLS_PER_LOGICAL_TICK) {
            return false;
        }
        this.loadingTickCalls = 0;
        return true;
    }

    public Component loadingMessage() {
        int dots = this.dotTicks / DOT_INTERVAL_TICKS % 3 + 1;
        String message = Component.translatable(
            "transition.java_reforged.loading.integrated_server"
        ).getString();
        return Component.literal(message + ".".repeat(dots));
    }

    public float loadingProgress() {
        if (!this.loadTracker.hasProgress()) {
            return 0.0F;
        }
        float progress = this.loadTracker.serverProgress();
        return Float.isFinite(progress)
            ? Math.max(0.0F, Math.min(progress, 1.0F))
            : 0.0F;
    }

    public Component statusMessage() {
        ChunkStatus status = this.currentChunkStatus();
        return resolved(statusTranslationKey(status));
    }

    private ChunkStatus currentChunkStatus() {
        ChunkLoadStatusView view = this.loadTracker.statusView();
        if (view == null) {
            return ChunkStatus.EMPTY;
        }
        int center = view.radius();
        ChunkStatus status = view.get(center, center);
        return status == null ? ChunkStatus.EMPTY : status;
    }

    private static String statusTranslationKey(ChunkStatus status) {
        if (status == ChunkStatus.STRUCTURE_STARTS) {
            return "transition.java_reforged.status.structure_starts";
        }
        if (status == ChunkStatus.STRUCTURE_REFERENCES) {
            return "transition.java_reforged.status.structure_references";
        }
        if (status == ChunkStatus.BIOMES) {
            return "transition.java_reforged.status.biomes";
        }
        if (status == ChunkStatus.NOISE) {
            return "transition.java_reforged.status.noise";
        }
        if (status == ChunkStatus.SURFACE) {
            return "transition.java_reforged.status.surface";
        }
        if (status == ChunkStatus.CARVERS) {
            return "transition.java_reforged.status.carvers";
        }
        if (status == ChunkStatus.FEATURES) {
            return "transition.java_reforged.status.features";
        }
        if (status == ChunkStatus.INITIALIZE_LIGHT) {
            return "transition.java_reforged.status.initialize_light";
        }
        if (status == ChunkStatus.LIGHT) {
            return "transition.java_reforged.status.light";
        }
        if (status == ChunkStatus.SPAWN) {
            return "transition.java_reforged.status.spawn";
        }
        if (status == ChunkStatus.FULL) {
            return "transition.java_reforged.status.full";
        }
        return "transition.java_reforged.status.empty";
    }

    private static Component resolved(String key) {
        return Component.literal(Component.translatable(key).getString());
    }

    @Override
    public void extractBackground(
        GuiGraphicsExtractor graphics,
        int mouseX,
        int mouseY,
        float partialTick
    ) {
        this.extractPanorama(graphics, partialTick);
        this.extractBlurredBackground(graphics);
        this.extractMenuBackground(graphics);
        this.extractDslBackground(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void extractRenderState(
        GuiGraphicsExtractor graphics,
        int mouseX,
        int mouseY,
        float partialTick
    ) {
        this.extractDslContent(graphics, mouseX, mouseY, partialTick);
        this.extractDslForeground(graphics, mouseX, mouseY, partialTick);
    }
}
