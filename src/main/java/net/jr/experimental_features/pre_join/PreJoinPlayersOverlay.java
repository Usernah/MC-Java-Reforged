package net.jr.experimental_features.pre_join;

import com.mojang.blaze3d.textures.FilterMode;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.jr.api.client.render.Draw;
import net.jr.api.client.resource.Asset;
import net.jr.api.client.split.SplitScreen;
import net.jr.client.components.elements.TextElement;
import net.jr.client.input.InputApi;
import net.jr.client.input.gamepad.GamepadDigitalInput;
import net.jr.client.input.sdl.SdlGamepad;
import net.jr.client.runtime.input.InputDeviceRouter;
import net.jr.client.ui.hint.glyph.ControllerGlyph;
import net.jr.client.ui.hint.glyph.ControllerGlyphTheme;
import net.jr.client.ui.hint.glyph.GamepadGlyphThemeState;
import net.jr.client.ui.hint.render.GlyphTextureBoundsCache;
import net.jr.client.ui.overlay.ScreenOverlayCloseReason;
import net.jr.client.ui.overlay.ScreenOverlayContext;
import net.jr.client.ui.overlay.VanillaScreenOverlay;
import net.jr.client.ui.render.GuiCompositionLayer;
import net.jr.registry.ModFonts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

/** Selects the local players that will enter a world with the primary player. */
public final class PreJoinPlayersOverlay implements VanillaScreenOverlay {
    private static final int PANEL_WIDTH = 144;
    private static final int PANEL_HEIGHT = 110;
    private static final int HORIZONTAL_GAP = 16;
    private static final int VERTICAL_GAP = 4;
    private static final int COMPOSITION_WIDTH = PANEL_WIDTH * 2 + HORIZONTAL_GAP;
    private static final int COMPOSITION_HEIGHT = PANEL_HEIGHT * 2 + VERTICAL_GAP;
    private static final int NAME_X = 39;
    private static final int NAME_Y = 8;
    private static final int CONTROL_SIZE = 354;
    private static final int CONTROL_VERTICAL_OFFSET = 12;
    private static final int CONTROLLER_LAYER_WIDTH = 1920;
    private static final int CONTROLLER_LAYER_HEIGHT = 1080;
    private static final float CONTROLLER_LAYER_SCALE = 0.25F;
    private static final float CONTROLLER_LAYER_PIXEL_BIAS = 0.35F;
    private static final int DIM_COLOR = 0xb2515151;
    private static final Asset ACTIVE_PANEL = Asset.MOD("textures/gui/containers/join_panel_active.png");
    private static final Asset EMPTY_PANEL = Asset.MOD("textures/gui/containers/join_panel.png");

    private final Screen sourceScreen;
    private final Runnable openWorld;
    private final Long[] assignedDevices = new Long[4];
    private final Map<Long, ButtonState> buttons = new HashMap<>();
    private final TextElement[] playerNames = new TextElement[4];
    private final TextElement joinLabel = text(Component.translatable("menu.java_reforged.pre_join.join"));
    private final GuiCompositionLayer controllerLayer =
        new GuiCompositionLayer(
            CONTROLLER_LAYER_WIDTH,
            CONTROLLER_LAYER_HEIGHT,
            CONTROLLER_LAYER_SCALE
        );

    public PreJoinPlayersOverlay(Screen sourceScreen, Runnable openWorld) {
        this.sourceScreen = sourceScreen;
        this.openWorld = openWorld;
        String primaryName = Minecraft.getInstance().getUser().getName();
        this.playerNames[0] = text(Component.literal(primaryName));
        for (int slotId = 1; slotId < this.playerNames.length; slotId++) {
            this.playerNames[slotId] = text(Component.literal(primaryName + " (" + (slotId + 1) + ")"));
        }
        PreJoinRoster.restoreInto(this.assignedDevices, SdlGamepad.deviceIds());
    }

    public static boolean shouldOpen() {
        return SplitScreen.hasRequiredControllerCountForJoin();
    }

    @Override
    public void opened(ScreenOverlayContext context) {
        InputApi.releaseBindings();
        InputApi.suppressHeldBindings();
        this.synchronizeButtons();
    }

    @Override
    public void closed(ScreenOverlayCloseReason reason) {
        this.controllerLayer.close();
    }

    @Override
    public void tick(ScreenOverlayContext context) {
        List<Long> connected = SdlGamepad.deviceIds();
        this.assignedDevicesForDisconnectedControllers(connected);
        this.buttons.keySet().removeIf(deviceId -> !connected.contains(deviceId));

        for (long deviceId : connected) {
            ButtonState state = this.buttons.computeIfAbsent(deviceId, ignored -> new ButtonState());
            boolean confirm = edge(deviceId, GamepadDigitalInput.BUTTON_DOWN, state.confirmDown);
            boolean back = edge(deviceId, GamepadDigitalInput.BUTTON_RIGHT, state.backDown);
            boolean start = edge(deviceId, GamepadDigitalInput.BUTTON_START, state.startDown);
            state.confirmDown = pressed(deviceId, GamepadDigitalInput.BUTTON_DOWN);
            state.backDown = pressed(deviceId, GamepadDigitalInput.BUTTON_RIGHT);
            state.startDown = pressed(deviceId, GamepadDigitalInput.BUTTON_START);

            int assignedPanel = this.panelForDevice(deviceId);
            if (back) {
                if (assignedPanel > 0) {
                    this.assignedDevices[assignedPanel] = null;
                } else {
                    context.close(ScreenOverlayCloseReason.CANCELLED);
                }
                return;
            }
            if (start || confirm && this.allPanelsOccupied()) {
                this.confirm(context);
                return;
            }
            if (confirm && assignedPanel < 0) {
                int freePanel = this.firstFreeSecondaryPanel();
                if (freePanel >= 0 && this.assignedSecondaryCount() < connected.size()) {
                    this.assignedDevices[freePanel] = deviceId;
                }
                return;
            }
        }
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, int width, int height, float partialTick) {
        graphics.fill(0, 0, width, height, DIM_COLOR);
        int left = (width - COMPOSITION_WIDTH) / 2;
        int top = (height - COMPOSITION_HEIGHT) / 2;
        ControllerGlyphTheme rotatingTheme = this.rotatingFreeTheme();
        ControllerGlyphTheme placeholderTheme = rotatingTheme != null
            ? rotatingTheme
            : rotatingTheme(this.connectedThemes());
        int secondaryCapacity = Math.min(this.assignedDevices.length - 1, SdlGamepad.deviceIds().size());

        for (int panel = 0; panel < 4; panel++) {
            int x = left + (panel % 2) * (PANEL_WIDTH + HORIZONTAL_GAP);
            int y = top + (panel / 2) * (PANEL_HEIGHT + VERTICAL_GAP);
            boolean occupied = panel == 0 || this.assignedDevices[panel] != null;
            boolean available = panel > 0 && panel <= secondaryCapacity;
            Draw.image(occupied ? ACTIVE_PANEL : EMPTY_PANEL, x, y, PANEL_WIDTH, PANEL_HEIGHT)
                .pixelSnap()
                .draw(graphics);
            if (occupied) {
                TextElement name = this.playerNames[panel];
                name.setPosition(x + NAME_X, y + NAME_Y);
                name.draw(graphics);
            } else if (available) {
                this.renderJoinPrompt(graphics, x, y, rotatingTheme);
            }
        }

        this.renderControllerLayer(
            graphics,
            left,
            top,
            rotatingTheme,
            placeholderTheme,
            secondaryCapacity
        );
    }

    private void renderJoinPrompt(
        GuiGraphicsExtractor graphics,
        int panelX,
        int panelY,
        ControllerGlyphTheme theme
    ) {
        if (theme == null) {
            return;
        }
        ControllerGlyph glyph = ControllerGlyph.BUTTON_DOWN;
        Asset icon = theme.texture(glyph);
        if (icon != null) {
            float iconHeight = theme.hintHeight(glyph);
            float iconWidth = GlyphTextureBoundsCache.get(icon).drawWidthForHeight(iconHeight);
            Draw.image(
                icon,
                panelX + (PANEL_WIDTH - iconWidth) / 2.0F,
                panelY + 26,
                iconWidth,
                iconHeight
            ).draw(graphics);
        }
        this.joinLabel.setPosition(
            panelX + (PANEL_WIDTH - this.joinLabel.width()) / 2.0F,
            panelY + 80
        );
        this.joinLabel.setScale(1.0F);
        this.joinLabel.draw(graphics);
    }

    private void renderControllerLayer(
        GuiGraphicsExtractor graphics,
        int left,
        int top,
        ControllerGlyphTheme rotatingTheme,
        ControllerGlyphTheme placeholderTheme,
        int secondaryCapacity
    ) {
        this.controllerLayer.submit(
            graphics,
            FilterMode.LINEAR,
            CONTROLLER_LAYER_PIXEL_BIAS,
            canvas -> {
                for (int panel = 0; panel < this.assignedDevices.length; panel++) {
                    int panelX = left + (panel % 2) * (PANEL_WIDTH + HORIZONTAL_GAP);
                    int panelY = top + (panel / 2) * (PANEL_HEIGHT + VERTICAL_GAP);
                    boolean occupied = panel == 0 || this.assignedDevices[panel] != null;
                    boolean available = panel > 0 && panel <= secondaryCapacity;
                    if (occupied) {
                        ControllerGlyphTheme theme = panel == 0
                            ? rotatingTheme
                            : this.themeForDevice(this.assignedDevices[panel]);
                        this.renderController(canvas, panelX, panelY, theme, 1.0F);
                    } else if (!available) {
                        this.renderController(canvas, panelX, panelY, placeholderTheme, 0.5F);
                    }
                }
            }
        );
    }

    private void renderController(
        GuiCompositionLayer.Canvas canvas,
        int panelX,
        int panelY,
        ControllerGlyphTheme theme,
        float alpha
    ) {
        if (theme == null) {
            return;
        }
        float centerX = panelX + PANEL_WIDTH / 2.0F;
        float centerY = panelY + PANEL_HEIGHT / 2.0F + CONTROL_VERTICAL_OFFSET;
        centerX = canvas.toCanvasX(centerX);
        centerY = canvas.toCanvasY(centerY);
        Draw.image(
            theme.controlTexture(),
            centerX - CONTROL_SIZE / 2.0F,
            centerY - CONTROL_SIZE / 2.0F,
            CONTROL_SIZE,
            CONTROL_SIZE
        ).alpha(alpha).draw(canvas.graphics());
    }

    private ControllerGlyphTheme rotatingFreeTheme() {
        return rotatingTheme(this.availableThemes());
    }

    private static ControllerGlyphTheme rotatingTheme(List<ControllerGlyphTheme> themes) {
        if (themes.isEmpty()) {
            return null;
        }
        return themes.get((int)((Util.getMillis() / 1000L) % themes.size()));
    }

    private ControllerGlyphTheme themeForDevice(Long deviceId) {
        return deviceId == null
            ? null
            : GamepadGlyphThemeState.themeForIdentity(SdlGamepad.identity(deviceId));
    }

    private List<ControllerGlyphTheme> availableThemes() {
        EnumSet<ControllerGlyphTheme> unique = EnumSet.noneOf(ControllerGlyphTheme.class);
        for (long deviceId : SdlGamepad.deviceIds()) {
            if (this.panelForDevice(deviceId) < 0) {
                unique.add(GamepadGlyphThemeState.themeForIdentity(SdlGamepad.identity(deviceId)));
            }
        }
        return new ArrayList<>(unique);
    }

    private List<ControllerGlyphTheme> connectedThemes() {
        EnumSet<ControllerGlyphTheme> unique = EnumSet.noneOf(ControllerGlyphTheme.class);
        for (long deviceId : SdlGamepad.deviceIds()) {
            unique.add(GamepadGlyphThemeState.themeForIdentity(SdlGamepad.identity(deviceId)));
        }
        return new ArrayList<>(unique);
    }

    private void confirm(ScreenOverlayContext context) {
        Map<Integer, Long> players = new LinkedHashMap<>();
        for (int slotId = 1; slotId < this.assignedDevices.length; slotId++) {
            Long deviceId = this.assignedDevices[slotId];
            if (deviceId != null) {
                players.put(slotId, deviceId);
            }
        }
        PreJoinRoster.remember(players);
        context.close(ScreenOverlayCloseReason.COMPLETED);
        PreJoinWorldLoad.begin(this.sourceScreen, players, this.openWorld);
    }

    private void synchronizeButtons() {
        for (long deviceId : SdlGamepad.deviceIds()) {
            ButtonState state = this.buttons.computeIfAbsent(deviceId, ignored -> new ButtonState());
            state.confirmDown = pressed(deviceId, GamepadDigitalInput.BUTTON_DOWN);
            state.backDown = pressed(deviceId, GamepadDigitalInput.BUTTON_RIGHT);
            state.startDown = pressed(deviceId, GamepadDigitalInput.BUTTON_START);
        }
    }

    private void assignedDevicesForDisconnectedControllers(List<Long> connected) {
        for (int panel = 1; panel < this.assignedDevices.length; panel++) {
            Long deviceId = this.assignedDevices[panel];
            if (deviceId != null && !connected.contains(deviceId)) {
                this.assignedDevices[panel] = null;
            }
        }
    }

    private int panelForDevice(long deviceId) {
        for (int panel = 1; panel < this.assignedDevices.length; panel++) {
            if (this.assignedDevices[panel] != null && this.assignedDevices[panel] == deviceId) {
                return panel;
            }
        }
        return -1;
    }

    private int firstFreeSecondaryPanel() {
        for (int panel = 1; panel < this.assignedDevices.length; panel++) {
            if (this.assignedDevices[panel] == null) {
                return panel;
            }
        }
        return -1;
    }

    private boolean allPanelsOccupied() {
        return this.assignedSecondaryCount() == 3;
    }

    private int assignedSecondaryCount() {
        int count = 0;
        for (int panel = 1; panel < this.assignedDevices.length; panel++) {
            if (this.assignedDevices[panel] != null) {
                count++;
            }
        }
        return count;
    }

    private static boolean edge(long deviceId, GamepadDigitalInput input, boolean wasDown) {
        return pressed(deviceId, input) && !wasDown;
    }

    private static boolean pressed(long deviceId, GamepadDigitalInput input) {
        return SdlGamepad.input(deviceId, input, InputApi.DIGITAL_PRESS_THRESHOLD);
    }

    private static TextElement text(Component component) {
        TextElement text = new TextElement(component, 0.0F, 0.0F, null);
        text.setColor(0xFF4D4D4D);
        text.setShadow(false);
        text.setScale(0.8F);
        return text;
    }

    private static final class ButtonState {
        private boolean confirmDown;
        private boolean backDown;
        private boolean startDown;
    }
}
