package net.jr.client.ui.hint.model;

import java.util.List;
import java.util.Objects;

public record ResolvedControlHintInput(List<ResolvedControlHintIcon> icons) {
    public ResolvedControlHintInput {
        icons = List.copyOf(Objects.requireNonNull(icons, "icons"));
        if (icons.isEmpty()) {
            throw new IllegalArgumentException("icons must not be empty");
        }
    }

    public float width(float joinerWidth, float joinerGap) {
        float width = 0.0F;
        for (ResolvedControlHintIcon icon : this.icons) {
            if (width > 0.0F) {
                width += (joinerGap * 2.0F) + joinerWidth;
            }
            width += icon.drawWidth();
        }
        return width;
    }

    public float height() {
        float height = 0.0F;
        for (ResolvedControlHintIcon icon : this.icons) {
            height = Math.max(height, icon.drawHeight());
        }
        return height;
    }

    public float visibleLeft() {
        return this.icons.getFirst().visibleLeft();
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
}
