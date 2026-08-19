package net.jr.client.ui.hint.model;

import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Objects;

public record ResolvedControlHint(
    HintPlacement placement,
    Component label,
    List<ResolvedControlHintIcon> icons
) {
    private static final float JOINER_GAP = 2.0F;
    private static final String JOINER_TEXT = "+";

    public ResolvedControlHint {
        Objects.requireNonNull(placement, "placement");
        Objects.requireNonNull(label, "label");
        icons = List.copyOf(Objects.requireNonNull(icons, "icons"));
        if (icons.isEmpty()) {
            throw new IllegalArgumentException("icons must not be empty");
        }
    }

    public float iconWidth(float joinerWidth, float joinerGap) {
        float width = 0.0F;
        for (ResolvedControlHintIcon icon : this.icons) {
            if (width > 0.0F) {
                width += (joinerGap * 2.0F) + joinerWidth;
            }
            width += icon.drawWidth();
        }
        return width;
    }

    public float iconHeight() {
        float height = 0.0F;
        for (ResolvedControlHintIcon icon : this.icons) {
            height = Math.max(height, icon.drawHeight());
        }
        return height;
    }

    public float visibleLeft() {
        ResolvedControlHintIcon first = this.icons.getFirst();
        return first.visibleLeft();
    }

    public float visibleRight(float joinerWidth, float joinerGap) {
        float x = 0.0F;
        for (int i = 0; i < this.icons.size(); i++) {
            ResolvedControlHintIcon icon = this.icons.get(i);
            if (i == this.icons.size() - 1) {
                return x + icon.visibleRight();
            }
            x += icon.drawWidth() + (joinerGap * 2.0F) + joinerWidth;
        }
        return 0.0F;
    }

    public static float joinerGap() {
        return JOINER_GAP;
    }

    public static String joinerText() {
        return JOINER_TEXT;
    }

}
