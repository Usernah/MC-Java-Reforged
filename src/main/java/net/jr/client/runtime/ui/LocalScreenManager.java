package net.jr.client.runtime.ui;

import javax.annotation.Nullable;
import net.jr.client.input.InputApi;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.context.LocalClientAcces;
import net.jr.client.runtime.context.LocalClientExecution;
import net.jr.client.runtime.context.LocalClient;
import net.jr.client.runtime.context.LocalClientScope;
import net.jr.client.runtime.slot.LocalClientSlot;
import net.jr.client.runtime.slot.LocalClientSlots;
import net.jr.client.runtime.viewport.ViewportGuiScale;
import net.jr.client.runtime.viewport.ViewportArea;
import net.jr.client.runtime.input.LocalScreenInput;
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

public final class LocalScreenManager {
    private LocalScreenManager() {
    }

    public static void bootstrapPrimary(Minecraft minecraft, Screen screen) {
        LocalClientExecution.runPrimary(minecraft, () -> minecraft.gui.setScreen(screen));
    }

    public static void initialize(Screen screen, Minecraft minecraft, int vanillaWidth, int vanillaHeight) {
        LocalClient active = LocalClientAcces.currentOrNull();
        int slotId = active == null ? 0 : active.slotId();
        ViewportArea viewport = LocalClientAcces.viewportOrNull(slotId);
        int width = viewport != null ? ViewportGuiScale.logicalWidth(viewport) : vanillaWidth;
        int height = viewport != null ? ViewportGuiScale.logicalHeight(viewport) : vanillaHeight;
        screen.init(width, height);
    }


    public static void resizeAll(Minecraft minecraft) {
        resizeAll(minecraft, false);
    }

    public static void resizeAll(Minecraft minecraft, boolean layoutChanged) {
        if (!ClientRuntime.INSTANCE.hasWindowMetrics()) {
            return;
        }

        for (int slotId = 0; slotId < LocalClientSlots.MAX_SLOTS; slotId++) {
            LocalClientSlot slot = ClientRuntime.INSTANCE.slots().slot(slotId);
            Screen screen = slot.screenState().screen();
            if (!slot.connected() || screen == null || !slot.hasViewport()) {
                continue;
            }

            ViewportArea viewport = slot.viewport();
            int width = ViewportGuiScale.logicalWidth(viewport);
            int height = ViewportGuiScale.logicalHeight(viewport);
            if (!layoutChanged && screen.width == width && screen.height == height) {
                continue;
            }

            LocalClientScope.run(slot, client ->
                    LocalScreenInput.runEvent(
                            () -> screen.resize(width, height),
                            "Resizing screen",
                            screen.getClass().getCanonicalName()
                    )
            );
        }
    }

    public static void tickSecondary() {
        for (int slotId = 1; slotId < LocalClientSlots.MAX_SLOTS; slotId++) {
            LocalClientSlot slot = ClientRuntime.INSTANCE.slots().slot(slotId);
            Screen screen = slot.screenState().screen();

            if (!slot.connected() || screen == null) {
                continue;
            }

            LocalClientScope.run(slot, client ->
                    LocalScreenInput.runEvent(
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
        LocalClient client = LocalClientAcces.current();
        UiInputModeController.updateCurrentClientFrame(minecraft);
        LocalClientSlot slot = client.slot();
        if (minecraft.gui.overlay() != null || !slot.hasViewport()) {
            return;
        }

        ViewportArea viewport = client.viewport();
        Screen screen = client.screen();
        if (screen == null && SecondaryWorldLoadingScreen.shouldPresent(slot)) {
            screen = SecondaryWorldLoadingScreen.forSlot(slot.id());
            int width = ViewportGuiScale.logicalWidth(viewport);
            int height = ViewportGuiScale.logicalHeight(viewport);
            if (screen.width != width || screen.height != height) {
                screen.init(width, height);
            }
        }
        if (screen != null) {
            Screen renderScreen = screen;
            int mouseX = logicalMouseX(minecraft, slot, viewport);
            int mouseY = logicalMouseY(minecraft, slot, viewport);
            LocalScreenInput.runEvent(
                    () -> ClientHooks.extractScreen(
                            renderScreen,
                            new Stack<>(),
                            graphics,
                            mouseX,
                            mouseY,
                            partialTick
                    ),
                    "Extracting slot screen render state",
                    renderScreen.getClass().getCanonicalName()
            );
            ControlHintPipeline.renderScreen(renderScreen, graphics);
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
                && (LocalClientAcces.input(slotId).mode() == InputMode.GAMEPAD
                    || !InputApi.canPhysicalMouseDriveClient(slotId));
    }

    public static boolean shouldOwnMouseModeTransitions() {
        return slotUiPassOwnsScreens();
    }

    public static boolean slotUiPassOwnsScreens() {
        if (!ClientRuntime.INSTANCE.hasWindowMetrics()) {
            return false;
        }

        return hasRenderableGameplayPass();
    }

    public static boolean hasMultipleLocalViewports() {
        return slotUiPassOwnsScreens() && LocalClientAcces.connectedCount() > 1;
    }

    public static boolean slotUiPassCanRender(LocalClientSlot slot) {
        if (!slot.drawable()) {
            return false;
        }
        if (slot.screenState().screen() != null) {
            return true;
        }
        if (SecondaryWorldLoadingScreen.shouldPresent(slot)) {
            return true;
        }
        return ClientRuntime.INSTANCE.slotGameplayReady(slot);
    }

    public static boolean shouldCaptureScreenForSlot(LocalClientSlot slot, @Nullable Screen screen) {
        return screen != null;
    }

    private static boolean hasRenderableGameplayPass() {
        for (LocalClientSlot slot : ClientRuntime.INSTANCE.slots().visibleSlots()) {
            if (slot.connected()
                    && slot.hasViewport()
                    && ClientRuntime.INSTANCE.slotGameplayReady(slot)) {
                return true;
            }
        }
        return false;
    }

    public static boolean usesLocalPause() {
        return true;
    }

    public static void openLocalPause(Minecraft minecraft, boolean pauseOnly) {
        if (LocalClientAcces.screen() != null) {
            return;
        }
        // Keep vanilla's normal menu/full-menu rule, but the pause flag itself is neutralized by MinecraftPauseMixin.
        minecraft.gui.setScreen(new PauseScreen(!pauseOnly));
    }

    @Nullable
    public static Screen activeScreenOrNull() {
        return ClientRuntime.INSTANCE.activeSlot().screenState().screen();
    }

    private static int logicalMouseX(Minecraft minecraft, LocalClientSlot slot, ViewportArea viewport) {
        if (GamepadInputProcessor.isControllerCursorActive(slot.id())) {
            return GamepadInputProcessor.resolveScreenMouseX(
                    slot.id(),
                    (int)Math.round(GamepadInputProcessor.cursorX(slot.id()))
            );
        }
        if (!InputApi.canPhysicalMouseDriveClient(slot.id())) {
            return Integer.MIN_VALUE;
        }
        int localGuiX = GamepadInputProcessor.resolveScreenMouseX(
                slot.id(),
                (int)viewport.windowMouseToLocalGuiX(minecraft.mouseHandler.xpos())
        );
        return ViewportGuiScale.logicalMouseX(viewport, localGuiX);
    }

    private static int logicalMouseY(Minecraft minecraft, LocalClientSlot slot, ViewportArea viewport) {
        if (GamepadInputProcessor.isControllerCursorActive(slot.id())) {
            return GamepadInputProcessor.resolveScreenMouseY(
                    slot.id(),
                    (int)Math.round(GamepadInputProcessor.cursorY(slot.id()))
            );
        }
        if (!InputApi.canPhysicalMouseDriveClient(slot.id())) {
            return Integer.MIN_VALUE;
        }
        int localGuiY = GamepadInputProcessor.resolveScreenMouseY(
                slot.id(),
                (int)viewport.windowMouseToLocalGuiY(minecraft.mouseHandler.ypos())
        );
        return ViewportGuiScale.logicalMouseY(viewport, localGuiY);
    }
}
