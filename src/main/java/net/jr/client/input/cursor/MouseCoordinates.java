package net.jr.client.input.cursor;

import net.minecraft.client.Minecraft;

public final class MouseCoordinates {
    private MouseCoordinates() {
    }

    public static double rawMouseToGlobalGuiX(Minecraft minecraft, double rawMouseX) {
        if (minecraft == null || minecraft.getWindow() == null) {
            return 0.0D;
        }

        return rawMouseX * minecraft.getWindow().getGuiScaledWidth() / (double) minecraft.getWindow().getScreenWidth();
    }

    public static double rawMouseToGlobalGuiY(Minecraft minecraft, double rawMouseY) {
        if (minecraft == null || minecraft.getWindow() == null) {
            return 0.0D;
        }

        return rawMouseY * minecraft.getWindow().getGuiScaledHeight() / (double) minecraft.getWindow().getScreenHeight();
    }
}

