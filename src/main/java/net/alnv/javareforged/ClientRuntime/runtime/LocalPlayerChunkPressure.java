package net.alnv.javareforged.ClientRuntime.runtime;

import net.alnv.javareforged.mixin.SSM.LocalPlayerSSAccessor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;

public final class LocalPlayerChunkPressure {
    private LocalPlayerChunkPressure() {
    }

    public static void afterTick(LocalPlayer player) {
        reportPosition(player, false);
    }

    public static void forcePosition(LocalPlayer player) {
        reportPosition(player, true);
    }

    private static void reportPosition(LocalPlayer player, boolean force) {
        if (Client.player() != player) {
            throw new IllegalStateException("LocalPlayer tick ran in the wrong player slot");
        }

        ClientLevel level = Client.level();
        if (level == null || player.level() != level) {
            throw new IllegalStateException("LocalPlayer tick ran with a level outside its player slot");
        }

        if (force || !level.hasChunkAt(player.getBlockX(), player.getBlockZ())) {
            ((LocalPlayerSSAccessor)player).splitTest$sendPosition();
        }
    }
}
