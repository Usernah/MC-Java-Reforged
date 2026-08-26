package net.jr.client.runtime.player;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public final class SecondaryPlayerNameResolver {
    private SecondaryPlayerNameResolver() {
    }

    public static Component resolve(Player player, Component vanillaName) {
        Component visibleName = SecondaryPlayerProfileFactory.visibleName(player.getGameProfile());
        return visibleName == null ? vanillaName : visibleName.copy().withStyle(vanillaName.getStyle());
    }
}
