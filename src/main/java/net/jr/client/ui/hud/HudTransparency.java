package net.jr.client.ui.hud;

import net.minecraft.util.Mth;

public final class HudTransparency {
    private static final float HUD_TRANSPARENCY_PERCENT = 0;

    private HudTransparency() {
    }

    public static float hudAlpha() {
        return 1.0F - Mth.clamp(HUD_TRANSPARENCY_PERCENT, 0.0F, 100.0F) / 100.0F;
    }

    public static float elementAlpha() {
        return hudAlpha();
    }

    public static int applyAlpha(int argb, float alpha) {
        int colorAlpha = argb >>> 24;
        if (colorAlpha == 0 && (argb & 0x00FFFFFF) != 0) {
            colorAlpha = 255;
        }
        int scaledAlpha = Mth.clamp(Math.round(colorAlpha * Mth.clamp(alpha, 0.0F, 1.0F)), 0, 255);
        return (argb & 0x00FFFFFF) | scaledAlpha << 24;
    }
}
