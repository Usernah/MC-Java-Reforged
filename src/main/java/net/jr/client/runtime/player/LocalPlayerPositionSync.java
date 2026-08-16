package net.jr.client.runtime.player;

import net.jr.client.runtime.context.LocalClientAcces;
import net.jr.mixin.runtime.LocalPlayerSSAccessor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;

public final class LocalPlayerPositionSync {
    private LocalPlayerPositionSync() {
    }

    public static void afterTick(LocalPlayer player) {
        reportPosition(player, false);
    }

    public static void forcePosition(LocalPlayer player) {
        reportPosition(player, true);
    }

    private static void reportPosition(LocalPlayer player, boolean force) {
        if (LocalClientAcces.player() != player) {
            throw new IllegalStateException("LocalPlayer tick ran in the wrong player slot");
        }

        ClientLevel level = LocalClientAcces.level();
        if (level == null || player.level() != level) {
            throw new IllegalStateException("LocalPlayer tick ran with a level outside its player slot");
        }

        if (force || !level.hasChunkAt(player.getBlockX(), player.getBlockZ())) {
            ((LocalPlayerSSAccessor)player).splitTest$sendPosition();
        }
    }
}
