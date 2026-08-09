package net.alnv.javareforged.ClientRuntime.test;

import net.alnv.javareforged.ClientRuntime.runtime.LocalPlayers;
import net.alnv.javareforged.ClientRuntime.runtime.TerrainDebug;
import net.alnv.javareforged.ClientRuntime.slot.PlayerSlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

public final class InputSlotProbe {
    private static boolean installed;
    private static boolean overlayVisible;

    private InputSlotProbe() {
    }

    public static void install() {
        if (installed) {
            return;
        }
        installed = true;
        NeoForge.EVENT_BUS.addListener(InputSlotProbe::onKey);
    }

    public static void renderOverlay(GuiGraphics graphics, PlayerSlot slot) {
        if (!overlayVisible) {
            return;
        }
        int focusedSlot = LocalPlayers.INSTANCE.inputFocus().focusedSlotId();
        String text = slot.id() == focusedSlot ? "INPUT FOCUS" : "slot " + slot.id();
        graphics.drawString(Minecraft.getInstance().font, text, 4, 4, slot.id() == focusedSlot ? 0xFFFFFF55 : 0xFFFFFFFF, true);
    }

    private static void onKey(InputEvent.Key event) {
        if (event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (event.getKey() == GLFW.GLFW_KEY_F7) {
            overlayVisible = TerrainDebug.toggleOverlay();
            minecraft.gui.setOverlayMessage(
                    Component.literal("Split debug overlay " + (overlayVisible ? "ON" : "OFF")),
                    false
            );
            return;
        }

        if (event.getKey() == GLFW.GLFW_KEY_F9) {
            LocalPlayers.INSTANCE.cycleTestInputFocus(minecraft);
            minecraft.gui.setOverlayMessage(
                    Component.literal("Input slot " + LocalPlayers.INSTANCE.inputFocus().focusedSlotId()),
                    false
            );
        }
    }
}
