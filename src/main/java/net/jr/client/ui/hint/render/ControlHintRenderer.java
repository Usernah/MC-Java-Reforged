package net.jr.client.ui.hint.render;

import java.util.ArrayList;
import java.util.List;
import net.jr.api.client.render.Draw;
import net.jr.api.client.local.LocalClients;
import net.jr.api.client.split.SplitScreen;
import net.jr.client.ui.hud.HudTransparency;
import net.jr.client.ui.hint.ControlHintContext;
import net.jr.client.ui.hint.model.HintPlacement;
import net.jr.client.ui.hint.model.ResolvedControlHint;
import net.jr.client.ui.hint.model.ResolvedControlHintIcon;
import net.jr.client.ui.hint.model.ResolvedControlHintInput;
import net.jr.registry.ModFonts;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class ControlHintRenderer {
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int TEXT_SHADOW = 0xFF282828;

    private ControlHintRenderer() {
    }

    private static ControlHintMetrics metrics() {
        return SplitScreen.isActive()
                ? ControlHintMetrics.HD
                : ControlHintMetrics.DEFAULT;
    }

    public static int reservedScreenBottomHeight() {
        ControlHintMetrics metrics = metrics();

        return (int)Math.ceil(
                metrics.barHeight()
                        + metrics.bottomPadding()
        );
    }

    public static void renderDirect(
            ControlHintContext context,
            GuiGraphicsExtractor graphics,
            List<ResolvedControlHint> hints,
            float canvasWidth,
            float canvasHeight
    ) {
        renderLayer(
                context,
                graphics,
                hints,
                canvasWidth,
                canvasHeight,
                true,
                true
        );
    }

    public static void renderIcons(
            ControlHintContext context,
            GuiGraphicsExtractor graphics,
            List<ResolvedControlHint> hints,
            float canvasWidth,
            float canvasHeight
    ) {
        renderLayer(
                context,
                graphics,
                hints,
                canvasWidth,
                canvasHeight,
                true,
                false
        );
    }

    public static void renderText(
            ControlHintContext context,
            GuiGraphicsExtractor graphics,
            List<ResolvedControlHint> hints,
            float canvasWidth,
            float canvasHeight
    ) {
        renderLayer(
                context,
                graphics,
                hints,
                canvasWidth,
                canvasHeight,
                false,
                true
        );
    }

    private static void renderLayer(
            ControlHintContext context,
            GuiGraphicsExtractor graphics,
            List<ResolvedControlHint> hints,
            float canvasWidth,
            float canvasHeight,
            boolean drawIcons,
            boolean drawText
    ) {
        ControlHintMetrics metrics = metrics();

        renderMenu(
                graphics,
                hints,
                canvasHeight,
                drawIcons,
                drawText,
                metrics
        );
    }

    private static void renderMenu(
            GuiGraphicsExtractor graphics,
            List<ResolvedControlHint> hints,
            float canvasHeight,
            boolean renderIcons,
            boolean renderText,
            ControlHintMetrics metrics
    ) {
        float rowHeight = rowHeight(hints, metrics);

        float anchorY =
                canvasHeight
                        - metrics.bottomPadding()
                        - Math.max(
                        metrics.barHeight(),
                        rowHeight
                ) / 2F;

        float previousTextEnd = 0F;
        boolean firstHint = true;

        for (ResolvedControlHint hint : hints) {
            float iconX = firstHint
                    ? metrics.leftPadding()
                    : previousTextEnd
                    + metrics.interHintPadding()
                    - hint.visibleLeft();

            drawIcons(
                    graphics,
                    hint,
                    iconX,
                    anchorY,
                    1.0F,
                    renderIcons,
                    renderText
            );

            float textX =
                    iconX
                            + visibleRight(hint)
                            + metrics.textOffsetFromIcon();

            Draw.TextBuilder labelText = Draw.text(hint.label())
                    .scale(metrics.menuTextScale())
                    .shadowColor(TEXT_SHADOW)
                    .color(TEXT_COLOR);

            if (
                    SplitScreen.isActive()
                            && LocalClients.count() > 1
            ) {
                labelText.font(ModFonts.MINECRAFT_MIN);
            }

            int textY = Math.round(
                    anchorY - labelText.lineHeight() / 2.0F
            );

            int textBlockX = Math.round(textX);

            if (renderText) {
                labelText
                        .position(
                                textBlockX + metrics.textBgPaddingX(),
                                textY + metrics.textBgPaddingY()
                        )
                        .draw(graphics);
            }

            previousTextEnd =
                    textBlockX
                            + labelText.width()
                            + metrics.textBgPaddingX() * 2;

            firstHint = false;
        }
    }

    private static void renderHud(
            GuiGraphicsExtractor graphics,
            List<ResolvedControlHint> hints,
            float canvasWidth,
            float canvasHeight,
            boolean renderIcons,
            boolean renderText,
            ControlHintMetrics metrics
    ) {
        List<ResolvedControlHint> left = new ArrayList<>();
        List<ResolvedControlHint> right = new ArrayList<>();

        for (ResolvedControlHint hint : hints) {
            (
                    hint.placement() == HintPlacement.LEFT
                            ? left
                            : right
            ).add(hint);
        }

        float alpha = HudTransparency.elementAlpha();

        float bottom =
                canvasHeight - metrics.bottomPadding();

        for (ResolvedControlHint hint : left) {
            float height =
                    rowHeight(hint, metrics);

            float y =
                    bottom - height / 2F;

            float iconX =
                    metrics.leftPadding();

            drawIcons(
                    graphics,
                    hint,
                    iconX,
                    y,
                    alpha,
                    renderIcons,
                    renderText
            );

            float textX =
                    iconX
                            + visibleRight(hint)
                            + metrics.textOffsetFromIcon();

            if (renderText) {
                drawText(
                        graphics,
                        hint.label(),
                        textX,
                        y,
                        false,
                        alpha,
                        metrics
                );
            }

            bottom -=
                    height
                            + metrics.verticalHintPadding();
        }

        bottom =
                canvasHeight - metrics.bottomPadding();

        for (ResolvedControlHint hint : right) {
            float height =
                    rowHeight(hint, metrics);

            float y =
                    bottom - height / 2F;

            float iconX =
                    canvasWidth
                            - metrics.leftPadding()
                            - iconWidth(hint);

            float textRight =
                    iconX
                            + hint.visibleLeft()
                            - metrics.textOffsetFromIcon();

            if (renderText) {
                drawText(
                        graphics,
                        hint.label(),
                        textRight,
                        y,
                        true,
                        alpha,
                        metrics
                );
            }

            drawIcons(
                    graphics,
                    hint,
                    iconX,
                    y,
                    alpha,
                    renderIcons,
                    renderText
            );

            bottom -=
                    height
                            + metrics.verticalHintPadding();
        }
    }

    private static void drawText(
            GuiGraphicsExtractor graphics,
            Component label,
            float anchorX,
            float centerY,
            boolean rightAligned,
            float alpha,
            ControlHintMetrics metrics
    ) {
        Draw.TextBuilder labelText = Draw.text(label)
                .scale(metrics.hudTextScale())
                .shadowColor(
                        HudTransparency.applyAlpha(
                                TEXT_SHADOW,
                                alpha
                        )
                )
                .color(
                        HudTransparency.applyAlpha(
                                TEXT_COLOR,
                                alpha
                        )
                );

        if (
                SplitScreen.isActive()
                        && LocalClients.count() > 1
        ) {
            labelText.font(ModFonts.MINECRAFT_MIN);
        }

        int textBlockWidth =
                labelText.width()
                        + metrics.textBgPaddingX() * 2;

        int textX = rightAligned
                ? Math.round(anchorX - textBlockWidth)
                : Math.round(anchorX);

        int textY = Math.round(
                centerY - labelText.lineHeight() / 2.0F
        );

        labelText
                .position(
                        textX + metrics.textBgPaddingX(),
                        textY + metrics.textBgPaddingY()
                )
                .draw(graphics);
    }

    private static void drawIcons(
            GuiGraphicsExtractor graphics,
            ResolvedControlHint hint,
            float x,
            float centerY,
            float alpha,
            boolean renderIcons,
            boolean renderText
    ) {
        float current = x;

        for (int inputIndex = 0; inputIndex < hint.inputs().size(); inputIndex++) {
            if (inputIndex > 0) {
                current += ResolvedControlHint.inputGap();
            }

            ResolvedControlHintInput input = hint.inputs().get(inputIndex);
            for (int iconIndex = 0; iconIndex < input.icons().size(); iconIndex++) {
                if (iconIndex > 0) {
                    current += ResolvedControlHint.joinerGap();

                    Draw.TextBuilder joinerText = joinerText(alpha);
                    joinerText.position(
                            Math.round(current),
                            Math.round(centerY - joinerText.lineHeight() / 2F)
                    );

                    if (renderText) {
                        joinerText.draw(graphics);
                    }

                    current += joinerText.width() + ResolvedControlHint.joinerGap();
                }

                ResolvedControlHintIcon icon = input.icons().get(iconIndex);
                GlyphTextureBounds metrics = icon.metrics();

                if (renderIcons) {
                    Draw.image(
                                    icon.texture(),
                                    current,
                                    centerY - icon.drawHeight() / 2F,
                                    icon.drawWidth(),
                                    icon.drawHeight()
                            )
                            .uvSize(metrics.sourceWidth(), metrics.sourceHeight())
                            .atlasSize(metrics.sourceWidth(), metrics.sourceHeight())
                            .alpha(alpha)
                            .draw(graphics);
                }

                current += icon.drawWidth();
            }
        }
    }

    private static float visibleRight(
            ResolvedControlHint hint
    ) {
        return hint.visibleRight(
                joinerText(1.0F).width(),
                ResolvedControlHint.joinerGap()
        );
    }

    private static float iconWidth(
            ResolvedControlHint hint
    ) {
        return hint.iconWidth(
                joinerText(1.0F).width(),
                ResolvedControlHint.joinerGap()
        );
    }

    private static Draw.TextBuilder joinerText(
            float alpha
    ) {
        return Draw.text(
                        Component.literal(
                                ResolvedControlHint.joinerText()
                        )
                )
                .shadowColor(
                        HudTransparency.applyAlpha(
                                TEXT_SHADOW,
                                alpha
                        )
                )
                .color(
                        HudTransparency.applyAlpha(
                                TEXT_COLOR,
                                alpha
                        )
                );
    }

    private static float rowHeight(
            List<ResolvedControlHint> hints,
            ControlHintMetrics metrics
    ) {
        float height =
                metrics.hintBoxSize();

        for (ResolvedControlHint hint : hints) {
            height = Math.max(
                    height,
                    rowHeight(hint, metrics)
            );
        }

        return height;
    }

    private static float rowHeight(
            ResolvedControlHint hint,
            ControlHintMetrics metrics
    ) {
        return Math.max(
                metrics.hintBoxSize(),
                hint.iconHeight()
        );
    }

    public static int compositionHeight(
            ControlHintContext context,
            List<ResolvedControlHint> hints
    ) {
        ControlHintMetrics metrics = metrics();

        if (!context.isHud()) {
            return (int)Math.ceil(
                    metrics.bottomPadding()
                            + Math.max(
                            metrics.barHeight(),
                            rowHeight(hints, metrics)
                    )
            );
        }

        float leftHeight =
                columnHeight(
                        hints,
                        HintPlacement.LEFT,
                        metrics
                );

        float rightHeight =
                columnHeight(
                        hints,
                        HintPlacement.RIGHT,
                        metrics
                );

        return (int)Math.ceil(
                metrics.bottomPadding()
                        + Math.max(
                        metrics.barHeight(),
                        Math.max(
                                leftHeight,
                                rightHeight
                        )
                )
        );
    }

    private static float columnHeight(
            List<ResolvedControlHint> hints,
            HintPlacement placement,
            ControlHintMetrics metrics
    ) {
        float height = 0F;
        boolean found = false;

        for (ResolvedControlHint hint : hints) {
            if (hint.placement() != placement) {
                continue;
            }

            if (found) {
                height +=
                        metrics.verticalHintPadding();
            }

            height +=
                    rowHeight(hint, metrics);

            found = true;
        }

        return height;
    }
}
