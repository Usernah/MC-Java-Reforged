package net.jr.ClientRuntime.runtime;

import com.mojang.blaze3d.platform.Window;
import java.util.Objects;
import net.jr.ClientConfig;
import net.jr.ClientRuntime.input.InputFocus;
import net.jr.ClientRuntime.network.ConnectionSlots;
import net.jr.ClientRuntime.player.PlayerSessions;
import net.jr.ClientRuntime.slot.PlayerSlot;
import net.jr.ClientRuntime.slot.PlayerSlots;
import net.jr.ClientRuntime.viewport.WindowMetrics;
import net.jr.api.client.split.SplitOrientation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.Connection;

public final class LocalPlayers {
    public static final LocalPlayers INSTANCE = new LocalPlayers();

    private final PlayerSlots slots = new PlayerSlots();
    private final InputFocus inputFocus = new InputFocus(PlayerSlots.MAX_SLOTS);
    private final ConnectionSlots connections = new ConnectionSlots();
    private final PlayerSessions sessions = new PlayerSessions();
    private WindowMetrics windowMetrics;
    private int testPlayerCount = 1;

    private LocalPlayers() {
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
        Screens.resizeAll(minecraft);
    }

    private static boolean validWindowMetric(int value) {
        return value > 0;
    }

    public void cycleTestPlayerCount(Minecraft minecraft) {
        Objects.requireNonNull(minecraft, "minecraft");
        this.testPlayerCount = this.testPlayerCount == PlayerSlots.MAX_SLOTS ? 1 : this.testPlayerCount + 1;
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
        for (int slotId = 1; slotId < PlayerSlots.MAX_SLOTS; slotId++) {
            PlayerSlot slot = this.slots.slot(slotId);
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
        if (slotId <= 0 || slotId >= PlayerSlots.MAX_SLOTS) {
            throw new IllegalArgumentException("Only secondary local clients can be disconnected here: " + slotId);
        }
        this.sessions.disconnectSecondaryClient(this, slotId);
        PlayerSlot slot = this.slots.slot(slotId);
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
        Screens.resizeAll(minecraft);
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
        Screens.resizeAll(minecraft, true);
    }

    public PlayerSlot activeSlot() {
        return this.slots.slot(ActiveSlot.requireId());
    }

    public PlayerSlot runtimeSlot() {
        Integer activeSlotId = ActiveSlot.idOrNull();
        if (activeSlotId != null) {
            return this.slots.slot(activeSlotId);
        }

        // Si no hay un ID activo en el hilo (como en el limbo del Respawn del Slot 0),
        // devolvemos el slot principal por defecto en lugar de tumbar la conexión.
        // Esto absorbe los vacíos de cámara nula y carga del jugador principal.
        return this.primarySlot();
    }


    public PlayerSlot primarySlot() {
        return this.slots.slot(0);
    }

    public boolean slotWorldBound(PlayerSlot slot) {
        LocalPlayer player = slot.gameplayState().player();
        ClientLevel level = slot.renderState().level();
        return slot.drawable()
                && player != null
                && level != null
                && player.level() == level
                && !player.isRemoved();
    }

    public boolean slotGameplayBound(PlayerSlot slot) {
        MultiPlayerGameMode gameMode = slot.gameplayState().gameMode();
        return this.slotWorldBound(slot)
                && gameMode != null;
    }

    public boolean slotWorldReady(PlayerSlot slot) {
        return this.slotWorldBound(slot)
                && !this.sessions.isJoining(slot.id());
    }

    public boolean slotGameplayReady(PlayerSlot slot) {
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

    public PlayerSlot slotForExecutionOrThrow(int slotId) {
        return this.slots.slot(slotId);
    }

    public PlayerSlots slots() {
        return this.slots;
    }

    public InputFocus inputFocus() {
        return this.inputFocus;
    }

    public ConnectionSlots connections() {
        return this.connections;
    }

    public PlayerSessions sessions() {
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
