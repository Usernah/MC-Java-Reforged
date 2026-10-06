package net.jr.client.runtime.player;

import net.jr.api.client.local.Client;
import net.jr.client.runtime.client.ClientInstance;

import net.jr.mixin.accessors.LocalPlayerAccessor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;

public final class PositionSync {
    private PositionSync() {
    }

    public static void afterTick(LocalPlayer player) {
        reportPosition(player, false);
    }

    public static void forcePosition(LocalPlayer player) {
        reportPosition(player, true);
    }

    private static void reportPosition(LocalPlayer player, boolean force) {
        ClientInstance client = Client.current();
        if (client.player() != player) {
            throw new IllegalStateException("LocalPlayer tick ran in the wrong player slot");
        }

        ClientLevel level = client.level();
        if (level == null || player.level() != level) {
            throw new IllegalStateException("LocalPlayer tick ran with a level outside its player slot");
        }

        if (force || !level.hasChunkAt(player.getBlockX(), player.getBlockZ())) {
            ((LocalPlayerAccessor)player).javaReforged$sendPosition();
        }
    }
}
