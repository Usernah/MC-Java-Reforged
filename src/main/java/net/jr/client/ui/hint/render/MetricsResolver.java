package net.jr.client.ui.hint.render;

public record MetricsResolver(
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
    public static final MetricsResolver DEFAULT = new MetricsResolver(
            23F,
            28F,
            10,
            4F,
            8F,
            8F,
            4F,
            0,
            2,
            3.5F,
            3.5F
    );

    public static final MetricsResolver HD = new MetricsResolver(
            23F,
            28F,
            2,
            4F,
            6F,
            2F,
            2F,
            0,
            1,
            1.5F,
            1.5F
    );

}
