package net.jr.ClientRuntime.test;

import net.jr.ClientRuntime.runtime.LocalPlayers;
import net.jr.ClientRuntime.slot.PlayerSlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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

    public static void renderOverlay(GuiGraphicsExtractor graphics, PlayerSlot slot) {
        if (!overlayVisible) {
            return;
        }
        int focusedSlot = LocalPlayers.INSTANCE.inputFocus().focusedSlotId();
        String text = slot.id() == focusedSlot ? "INPUT FOCUS" : "slot " + slot.id();
        graphics.text(Minecraft.getInstance().font, text, 4, 4, slot.id() == focusedSlot ? 0xFFFFFF55 : 0xFFFFFFFF, true);
    }

    private static void onKey(InputEvent.Key event) {
        if (event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (event.getKey() == GLFW.GLFW_KEY_F7) {
            overlayVisible = !overlayVisible;
            minecraft.gui.hud.setOverlayMessage(
                    Component.literal("Split debug overlay " + (overlayVisible ? "ON" : "OFF")),
                    false
            );
            return;
        }

        if (event.getKey() == GLFW.GLFW_KEY_F9) {
            LocalPlayers.INSTANCE.cycleTestInputFocus(minecraft);
            minecraft.gui.hud.setOverlayMessage(
                    Component.literal("Input slot " + LocalPlayers.INSTANCE.inputFocus().focusedSlotId()),
                    false
            );
        }
    }
}
