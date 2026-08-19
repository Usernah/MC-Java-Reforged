package net.jr.screens.controller;

import net.jr.Java_reforged;
import net.jr.api.client.render.Draw;
import net.jr.client.input.InputApi;
import net.jr.client.input.gamepad.GamepadCalibrationRegistry;
import net.jr.client.input.gamepad.GamepadDigitalInput;
import net.jr.client.input.gamepad.GamepadIdentity;
import net.jr.client.input.gamepad.RawGamepadInputPress;
import net.jr.client.ui.hint.glyph.ControllerGlyph;
import net.jr.client.ui.hint.glyph.ControllerGlyphTheme;
import net.jr.client.ui.hint.glyph.GamepadGlyphThemeState;
import net.jr.client.ui.hint.render.GlyphTextureBounds;
import net.jr.client.ui.hint.render.GlyphTextureBoundsCache;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

public final class ControllerCalibrationScreen extends Screen implements ControllerMenuCaptureAware {
    private static final Component TITLE = Component.literal("Calibracion de mando");
    private static final String DEBUG_VERSION = "calibration-screen-v2";
    private static final long CAPTURE_COOLDOWN_MS = 250L;
    private static final long RELEASE_STABLE_MS = 80L;
    private static final long RESCAN_INTERVAL_MS = 1000L;
    private static final GamepadDigitalInput[] STEPS = {
        GamepadDigitalInput.BUTTON_DOWN,
        GamepadDigitalInput.BUTTON_RIGHT,
        GamepadDigitalInput.BUTTON_LEFT,
        GamepadDigitalInput.BUTTON_UP,
        GamepadDigitalInput.BUTTON_START,
        GamepadDigitalInput.BUTTON_SELECT,
        GamepadDigitalInput.BUTTON_GUIDE,
        GamepadDigitalInput.MISC_1,
        GamepadDigitalInput.TOUCHPAD_BUTTON,
        GamepadDigitalInput.STICK_LEFT_BUTTON,
        GamepadDigitalInput.STICK_RIGHT_BUTTON,
        GamepadDigitalInput.BUMPER_LEFT,
        GamepadDigitalInput.BUMPER_RIGHT,
        GamepadDigitalInput.TRIGGER_LEFT,
        GamepadDigitalInput.TRIGGER_RIGHT,
        GamepadDigitalInput.DPAD_UP,
        GamepadDigitalInput.DPAD_DOWN,
        GamepadDigitalInput.DPAD_LEFT,
        GamepadDigitalInput.DPAD_RIGHT,
        GamepadDigitalInput.STICK_LEFT_MOVE_UP,
        GamepadDigitalInput.STICK_LEFT_MOVE_DOWN,
        GamepadDigitalInput.STICK_LEFT_MOVE_LEFT,
        GamepadDigitalInput.STICK_LEFT_MOVE_RIGHT,
        GamepadDigitalInput.STICK_RIGHT_MOVE_UP,
        GamepadDigitalInput.STICK_RIGHT_MOVE_DOWN,
        GamepadDigitalInput.STICK_RIGHT_MOVE_LEFT,
        GamepadDigitalInput.STICK_RIGHT_MOVE_RIGHT
    };

    private final Screen lastScreen;
    private @Nullable GamepadIdentity selectedController;
    private long selectedDeviceId = -1L;
    private @Nullable String lastCapturedText;
    private boolean waitingForRelease;
    private long releaseStableSinceMs = -1L;
    private int stepIndex;
    private long ignoreInputUntilMs;
    private long nextRescanAtMs;
    private Button skipButton;

    public ControllerCalibrationScreen(Screen lastScreen) {
        super(TITLE);
        this.lastScreen = lastScreen;
    }

    @Override
    protected void init() {
        InputApi.updateGamepads();
        InputApi.requestGamepadRescan();
        InputApi.clearCalibrationPresses();
        GamepadCalibrationRegistry.get().ensureLoaded(minecraft);
        waitingForRelease = false;
        releaseStableSinceMs = -1L;
        nextRescanAtMs = Util.getMillis() + RESCAN_INTERVAL_MS;

        PanelBounds panel = panelBounds();
        int buttonY = panel.y + panel.height - 28;
        skipButton = addRenderableWidget(Button.builder(
            Component.literal("Saltar"),
            button -> skipCurrentStep()
        ).bounds(panel.centerX() - 155, buttonY, 150, 20).build());
        addRenderableWidget(Button.builder(
            Component.literal("Volver"),
            button -> onClose()
        ).bounds(panel.centerX() + 5, buttonY, 150, 20).build());

        Java_reforged.LOGGER.info("{} abierta. {}", DEBUG_VERSION, InputApi.gamepadDebugStatus());
    }

    @Override
    public void tick() {
        super.tick();
        tickRescan();
        skipButton.active = selectedController != null && !isComplete();

        if (waitingForRelease) {
            InputApi.clearCalibrationPresses();
            if (selectedDeviceId >= 0L && InputApi.hasActiveCalibrationInput(selectedDeviceId)) {
                releaseStableSinceMs = -1L;
                return;
            }

            long now = Util.getMillis();
            if (releaseStableSinceMs < 0L) {
                releaseStableSinceMs = now;
                return;
            }
            if (now - releaseStableSinceMs >= RELEASE_STABLE_MS) {
                waitingForRelease = false;
                releaseStableSinceMs = -1L;
                cooldown();
            }
            return;
        }

        if (Util.getMillis() < ignoreInputUntilMs || isComplete()) {
            return;
        }

        RawGamepadInputPress press = InputApi.pollCalibrationPress();
        if (press == null) {
            return;
        }
        if (selectedController == null) {
            selectedController = press.identity();
            selectedDeviceId = press.deviceId();
            stepIndex = 0;
            lastCapturedText = "Mando seleccionado: " + press.identity().displayName();
            Java_reforged.LOGGER.info(
                "Mando seleccionado para calibracion: {} ({}) deviceId={}",
                press.identity().displayName(),
                press.identity().key(),
                press.deviceId()
            );
            waitForRelease();
            cooldown();
            return;
        }

        if (press.deviceId() != selectedDeviceId) {
            Java_reforged.LOGGER.debug(
                "Entrada de calibracion ignorada: deviceId={} no coincide con seleccionado={}.",
                press.deviceId(),
                selectedDeviceId
            );
            return;
        }

        GamepadDigitalInput target = STEPS[stepIndex];
        GamepadCalibrationRegistry.get().setInput(selectedController, target, press.input());
        lastCapturedText = label(target) + " = " + press.input().displayName();
        Java_reforged.LOGGER.info(
            "Calibracion capturada: {} -> {} para {}.",
            target,
            press.input().displayName(),
            selectedController.displayName()
        );
        stepIndex++;
        waitForRelease();
        cooldown();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        PanelBounds panel = panelBounds();
        int centerX = panel.centerX();
        graphics.fill(0, 0, width, height, 0x66000000);
        graphics.fill(panel.x - 3, panel.y - 3, panel.x + panel.width + 3, panel.y + panel.height + 3, 0xFF111111);
        graphics.fill(panel.x - 1, panel.y - 1, panel.x + panel.width + 1, panel.y + panel.height + 1, 0xFFFFFFFF);
        graphics.fill(panel.x, panel.y, panel.x + panel.width, panel.y + panel.height, 0xFF202020);
        centered(graphics, title, centerX, panel.y + 10, 0xFFFFFFFF);
        centered(graphics, Component.literal(DEBUG_VERSION), centerX, panel.y + 22, 0xFF77FFAA);

        int y = panel.y + 44;
        if (selectedController == null) {
            centered(graphics, Component.literal("Activa ANALOG si tu mando tiene ese boton."), centerX, y, 0xFFFFDD55);
            centered(graphics, Component.literal("Luego presiona cualquier boton del mando que quieres calibrar."), centerX, y + 14, 0xFFFFFFFF);
            centered(graphics, Component.literal("El primer boton solo selecciona el mando."), centerX, y + 28, 0xFFB8B8B8);
        } else if (isComplete()) {
            centered(graphics, Component.literal("Calibracion guardada para:"), centerX, y, 0xFF55FF55);
            centered(graphics, Component.literal(selectedController.displayName()), centerX, y + 14, 0xFFFFFFFF);
            int count = GamepadCalibrationRegistry.get().calibratedButtonCount(selectedController);
            centered(graphics, Component.literal(count + " entradas calibradas."), centerX, y + 28, 0xFFB8B8B8);
        } else {
            GamepadDigitalInput target = STEPS[stepIndex];
            centered(graphics, Component.literal("Presiona el boton " + label(target)), centerX, y, 0xFFFFDD55);
            renderButtonIcon(graphics, target, y + 22);
            centered(graphics, Component.literal("Mando: " + selectedController.displayName()), centerX, y + 58, 0xFFB8B8B8);
            centered(graphics, Component.literal("Solo se capturan entradas de ese mando."), centerX, y + 72, 0xFFB8B8B8);
        }

        if (lastCapturedText != null) {
            centered(graphics, Component.literal(lastCapturedText), centerX, panel.y + panel.height - 58, 0xFFFFFFFF);
        }
        centered(graphics, Component.literal(InputApi.gamepadDebugStatus()), centerX, panel.y + panel.height - 44, 0xFF9FD6FF);
        centered(graphics, Component.literal("Si dice devices=0, SDL no esta viendo el mando."), centerX, panel.y + panel.height - 32, 0xFFB8B8B8);
        renderWidgets(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isEscape()) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(lastScreen);
    }

    @Override
    public boolean javareforged$isCapturingControllerBinding() {
        return true;
    }

    private void skipCurrentStep() {
        if (selectedController == null || isComplete()) {
            return;
        }
        lastCapturedText = "Saltado: " + label(STEPS[stepIndex]);
        stepIndex++;
        waitingForRelease = false;
        releaseStableSinceMs = -1L;
        cooldown();
    }

    private void waitForRelease() {
        waitingForRelease = true;
        releaseStableSinceMs = -1L;
    }

    private void cooldown() {
        InputApi.clearCalibrationPresses();
        ignoreInputUntilMs = Util.getMillis() + CAPTURE_COOLDOWN_MS;
    }

    private void tickRescan() {
        long now = Util.getMillis();
        if (now < nextRescanAtMs) {
            return;
        }
        nextRescanAtMs = now + RESCAN_INTERVAL_MS;
        InputApi.updateGamepads();
    }

    private boolean isComplete() {
        return selectedController != null && stepIndex >= STEPS.length;
    }

    private PanelBounds panelBounds() {
        int panelWidth = Math.min(520, Math.max(300, width - 32));
        int panelHeight = Math.min(190, Math.max(166, height - 48));
        int panelX = (width - panelWidth) / 2;
        int panelY = Math.max(16, (height - panelHeight) / 2 - 12);
        return new PanelBounds(panelX, panelY, panelWidth, panelHeight);
    }

    private void renderButtonIcon(GuiGraphicsExtractor graphics, GamepadDigitalInput input, int y) {
        ControllerGlyph glyph = ControllerGlyph.forInput(input);
        if (glyph == null) {
            return;
        }
        ControllerGlyphTheme theme = GamepadGlyphThemeState.currentTheme();
        if (!theme.supports(glyph) || theme.texture(glyph) == null) {
            return;
        }
        GlyphTextureBounds metrics = GlyphTextureBoundsCache.get(theme.texture(glyph));
        float iconHeight = theme.hintHeight(glyph);
        float iconWidth = metrics.drawWidthForHeight(iconHeight);
        float x = (width - iconWidth) / 2.0F;
        Draw.image(theme.texture(glyph), x, y, iconWidth, iconHeight)
            .uvSize(metrics.sourceWidth(), metrics.sourceHeight())
            .atlasSize(metrics.sourceWidth(), metrics.sourceHeight())
            .draw(graphics);
    }

    private void renderWidgets(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        for (Renderable renderable : renderables) {
            renderable.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }
    }

    private void centered(GuiGraphicsExtractor graphics, Component text, int centerX, int y, int color) {
        graphics.text(font, text, centerX - font.width(text) / 2, y, color, false);
    }

    private static String label(GamepadDigitalInput input) {
        return switch (input) {
            case BUTTON_DOWN -> "Abajo / A / Cross";
            case BUTTON_RIGHT -> "Derecha / B / Circle";
            case BUTTON_LEFT -> "Izquierda / X / Square";
            case BUTTON_UP -> "Arriba / Y / Triangle";
            case BUTTON_START -> "Start / Options";
            case BUTTON_SELECT -> "Select / Back";
            case BUTTON_GUIDE -> "Guide / Home";
            case BUTTON_SHARE -> "Share / Capture";
            case MISC_1 -> "Misc 1";
            case MISC_2 -> "Misc 2";
            case MISC_3 -> "Misc 3";
            case MISC_4 -> "Misc 4";
            case MISC_5 -> "Misc 5";
            case MISC_6 -> "Misc 6";
            case TOUCHPAD_BUTTON -> "Boton de touchpad";
            case TOUCHPAD_LEFT_BUTTON -> "Boton izquierdo de touchpad";
            case TOUCHPAD_RIGHT_BUTTON -> "Boton derecho de touchpad";
            case STICK_LEFT_BUTTON -> "LS / L3";
            case STICK_RIGHT_BUTTON -> "RS / R3";
            case BUMPER_LEFT -> "LB / L1";
            case BUMPER_RIGHT -> "RB / R1";
            case PADDLE_RIGHT_1 -> "Paddle derecha 1";
            case PADDLE_LEFT_1 -> "Paddle izquierda 1";
            case PADDLE_RIGHT_2 -> "Paddle derecha 2";
            case PADDLE_LEFT_2 -> "Paddle izquierda 2";
            case TRIGGER_LEFT -> "LT / L2";
            case TRIGGER_RIGHT -> "RT / R2";
            case DPAD_UP -> "D-Pad arriba";
            case DPAD_DOWN -> "D-Pad abajo";
            case DPAD_LEFT -> "D-Pad izquierda";
            case DPAD_RIGHT -> "D-Pad derecha";
            case STICK_LEFT_MOVE_UP -> "Stick izquierdo arriba";
            case STICK_LEFT_MOVE_DOWN -> "Stick izquierdo abajo";
            case STICK_LEFT_MOVE_LEFT -> "Stick izquierdo izquierda";
            case STICK_LEFT_MOVE_RIGHT -> "Stick izquierdo derecha";
            case STICK_RIGHT_MOVE_UP -> "Stick derecho arriba";
            case STICK_RIGHT_MOVE_DOWN -> "Stick derecho abajo";
            case STICK_RIGHT_MOVE_LEFT -> "Stick derecho izquierda";
            case STICK_RIGHT_MOVE_RIGHT -> "Stick derecho derecha";
        };
    }

    private record PanelBounds(int x, int y, int width, int height) {
        private int centerX() {
            return x + width / 2;
        }
    }
}
