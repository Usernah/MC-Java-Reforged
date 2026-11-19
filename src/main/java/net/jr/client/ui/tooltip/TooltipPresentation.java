package net.jr.client.ui.tooltip;

import net.jr.api.client.render.font.FontHolder;
import net.jr.client.ui.presentation.UiPresentation;

public class TooltipPresentation {

    private TooltipPresentation() {}

    public static float scale() {
        return UiPresentation.isSmall() ? 1.0F : 2.0F;
    }

    public static FontHolder font() {
        return UiPresentation.currentFont();
    }

}
