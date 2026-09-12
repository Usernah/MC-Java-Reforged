package net.jr.client.compat.xaero;

import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.client.LocalClient;
import net.jr.client.runtime.context.LocalClientExecution;
import xaero.map.MapRunner;
import xaero.map.WorldMapSession;

public final class XaeroWorldMapRuntime {
    private XaeroWorldMapRuntime() {
    }

    public static boolean runProcessors(MapRunner runner) {
        boolean[] ran = {false};
        for (LocalClient client : ClientRuntime.INSTANCE.clients().all()) {
            if (client.player() == null) {
                continue;
            }
            WorldMapSession session = WorldMapSession.getForPlayer(client.player());
            if (session == null || !session.isUsable()) {
                continue;
            }
            ran[0] = true;
            LocalClientExecution.runForClient(
                client.slotId(),
                () -> session.getMapProcessor().run(runner)
            );
        }
        return ran[0];
    }

    public static void runEveryClient(Runnable action) {
        for (LocalClient client : ClientRuntime.INSTANCE.clients().all()) {
            if (client.player() != null) {
                LocalClientExecution.runForClient(client.slotId(), action);
            }
        }
    }

    public static void runForPlayer(PlayerAction action, net.minecraft.world.entity.player.Player player) {
        for (LocalClient client : ClientRuntime.INSTANCE.clients().all()) {
            if (client.player() == player) {
                LocalClientExecution.runForClient(client.slotId(), () -> action.run(player));
                return;
            }
        }
        action.run(player);
    }

    @FunctionalInterface
    public interface PlayerAction {
        void run(net.minecraft.world.entity.player.Player player);
    }
}
