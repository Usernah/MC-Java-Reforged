package net.jr.client.ui.hint.render;

public record ControlHintMetrics(
        float hintBoxSize,
        float barHeight,
        float bottomPadding,
        float leftPadding,
        float interHintPadding,
        float verticalHintPadding,
        float textOffsetFromIcon,
        int textBgPaddingX,
        int textBgPaddingY,
        float menuTextScale,
        float hudTextScale
) {
    public static final ControlHintMetrics DEFAULT = new ControlHintMetrics(
            23F,
            40F,
            18F,
            14F,
            18F,
            18F,
            10F,
            0,
            2,
            3.0F,
            3.0F
    );

    public static final ControlHintMetrics HD = new ControlHintMetrics(
            23F,
            20F,
            10F,
            8F,
            6F,
            2F,
            2F,
            0,
            1,
            1.0F,
            1.0F
    );
}
