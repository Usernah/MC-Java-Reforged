package net.jr.server.container;

import net.jr.Java_reforged;
import net.jr.network.ContainerOriginNetwork;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ChestMenu;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = Java_reforged.MODID)
public final class ContainerOriginEvents {
    private static final Map<ServerPlayer, PendingOrigin> PENDING = new WeakHashMap<>();

    private ContainerOriginEvents() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Identifier blockId = BuiltInRegistries.BLOCK.getKey(
            player.level().getBlockState(event.getPos()).getBlock()
        );
        PENDING.put(player, new PendingOrigin(blockId, player.level().getGameTime()));
    }

    @SubscribeEvent
    public static void onOpen(PlayerContainerEvent.Open event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        PendingOrigin pending = PENDING.remove(player);
        if (!(event.getContainer() instanceof ChestMenu menu)
            || !player.connection.hasChannel(ContainerOriginNetwork.OpenOrigin.TYPE)) {
            return;
        }
        String blockId = pending != null && pending.gameTime() == player.level().getGameTime()
            ? pending.blockId().toString()
            : "";
        PacketDistributor.sendToPlayer(
            player,
            new ContainerOriginNetwork.OpenOrigin(menu.containerId, blockId)
        );
    }

    private record PendingOrigin(Identifier blockId, long gameTime) {
    }
}
