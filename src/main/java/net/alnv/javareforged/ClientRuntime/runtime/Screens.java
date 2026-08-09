package net.alnv.javareforged.ClientRuntime.runtime;

import javax.annotation.Nullable;
import net.alnv.javareforged.client.input.InputApi;
import net.alnv.javareforged.ClientRuntime.slot.PlayerSlot;
import net.alnv.javareforged.ClientRuntime.slot.PlayerSlots;
import net.alnv.javareforged.ClientRuntime.viewport.ViewportArea;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.alnv.javareforged.client.input.cursor.CursorHider;
import net.alnv.javareforged.client.input.cursor.CursorRenderer;
import net.alnv.javareforged.client.input.mode.InputMode;
import net.alnv.javareforged.client.input.runtime.GamepadInputProcessor;
import net.alnv.javareforged.client.ui.hint.ControlHintPipeline;
import net.alnv.javareforged.client.ui.navigation.UiInputModeController;
import net.alnv.javareforged.screens.ClosingContainerVisuals;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.client.ClientHooks;

public final class Screens {
    private Screens() {
    }

    public static void bootstrapPrimary(Minecraft minecraft, Screen screen) {
        ClientBoundary.runPrimary(minecraft, () -> minecraft.setScreen(screen));
    }

    public static void initialize(Screen screen, Minecraft minecraft, int vanillaWidth, int vanillaHeight) {
        LocalClient client = Client.current();
        int width = client.hasViewport() ? ScreenScale.logicalWidth(client.viewport()) : vanillaWidth;
        int height = client.hasViewport() ? ScreenScale.logicalHeight(client.viewport()) : vanillaHeight;
        screen.init(minecraft, width, height);
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
                    Screen.wrapScreenError(
                            () -> screen.resize(minecraft, width, height),
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
                    Screen.wrapScreenError(
                            screen::tick,
                            "Ticking screen",
                            screen.getClass().getCanonicalName()
                    )
            );
        }
    }

    public static void render(
            Minecraft minecraft,
            GuiGraphics graphics,
            float partialTick
    ) {
        LocalClient client = Client.current();
        UiInputModeController.updateCurrentClientFrame(minecraft);
        PlayerSlot slot = client.rawSlot();
        if (minecraft.getOverlay() != null || !slot.hasViewport()) {
            return;
        }

        Screen screen = client.screen();
        ViewportArea viewport = client.viewport();

        if (screen != null) {
            int mouseX = logicalMouseX(minecraft, slot, viewport);
            int mouseY = logicalMouseY(minecraft, slot, viewport);
            GuiGraphics localGraphics = new GuiGraphics(minecraft, graphics.bufferSource());

            try {
                try (ScreenScale.Scope ignored = ScreenScale.enter(viewport)) {
                    Screen.wrapScreenError(
                            () -> ClientHooks.drawScreen(
                                    screen,
                                    localGraphics,
                                    mouseX,
                                    mouseY,
                                    partialTick
                            ),
                            "Rendering slot screen",
                            screen.getClass().getCanonicalName()
                    );
                    localGraphics.flush();
                }

                ControlHintPipeline.renderScreen(screen, localGraphics);
                CursorRenderer.renderForCurrentClient(localGraphics, minecraft);
            } finally {
                localGraphics.flush();
            }
            return;
        }

        ClosingContainerVisuals.renderForSlot(minecraft, graphics, slot, partialTick);
    }

    public static void drawVanilla(
            Screen screen,
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        // Slot screens are rendered by the slot UI pass, even when only slot 0 is
        // visible. If a captured screen is drawable by a slot, the vanilla global
        // draw path would be a second owner and can only target the global window.
        if (slotUiPassOwnsScreens()) {
            return;
        }

        ClientHooks.drawScreen(
                screen,
                graphics,
                mouseX,
                mouseY,
                partialTick
        );
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
        minecraft.setScreen(new PauseScreen(!pauseOnly));
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
