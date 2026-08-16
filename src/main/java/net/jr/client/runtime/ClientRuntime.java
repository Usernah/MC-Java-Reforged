package net.jr.client.runtime;

import com.mojang.blaze3d.platform.Window;
import java.util.Objects;
import net.jr.ClientConfig;
import net.jr.client.runtime.input.InputFocus;
import net.jr.client.runtime.network.ConnectionSlotRegistry;
import net.jr.client.runtime.session.LocalClientSessions;
import net.jr.client.runtime.context.ActiveClientSlot;
import net.jr.client.runtime.ui.LocalScreenManager;
import net.jr.client.runtime.slot.LocalClientSlot;
import net.jr.client.runtime.slot.LocalClientSlots;
import net.jr.client.runtime.viewport.WindowMetrics;
import net.jr.api.client.split.SplitOrientation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.Connection;

public final class ClientRuntime {
    public static final ClientRuntime INSTANCE = new ClientRuntime();

    private final LocalClientSlots slots = new LocalClientSlots();
    private final InputFocus inputFocus = new InputFocus(LocalClientSlots.MAX_SLOTS);
    private final ConnectionSlotRegistry connections = new ConnectionSlotRegistry();
    private final LocalClientSessions sessions = new LocalClientSessions();
    private WindowMetrics windowMetrics;
    private int testPlayerCount = 1;

    private ClientRuntime() {
        this.slots.setTwoPlayerOrientation(ClientConfig.splitOrientation());
        this.slots.setVisiblePlayerCount(this.testPlayerCount);
    }

    public void refreshWindow(Minecraft minecraft) {
        Objects.requireNonNull(minecraft, "minecraft");
        Window window = minecraft.getWindow();
        if (window == null) {
            return;
        }

        int windowWidth = window.getScreenWidth();
        int windowHeight = window.getScreenHeight();
        int framebufferWidth = window.getWidth();
        int framebufferHeight = window.getHeight();
        int guiWidth = window.getGuiScaledWidth();
        int guiHeight = window.getGuiScaledHeight();
        double guiScale = window.getGuiScale();

        if (!validWindowMetric(windowWidth)
            || !validWindowMetric(windowHeight)
            || !validWindowMetric(framebufferWidth)
            || !validWindowMetric(framebufferHeight)
            || !validWindowMetric(guiWidth)
            || !validWindowMetric(guiHeight)
            || guiScale <= 0.0D
            || Double.isNaN(guiScale)
            || Double.isInfinite(guiScale)
        ) {
            return;
        }

        WindowMetrics metrics = new WindowMetrics(
            windowWidth,
            windowHeight,
            framebufferWidth,
            framebufferHeight,
            guiWidth,
            guiHeight,
            (int)Math.max(1.0D, Math.round(guiScale)),
            guiScale
        );
        if (metrics.equals(this.windowMetrics)) {
            return;
        }
        this.windowMetrics = metrics;
        this.slots.rebuildViewports(metrics);
        LocalScreenManager.resizeAll(minecraft);
    }

    private static boolean validWindowMetric(int value) {
        return value > 0;
    }

    public void cycleTestPlayerCount(Minecraft minecraft) {
        Objects.requireNonNull(minecraft, "minecraft");
        this.testPlayerCount = this.testPlayerCount == LocalClientSlots.MAX_SLOTS ? 1 : this.testPlayerCount + 1;
        this.setTestPlayerCount(minecraft, this.testPlayerCount);
    }

    public void setTestPlayerCount(Minecraft minecraft, int playerCount) {
        Objects.requireNonNull(minecraft, "minecraft");
        this.testPlayerCount = playerCount;
        this.refreshWindow(minecraft);
        this.slots.setVisiblePlayerCount(playerCount);
        this.applyLayoutTransition(minecraft);
        this.inputFocus.clampToSlotCount(playerCount);
        this.sessions.ensurePlayerCount(minecraft, this, playerCount);
    }

    public int joinNextClient(Minecraft minecraft) {
        Objects.requireNonNull(minecraft, "minecraft");
        for (int slotId = 1; slotId < LocalClientSlots.MAX_SLOTS; slotId++) {
            LocalClientSlot slot = this.slots.slot(slotId);
            if (slot.connected()) {
                continue;
            }
            this.refreshWindow(minecraft);
            this.slots.setClientConnected(slotId, true);
            this.applyLayoutTransition(minecraft);
            this.inputFocus.clampToSlotCount(this.slots.presentSlotCount());
            this.sessions.ensureClient(minecraft, this, slotId);
            return slotId;
        }
        throw new IllegalStateException("All local clients are already connected");
    }

    public void disconnectSecondaryClient(Minecraft minecraft, int slotId) {
        Objects.requireNonNull(minecraft, "minecraft");
        if (slotId <= 0 || slotId >= LocalClientSlots.MAX_SLOTS) {
            throw new IllegalArgumentException("Only secondary local clients can be disconnected here: " + slotId);
        }
        this.sessions.disconnectSecondaryClient(this, slotId);
        LocalClientSlot slot = this.slots.slot(slotId);
        slot.clearWorldBinding();
        this.slots.setClientConnected(slotId, false);
        this.inputFocus.clampToSlotCount(this.slots.presentSlotCount());
        this.applyLayoutTransition(minecraft);
    }

    public void cycleTestInputFocus(Minecraft minecraft) {
        Objects.requireNonNull(minecraft, "minecraft");
        this.inputFocus.focusNext(this.slots.presentSlotCount());
    }

    public void returnToPrimaryOnly(Minecraft minecraft) {
        Objects.requireNonNull(minecraft, "minecraft");
        this.testPlayerCount = 1;
        this.slots.setVisiblePlayerCount(1);
        this.inputFocus.clampToSlotCount(1);
        this.applyLayoutTransition(minecraft);
    }

    public void refreshViewportOptions(Minecraft minecraft) {
        Objects.requireNonNull(minecraft, "minecraft");
        if (this.windowMetrics == null) {
            this.refreshWindow(minecraft);
            return;
        }
        this.slots.rebuildViewports(this.windowMetrics);
        LocalScreenManager.resizeAll(minecraft);
    }

    public void setTwoPlayerOrientation(Minecraft minecraft, SplitOrientation orientation) {
        Objects.requireNonNull(minecraft, "minecraft");
        Objects.requireNonNull(orientation, "orientation");
        this.slots.setTwoPlayerOrientation(orientation);
        if (this.windowMetrics != null) {
            this.applyLayoutTransition(minecraft);
        }
    }

    private void applyLayoutTransition(Minecraft minecraft) {
        minecraft.resizeGui();
        this.refreshWindow(minecraft);
        LocalScreenManager.resizeAll(minecraft, true);
    }

    public LocalClientSlot activeSlot() {
        return this.slots.slot(ActiveClientSlot.requireId());
    }

    public LocalClientSlot runtimeSlot() {
        Integer activeSlotId = ActiveClientSlot.idOrNull();
        if (activeSlotId != null) {
            return this.slots.slot(activeSlotId);
        }

        // Si no hay un ID activo en el hilo (como en el limbo del Respawn del Slot 0),
        // devolvemos el slot principal por defecto en lugar de tumbar la conexión.
        // Esto absorbe los vacíos de cámara nula y carga del jugador principal.
        return this.primarySlot();
    }


    public LocalClientSlot primarySlot() {
        return this.slots.slot(0);
    }

    public boolean slotWorldBound(LocalClientSlot slot) {
        LocalPlayer player = slot.gameplayState().player();
        ClientLevel level = slot.renderState().level();
        return slot.drawable()
                && player != null
                && level != null
                && player.level() == level
                && !player.isRemoved();
    }

    public boolean slotGameplayBound(LocalClientSlot slot) {
        MultiPlayerGameMode gameMode = slot.gameplayState().gameMode();
        return this.slotWorldBound(slot)
                && gameMode != null;
    }

    public boolean slotWorldReady(LocalClientSlot slot) {
        return this.slotWorldBound(slot)
                && !this.sessions.isJoining(slot.id());
    }

    public boolean slotGameplayReady(LocalClientSlot slot) {
        return this.slotGameplayBound(slot)
                && !this.sessions.isJoining(slot.id());
    }

    public ClientPacketListener primaryPacketListener() {
        LocalPlayer player = this.primarySlot().gameplayState().player();
        return player != null ? player.connection : null;
    }

    public Connection primaryConnection() {
        ClientPacketListener listener = this.primaryPacketListener();
        return listener != null ? listener.getConnection() : null;
    }

    public boolean isPrimaryPacketListener(ClientPacketListener listener) {
        return listener != null && listener == this.primaryPacketListener();
    }

    public int slotForClientPacketListener(ClientPacketListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener cannot be null");
        }
        Connection connection = listener.getConnection();
        if (connection != null) {
            Integer mappedSlot = this.connections.slotOrNull(connection);
            if (mappedSlot != null) {
                return mappedSlot;
            }
        }
        if (this.isPrimaryPacketListener(listener)) {
            return 0;
        }
        throw new IllegalStateException("No player slot is bound to ClientPacketListener " + listener);
    }

    public LocalClientSlot slotForExecutionOrThrow(int slotId) {
        return this.slots.slot(slotId);
    }

    public LocalClientSlots slots() {
        return this.slots;
    }

    public InputFocus inputFocus() {
        return this.inputFocus;
    }

    public ConnectionSlotRegistry connections() {
        return this.connections;
    }

    public LocalClientSessions sessions() {
        return this.sessions;
    }

    public boolean hasWindowMetrics() {
        return this.windowMetrics != null;
    }

    public WindowMetrics windowMetrics() {
        if (this.windowMetrics == null) {
            throw new IllegalStateException("Window metrics have not been captured yet");
        }
        return this.windowMetrics;
    }
}
