package net.jr.client.runtime.player;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public final class SecondaryNameResolver {
    private SecondaryNameResolver() {
    }

    public static Component resolve(Player player, Component vanillaName) {
        Component visibleName = SecondaryProfile.visibleName(player.getGameProfile());
        return visibleName == null ? vanillaName : visibleName.copy().withStyle(vanillaName.getStyle());
    }
}
