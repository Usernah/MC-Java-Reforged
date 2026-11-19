package net.jr.client.ui.hint.render;

import net.jr.client.ui.presentation.UiPresentation;

public record ControlHintMetrics(
        float barHeight,
        float bottomPadding,
        float leftPadding,
        float interInputSpacing,
        float interHintPadding,
        float textScale
) {
    public static final ControlHintMetrics DEFAULT = new ControlHintMetrics(
            40F,
            30F,
            14F,
            9F,
            26F,
            3.0F
    );

    public static final ControlHintMetrics SMALL = new ControlHintMetrics(
            14F,
            16F,
            8F,
            3F,
            10F,
            1.0F
    );

    public static ControlHintMetrics current() {
        return UiPresentation.isSmall() ? SMALL : DEFAULT;
    }
}
