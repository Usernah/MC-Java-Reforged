package net.jr.client.runtime.terrain;

import java.util.ArrayList;
import java.util.List;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.client.LocalClient;
import net.jr.client.runtime.slot.LocalClientSlotRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;

/** Read-only counters used to locate render-distance retention across runtime layers. */
public final class TerrainResidencyDiagnostics {
    private TerrainResidencyDiagnostics() {
    }

    public static Snapshot snapshot() {
        Minecraft minecraft = Minecraft.getInstance();
        int configuredDistance = minecraft.options.getEffectiveRenderDistance();
        int serverDistance = -1;
        int serverLoadedChunks = 0;
        var server = minecraft.getSingleplayerServer();
        if (server != null) {
            serverDistance = server.getPlayerList().getViewDistance();
            for (var level : server.getAllLevels()) {
                serverLoadedChunks += level.getChunkSource().getLoadedChunksCount();
            }
        }

        List<SlotSnapshot> slots = new ArrayList<>(LocalClientSlotRegistry.MAX_SLOTS);
        for (int slotId = 0; slotId < LocalClientSlotRegistry.MAX_SLOTS; slotId++) {
            LocalClient client = ClientRuntime.INSTANCE.clients().clientOrNull(slotId);
            if (client == null) {
                continue;
            }
            ClientLevel level = client.level();
            if (level == null) {
                slots.add(new SlotSnapshot(slotId, -1, 0, 0, -1, 0L));
                continue;
            }

            ClientChunkCache cache = level.getChunkSource();
            int capacity = cacheCapacity(cache);
            int cacheRadius = capacity > 0 ? (int)((Math.sqrt(capacity) - 1.0D) / 2.0D) : -1;
            LocalTerrainView view = client.render().terrain().view();
            slots.add(new SlotSnapshot(
                slotId,
                cacheRadius,
                capacity,
                cache.getLoadedChunksCount(),
                view.viewDistance(),
                view.referencedSectionCapacity()
            ));
        }

        SharedTerrainStore store = TerrainStateStore.nullableTerrainStore();
        SharedTerrainStore.Diagnostics storeDiagnostics = store != null
            ? store.diagnostics()
            : SharedTerrainStore.Diagnostics.EMPTY;
        return new Snapshot(configuredDistance, serverDistance, serverLoadedChunks, List.copyOf(slots), storeDiagnostics);
    }

    private static int cacheCapacity(ClientChunkCache cache) {
        String stats = cache.gatherStats();
        int separator = stats.indexOf(',');
        if (separator <= 0) {
            return -1;
        }
        try {
            return Integer.parseInt(stats.substring(0, separator).trim());
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    public record Snapshot(
        int configuredDistance,
        int serverDistance,
        int serverLoadedChunks,
        List<SlotSnapshot> slots,
        SharedTerrainStore.Diagnostics store
    ) {
    }

    public record SlotSnapshot(
        int slotId,
        int cacheRadius,
        int cacheCapacity,
        int loadedChunks,
        int terrainViewDistance,
        long referencedSectionCapacity
    ) {
    }
}
