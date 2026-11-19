package net.jr.client.runtime.render.pass;

import javax.annotation.Nullable;
import net.jr.api.client.render.gui.GlobalRenderContext;
import net.jr.api.client.render.Draw;
import net.jr.api.client.resource.Asset;
import net.jr.api.client.split.JoinPromptAnnouncement;
import net.jr.api.client.split.JoinPromptAnnouncements;
import net.jr.api.client.input.IGamepad;
import net.jr.client.components.elements.TextElement;
import net.jr.client.runtime.viewport.GuiScaleCalculator;
import net.jr.client.ui.hint.glyph.ControllerGlyphTheme;
import net.jr.client.ui.hint.glyph.GamepadGlyphThemeState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;


public final class JoinPromptRenderPass {
    private static final int PANEL_WIDTH = 186;
    private static final int PANEL_HEIGHT = 37;
    private static final int CONTROLLER_X = 6;
    private static final int CONTROLLER_Y = 4;
    private static final int CONTROLLER_WIDTH = 26;
    private static final int CONTROLLER_HEIGHT = 29;
    private static final int TEXT_AREA_LEFT = 40;
    private static final int TEXT_AREA_RIGHT = 6;
    private static final float LABEL_SCALE = 1.0F;
    private static final int TEXT_COLOR = 0xFF4D4D4D;
    private static final long BLINK_PERIOD_MS = 1_000L;

    private static final Asset PANEL = Asset.MOD("textures/gui/prompts/panel_prompt.png");
    private static final Asset JOYSTICKS = Asset.MOD("textures/gui/prompts/joysticks_icon.png");
    private static final Asset JOYSTICKS_P2 = Asset.MOD("textures/gui/prompts/joysticks_p2_icon.png");
    private static final Asset JOYSTICKS_P3 = Asset.MOD("textures/gui/prompts/joysticks_p3_icon.png");
    private static final Asset JOYSTICKS_P4 = Asset.MOD("textures/gui/prompts/joysticks_p4_icon.png");

    @Nullable
    private static JoinPromptAnnouncement.Show current;
    private static long shownAtMs;

    static {
        JoinPromptAnnouncements.source().subscribe(JoinPromptRenderPass::onAnnouncement);
    }

    private JoinPromptRenderPass() {
    }

    public static void render(GlobalRenderContext context) {
        JoinPromptAnnouncement.Show prompt = current;
        if (prompt == null) {
            return;
        }

        GuiGraphicsExtractor graphics = context.graphics();
        Minecraft minecraft = Minecraft.getInstance();
        double activeScale = minecraft.getWindow().getGuiScale();
        int standardScale = GuiScaleCalculator.resolveStandard(
            minecraft.getWindow().getWidth(),
            minecraft.getWindow().getHeight(),
            minecraft.options.guiScale().get(),
            minecraft.isEnforceUnicode()
        );
        float presentationScale = (float)(standardScale / activeScale);
        if (!Float.isFinite(presentationScale) || presentationScale <= 0.0F) {
            presentationScale = 1.0F;
        }

        float canvasWidth = context.guiWidth() / presentationScale;
        float canvasHeight = context.guiHeight() / presentationScale;
        float panelX = (canvasWidth - PANEL_WIDTH) / 2.0F;
        float panelY = (canvasHeight - PANEL_HEIGHT) / 2.0F;

        graphics.pose().pushMatrix();
        graphics.pose().scale(presentationScale, presentationScale);
        try {
            renderPrompt(graphics, prompt, panelX, panelY);
        } finally {
            graphics.pose().popMatrix();
        }
    }

    private static void renderPrompt(
        GuiGraphicsExtractor graphics,
        JoinPromptAnnouncement.Show prompt,
        float panelX,
        float panelY
    ) {
        Draw.image(PANEL, panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT).draw(graphics);
        Draw.image(
            JOYSTICKS,
            panelX + CONTROLLER_X,
            panelY + CONTROLLER_Y,
            CONTROLLER_WIDTH,
            CONTROLLER_HEIGHT
        ).draw(graphics);
        Draw.image(
            playerIcon(prompt.playerNumber()),
            panelX + CONTROLLER_X,
            panelY + CONTROLLER_Y,
            CONTROLLER_WIDTH,
            CONTROLLER_HEIGHT
        ).alpha(blinkAlpha()).draw(graphics);

        renderMessage(graphics, panelX, panelY, prompt.gamepad());
    }

    private static void renderMessage(
        GuiGraphicsExtractor graphics,
        float panelX,
        float panelY,
        IGamepad gamepad
    ) {
        ControllerGlyphTheme theme = GamepadGlyphThemeState.themeForGamepad(gamepad);
        float areaX = panelX + TEXT_AREA_LEFT;
        int areaWidth = PANEL_WIDTH - TEXT_AREA_LEFT - TEXT_AREA_RIGHT;
        GamepadGlyphThemeState.withRenderTheme(theme, () -> {
            TextElement message = new TextElement(
                Component.translatable("prompt.java_reforged.join"),
                areaX + areaWidth / 2.0F,
                0.0F,
                graphics
            );
            message.setScale(LABEL_SCALE);
            message.setColor(TEXT_COLOR);
            message.setAlign(Draw.TextAlign.CENTER);
            message.setY(panelY + (PANEL_HEIGHT - message.height()) / 2.0F);
            message.draw();
        });
    }

    private static Asset playerIcon(int playerNumber) {
        return switch (playerNumber) {
            case 2 -> JOYSTICKS_P2;
            case 3 -> JOYSTICKS_P3;
            case 4 -> JOYSTICKS_P4;
            default -> throw new IllegalArgumentException("Unsupported join prompt player " + playerNumber);
        };
    }

    private static float blinkAlpha() {
        double phase = (Util.getMillis() - shownAtMs) % BLINK_PERIOD_MS
            / (double)BLINK_PERIOD_MS;
        return (float)(0.5D + 0.5D * Math.cos(phase * Math.PI * 2.0D));
    }

    private static void onAnnouncement(JoinPromptAnnouncement announcement) {
        if (announcement instanceof JoinPromptAnnouncement.Show show) {
            current = show;
            shownAtMs = Util.getMillis();
        } else {
            current = null;
            shownAtMs = 0L;
        }
    }
}
