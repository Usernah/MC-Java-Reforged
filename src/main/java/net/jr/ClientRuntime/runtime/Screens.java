package net.jr.ClientRuntime.runtime;

import javax.annotation.Nullable;
import net.jr.client.input.InputApi;
import net.jr.ClientRuntime.slot.PlayerSlot;
import net.jr.ClientRuntime.slot.PlayerSlots;
import net.jr.ClientRuntime.viewport.ViewportArea;
import net.jr.ClientRuntime.input.ScreenInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.jr.client.input.cursor.CursorHider;
import net.jr.client.input.cursor.CursorRenderer;
import net.jr.client.input.mode.InputMode;
import net.jr.client.input.runtime.GamepadInputProcessor;
import net.jr.client.ui.hint.ControlHintPipeline;
import net.jr.client.ui.navigation.UiInputModeController;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.client.ClientHooks;
import java.util.Stack;

public final class Screens {
    private Screens() {
    }

    public static void bootstrapPrimary(Minecraft minecraft, Screen screen) {
        ClientBoundary.runPrimary(minecraft, () -> minecraft.gui.setScreen(screen));
    }

    public static void initialize(Screen screen, Minecraft minecraft, int vanillaWidth, int vanillaHeight) {
        LocalClient active = Client.currentOrNull();
        int slotId = active == null ? 0 : active.slotId();
        ViewportArea viewport = Client.viewportOrNull(slotId);
        int width = viewport != null ? ScreenScale.logicalWidth(viewport) : vanillaWidth;
        int height = viewport != null ? ScreenScale.logicalHeight(viewport) : vanillaHeight;
        screen.init(width, height);
    }


    public static void resizeAll(Minecraft minecraft) {
        if (!LocalPlayers.INSTANCE.hasWindowMetrics()) {
            return;
        }

        for (int slotId = 0; slotId < PlayerSlots.MAX_SLOTS; slotId++) {
            PlayerSlot slot = LocalPlayers.INSTANCE.slots().slot(slotId);
            Screen screen = slot.screenState().screen();
            if (!slot.connected() || screen == null || !slot.hasViewport()) {
                continue;
            }

            ViewportArea viewport = slot.viewport();
            int width = ScreenScale.logicalWidth(viewport);
            int height = ScreenScale.logicalHeight(viewport);
            if (screen.width == width && screen.height == height) {
                continue;
            }

            LocalClientScope.runClient(slot, client ->
                    ScreenInput.runEvent(
                            () -> screen.resize(width, height),
                            "Resizing screen",
                            screen.getClass().getCanonicalName()
                    )
            );
        }
    }

    public static void tickSecondary() {
        for (int slotId = 1; slotId < PlayerSlots.MAX_SLOTS; slotId++) {
            PlayerSlot slot = LocalPlayers.INSTANCE.slots().slot(slotId);
            Screen screen = slot.screenState().screen();

            if (!slot.connected() || screen == null) {
                continue;
            }

            LocalClientScope.runClient(slot, client ->
                    ScreenInput.runEvent(
                            screen::tick,
                            "Ticking screen",
                            screen.getClass().getCanonicalName()
                    )
            );
        }
    }

    public static void extract(
            Minecraft minecraft,
            GuiGraphicsExtractor graphics,
            float partialTick
    ) {
        LocalClient client = Client.current();
        UiInputModeController.updateCurrentClientFrame(minecraft);
        PlayerSlot slot = client.rawSlot();
        if (minecraft.gui.overlay() != null || !slot.hasViewport()) {
            return;
        }

        Screen screen = client.screen();
        ViewportArea viewport = client.viewport();

        if (screen != null) {
            int mouseX = logicalMouseX(minecraft, slot, viewport);
            int mouseY = logicalMouseY(minecraft, slot, viewport);
            ScreenInput.runEvent(
                    () -> ClientHooks.extractScreen(
                            screen,
                            new Stack<>(),
                            graphics,
                            mouseX,
                            mouseY,
                            partialTick
                    ),
                    "Extracting slot screen render state",
                    screen.getClass().getCanonicalName()
            );
            ControlHintPipeline.renderScreen(screen, graphics);
            CursorRenderer.renderForCurrentClient(graphics, minecraft);
            return;
        }
    }

    public static void onSlotScreenChanged(Minecraft minecraft, int slotId, @Nullable Screen screen) {
        if (screen == null) {
            CursorHider.clearHiddenForSlot(slotId);
            CursorHider.clearReplacementHiddenForSlot(slotId);
            GamepadInputProcessor.releaseFocusedSlotCursor(slotId);
        } else {
            if (shouldSeedControllerCursor(slotId)) {
                GamepadInputProcessor.centerControllerCursorForScreen(slotId);
            }
            CursorHider.setReplacementHiddenForSlot(slotId, true);
        }
        CursorHider.sync();
    }

    private static boolean shouldSeedControllerCursor(int slotId) {
        return InputApi.hasGamepadForClient(slotId)
                && (Client.input(slotId).mode() == InputMode.GAMEPAD
                    || !InputApi.canPhysicalMouseDriveClient(slotId));
    }

    public static boolean shouldOwnMouseModeTransitions() {
        return slotUiPassOwnsScreens();
    }

    public static boolean slotUiPassOwnsScreens() {
        if (!LocalPlayers.INSTANCE.hasWindowMetrics()) {
            return false;
        }

        return hasRenderableGameplayPass();
    }

    public static boolean hasMultipleLocalViewports() {
        return slotUiPassOwnsScreens() && Client.connectedCount() > 1;
    }

    static boolean slotUiPassCanRender(PlayerSlot slot) {
        if (!slot.drawable()) {
            return false;
        }
        if (slot.screenState().screen() != null) {
            return true;
        }
        return LocalPlayers.INSTANCE.slotGameplayReady(slot);
    }

    public static boolean shouldCaptureScreenForSlot(PlayerSlot slot, @Nullable Screen screen) {
        return screen != null;
    }

    private static boolean hasRenderableGameplayPass() {
        for (PlayerSlot slot : LocalPlayers.INSTANCE.slots().visibleSlots()) {
            if (slot.connected()
                    && slot.hasViewport()
                    && LocalPlayers.INSTANCE.slotGameplayReady(slot)) {
                return true;
            }
        }
        return false;
    }

    public static boolean usesLocalPause() {
        return true;
    }

    public static void openLocalPause(Minecraft minecraft, boolean pauseOnly) {
        if (Client.screen() != null) {
            return;
        }
        // Keep vanilla's normal menu/full-menu rule, but the pause flag itself is neutralized by MinecraftPauseMixin.
        minecraft.gui.setScreen(new PauseScreen(!pauseOnly));
    }

    @Nullable
    public static Screen activeScreenOrNull() {
        return LocalPlayers.INSTANCE.activeSlot().screenState().screen();
    }

    private static int logicalMouseX(Minecraft minecraft, PlayerSlot slot, ViewportArea viewport) {
        if (GamepadInputProcessor.isControllerCursorActive(slot.id())) {
            int localGuiX = GamepadInputProcessor.resolveScreenMouseX(slot.id(), (int)Math.round(GamepadInputProcessor.cursorX(slot.id())));
            return ScreenScale.logicalMouseX(viewport, localGuiX);
        }
        if (!InputApi.canPhysicalMouseDriveClient(slot.id())) {
            return Integer.MIN_VALUE;
        }
        int localGuiX = GamepadInputProcessor.resolveScreenMouseX(
                slot.id(),
                (int)viewport.windowMouseToLocalGuiX(minecraft.mouseHandler.xpos())
        );
        return ScreenScale.logicalMouseX(viewport, localGuiX);
    }

    private static int logicalMouseY(Minecraft minecraft, PlayerSlot slot, ViewportArea viewport) {
        if (GamepadInputProcessor.isControllerCursorActive(slot.id())) {
            int localGuiY = GamepadInputProcessor.resolveScreenMouseY(slot.id(), (int)Math.round(GamepadInputProcessor.cursorY(slot.id())));
            return ScreenScale.logicalMouseY(viewport, localGuiY);
        }
        if (!InputApi.canPhysicalMouseDriveClient(slot.id())) {
            return Integer.MIN_VALUE;
        }
        int localGuiY = GamepadInputProcessor.resolveScreenMouseY(
                slot.id(),
                (int)viewport.windowMouseToLocalGuiY(minecraft.mouseHandler.ypos())
        );
        return ScreenScale.logicalMouseY(viewport, localGuiY);
    }
}
