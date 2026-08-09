package net.jr.client.ui.hint.render;

import java.util.ArrayList;
import java.util.List;
import net.jr.api.client.render.Draw;
import net.jr.client.ui.hud.HudTransparency;
import net.jr.client.ui.hint.ControlHintContext;
import net.jr.client.ui.hint.model.HintPlacement;
import net.jr.client.ui.hint.model.ResolvedControlHint;
import net.jr.client.ui.hint.model.ResolvedControlHintIcon;
import net.jr.registry.ModFonts;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class ControlHintRenderer {
    private static final float DEFAULT_HINT_BOX_SIZE = 23F;
    private static final float BAR_HEIGHT = 28F;
    private static final float LEFT_PADDING = 6F;
    private static final float INTER_HINT_PADDING = 8F;
    private static final float VERTICAL_HINT_PADDING = 2F;
    private static final float TEXT_OFFSET_FROM_ICON = 2F;
    private static final int TEXT_BG_PADDING_X = 0;
    private static final int TEXT_BG_PADDING_Y = 1;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int TEXT_SHADOW = 0xFF0f0f0f;
    private static final float MENU_TEXT_SCALE = 0.6F;
    private static final float HUD_TEXT_SCALE = 0.6F;

    private ControlHintRenderer() {}

    public static int reservedScreenBottomHeight() {
        return (int) Math.ceil(BAR_HEIGHT);
    }

    public static void render(ControlHintContext context, GuiGraphicsExtractor graphics, List<ResolvedControlHint> hints) {
        if (hints.isEmpty()) {
            return;
        }
        if (context.isHud()) renderHud(graphics, hints);
        else renderMenu(graphics, hints);
    }

    private static void renderMenu(GuiGraphicsExtractor graphics, List<ResolvedControlHint> hints) {
        float rowHeight = rowHeight(hints);
        float anchorY = graphics.guiHeight() - Math.max(BAR_HEIGHT, rowHeight) / 2F;
        float previousTextEnd = 0F;
        boolean firstHint = true;

        for (ResolvedControlHint hint : hints) {
            float iconX = firstHint
                ? LEFT_PADDING
                : previousTextEnd + INTER_HINT_PADDING - hint.visibleLeft();
            drawIcons(graphics, hint, iconX, anchorY, 1.0F);

            float textX = iconX + visibleRight(hint) + TEXT_OFFSET_FROM_ICON;
            Draw.TextBuilder labelText = Draw.text(hint.label())
                .font(ModFonts.MINECRAFT_MIN)
                .scale(MENU_TEXT_SCALE)
                .shadowColor(TEXT_SHADOW)
                .color(TEXT_COLOR);
            int textY = Math.round(anchorY - labelText.lineHeight() / 2.0F);
            int textBlockX = Math.round(textX);
            labelText.position(textBlockX + TEXT_BG_PADDING_X, textY).draw(graphics);
            previousTextEnd = textBlockX + labelText.width() + TEXT_BG_PADDING_X * 2;
            firstHint = false;
        }
    }

    private static void renderHud(GuiGraphicsExtractor graphics, List<ResolvedControlHint> hints) {
        List<ResolvedControlHint> left = new ArrayList<>();
        List<ResolvedControlHint> right = new ArrayList<>();
        for (ResolvedControlHint hint : hints) {
            (hint.placement() == HintPlacement.LEFT ? left : right).add(hint);
        }
        float alpha = HudTransparency.elementAlpha();
        float y = graphics.guiHeight() - BAR_HEIGHT / 2F;
        for (ResolvedControlHint hint : left) {
            float iconX = LEFT_PADDING;
            drawIcons(graphics, hint, iconX, y, alpha);
            float textX = iconX + visibleRight(hint) + TEXT_OFFSET_FROM_ICON;
            drawText(graphics, hint.label(), textX, y, false, alpha);
            y -= rowHeight(hint) + VERTICAL_HINT_PADDING;
        }

        y = graphics.guiHeight() - BAR_HEIGHT / 2F;
        for (ResolvedControlHint hint : right) {
            float iconX = graphics.guiWidth() - LEFT_PADDING - visibleRight(hint);
            float textRight = iconX + hint.visibleLeft() - TEXT_OFFSET_FROM_ICON;
            drawText(graphics, hint.label(), textRight, y, true, alpha);
            drawIcons(graphics, hint, iconX, y, alpha);
            y -= rowHeight(hint) + VERTICAL_HINT_PADDING;
        }
    }

    private static void drawText(
        GuiGraphicsExtractor graphics,
        Component label,
        float anchorX,
        float centerY,
        boolean rightAligned,
        float alpha
    ) {
        Draw.TextBuilder labelText = Draw.text(label)
            .font(ModFonts.MINECRAFT_MIN)
            .scale(HUD_TEXT_SCALE)
            .shadowColor(HudTransparency.applyAlpha(TEXT_SHADOW, alpha))
            .color(HudTransparency.applyAlpha(TEXT_COLOR, alpha));
        int textBlockWidth = labelText.width() + TEXT_BG_PADDING_X * 2;
        int textX = rightAligned ? Math.round(anchorX - textBlockWidth) : Math.round(anchorX);
        int textY = Math.round(centerY - labelText.lineHeight() / 2.0F);
        labelText.position(textX + TEXT_BG_PADDING_X, textY).draw(graphics);
    }

    private static void drawIcons(
        GuiGraphicsExtractor graphics,
        ResolvedControlHint hint,
        float x,
        float centerY,
        float alpha
    ) {
        float current = x;
        for (int i = 0; i < hint.icons().size(); i++) {
            if (i > 0) {
                current += ResolvedControlHint.joinerGap();
                Draw.TextBuilder joinerText = joinerText(alpha);
                joinerText.position(
                    Math.round(current),
                    Math.round(centerY - joinerText.lineHeight() / 2F)
                ).draw(graphics);
                current += joinerText.width() + ResolvedControlHint.joinerGap();
            }
            ResolvedControlHintIcon icon = hint.icons().get(i);
            GlyphTextureBounds metrics = icon.metrics();
            Draw.image(icon.texture(), current, centerY - icon.drawHeight() / 2F, icon.drawWidth() , icon.drawHeight() )
                .uvSize(metrics.sourceWidth(), metrics.sourceHeight())
                .atlasSize(metrics.sourceWidth(), metrics.sourceHeight())
                .alpha(alpha)
                .draw(graphics);
            current += icon.drawWidth();
        }
    }

    private static float visibleRight(ResolvedControlHint hint) {
        return hint.visibleRight(joinerText(1.0F).width(), ResolvedControlHint.joinerGap());
    }

    private static Draw.TextBuilder joinerText(float alpha) {
        return Draw.text(Component.literal(ResolvedControlHint.joinerText()))
            .shadowColor(HudTransparency.applyAlpha(TEXT_SHADOW, alpha))
            .color(HudTransparency.applyAlpha(TEXT_COLOR, alpha));
    }

    private static float rowHeight(List<ResolvedControlHint> hints) {
        float height = DEFAULT_HINT_BOX_SIZE;
        for (ResolvedControlHint hint : hints) {
            height = Math.max(height, rowHeight(hint));
        }
        return height;
    }

    private static float rowHeight(ResolvedControlHint hint) {
        return Math.max(DEFAULT_HINT_BOX_SIZE, hint.iconHeight());
    }
}
