package net.jr.client.ui.hint.render;

import net.jr.api.client.render.Draw;
import net.jr.api.client.ui.UiDataValues;
import net.jr.client.ui.hint.ControlHintContext;
import net.jr.client.ui.hint.model.ResolvedControlHint;
import net.jr.client.ui.presentation.UiPresentation;
import net.jr.registry.ModUi;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class ControlHintRenderer {
    static UiDataValues c_data = ModUi.CONTROL_TOOLTIPS_DAT.readData();
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int TEXT_SHADOW = 0xFF101010;

    private ControlHintRenderer() {
    }

    private static ControlHintMetrics metrics() {
        return ControlHintMetrics.current();
    }

    public static int reservedScreenBottomHeight() {
        ControlHintMetrics metrics = metrics();
        return (int)Math.ceil(metrics.barHeight() + metrics.bottomPadding());
    }


    public static void renderDirect(
            ControlHintContext context,
            GuiGraphicsExtractor graphics,
            List<ResolvedControlHint> hints,
            float canvasWidth,
            float canvasHeight
    ) {
        ControlHintMetrics metrics = metrics();

        int textHeight = Draw.text(Component.empty())
                .scale(metrics.textScale())
                .font(UiPresentation.currentFont())
                .height();
        int textY = Math.round(
                canvasHeight
                        - metrics.bottomPadding()
                        - textHeight
        );

        float cursorX = metrics.leftPadding();

        for (ResolvedControlHint hint : hints) {
            Draw.TextBuilder text = Draw.text(hint.content())
                    .scale(metrics.textScale())
                    .shadowColor(c_data.color("t_shadow_color"))
                    .legacyShadow(c_data.bool("t_shadow"))
                    .color(c_data.color("t_color"))
                    .font(c_data.asset("t_font"));

            text.position(
                    Math.round(cursorX),
                    textY
            ).draw(graphics);

            cursorX += text.width()
                    + metrics.interHintPadding();
        }
    }


    public static int compositionHeight(
        ControlHintContext context,
        List<ResolvedControlHint> hints
    ) {
        ControlHintMetrics metrics = metrics();
        return (int)Math.ceil(metrics.bottomPadding() + metrics.barHeight());
    }
}
