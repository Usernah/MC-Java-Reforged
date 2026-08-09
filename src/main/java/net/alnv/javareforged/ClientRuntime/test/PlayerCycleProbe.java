package net.alnv.javareforged.ClientRuntime.test;

import net.alnv.javareforged.ClientRuntime.runtime.LocalPlayers;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

public final class PlayerCycleProbe {
    private static boolean installed;

    private PlayerCycleProbe() {
    }

    public static void install() {
        if (installed) {
            return;
        }
        installed = true;
        NeoForge.EVENT_BUS.addListener(PlayerCycleProbe::onKey);
    }

    private static void onKey(InputEvent.Key event) {
        if (event.getKey() != GLFW.GLFW_KEY_F8 || event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }
        LocalPlayers.INSTANCE.cycleTestPlayerCount(Minecraft.getInstance());
    }
}
