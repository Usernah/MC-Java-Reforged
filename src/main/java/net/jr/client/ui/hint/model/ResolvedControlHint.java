package net.jr.client.ui.hint.model;

import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Objects;

public record ResolvedControlHint(
    HintPlacement placement,
    Component label,
    List<ResolvedControlHintInput> inputs
) {
    private static final float JOINER_GAP = 2.0F;
    private static final float INPUT_GAP = 4.0F;
    private static final String JOINER_TEXT = "+";

    public ResolvedControlHint {
        Objects.requireNonNull(placement, "placement");
        Objects.requireNonNull(label, "label");
        inputs = List.copyOf(Objects.requireNonNull(inputs, "inputs"));
        if (inputs.isEmpty()) {
            throw new IllegalArgumentException("inputs must not be empty");
        }
    }

    public float iconWidth(float joinerWidth, float joinerGap) {
        float width = 0.0F;
        for (ResolvedControlHintInput input : this.inputs) {
            if (width > 0.0F) {
                width += INPUT_GAP;
            }
            width += input.width(joinerWidth, joinerGap);
        }
        return width;
    }

    public float iconHeight() {
        float height = 0.0F;
        for (ResolvedControlHintInput input : this.inputs) {
            height = Math.max(height, input.height());
        }
        return height;
    }

    public float visibleLeft() {
        return this.inputs.getFirst().visibleLeft();
    }

    public float visibleRight(float joinerWidth, float joinerGap) {
        float x = 0.0F;
        for (int i = 0; i < this.inputs.size(); i++) {
            ResolvedControlHintInput input = this.inputs.get(i);
            if (i == this.inputs.size() - 1) {
                return x + input.visibleRight(joinerWidth, joinerGap);
            }
            x += input.width(joinerWidth, joinerGap) + INPUT_GAP;
        }
        return 0.0F;
    }

    public static float joinerGap() {
        return JOINER_GAP;
    }

    public static String joinerText() {
        return JOINER_TEXT;
    }

    public static float inputGap() {
        return INPUT_GAP;
    }

}
